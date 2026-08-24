package dev.wecke.oreheightindicator.data;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.FeaturePlacementContext;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.placementmodifier.PlacementModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Builds compact height-suitability profiles from the effective registry of an
 * integrated server. The registry already contains the active vanilla data,
 * datapacks and standard modded ore features for the current world.
 */
public final class RuntimeWorldgenProvider implements OreDataProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger("OreHeightIndicator/Worldgen");
    private static final int PROFILE_SAMPLES = 768;
    private static final long MAX_POSITIONS_PER_SAMPLE = 4096L;
    private static final Snapshot EMPTY = new Snapshot("empty", -64, 319, 0L, List.of());

    private volatile Snapshot snapshot = EMPTY;
    private volatile String requestedContext = "";
    private long nextRevision = 1L;

    @Override
    public void refresh(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            return;
        }

        IntegratedServer server = client.getServer();
        if (server == null) {
            return;
        }

        BlockPos playerPos = client.player.getBlockPos().toImmutable();
        RegistryKey<World> dimensionKey = client.world.getRegistryKey();
        String biomeKey = client.world.getBiome(playerPos)
            .getKey()
            .map(key -> key.getValue().toString())
            .orElse("unknown");
        String contextKey = System.identityHashCode(server.getRegistryManager())
            + "|" + dimensionKey.getValue()
            + "|" + biomeKey;

        if (contextKey.equals(requestedContext) || contextKey.equals(snapshot.contextKey)) {
            return;
        }

        requestedContext = contextKey;
        server.execute(() -> rebuildOnServerThread(server, dimensionKey, playerPos, contextKey));
    }

    private void rebuildOnServerThread(
        IntegratedServer server,
        RegistryKey<World> dimensionKey,
        BlockPos playerPos,
        String contextKey
    ) {
        try {
            ServerWorld world = server.getWorld(dimensionKey);
            if (world == null) {
                if (contextKey.equals(requestedContext)) {
                    requestedContext = "";
                }
                return;
            }

            Snapshot rebuilt = buildSnapshot(world, playerPos, contextKey, nextRevision++);
            if (contextKey.equals(requestedContext)) {
                snapshot = rebuilt;
                LOGGER.info(
                    "Loaded {} ore profiles from active worldgen for {} at {}",
                    rebuilt.ores.size(),
                    dimensionKey.getValue(),
                    rebuilt.biomeLabel
                );
            }
        } catch (RuntimeException ex) {
            if (contextKey.equals(requestedContext)) {
                requestedContext = "";
            }
            LOGGER.warn("Could not build ore profiles from active worldgen", ex);
        }
    }

    static Snapshot buildSnapshot(ServerWorld world, BlockPos origin, String contextKey, long revision) {
        int minY = world.getBottomY();
        int maxY = world.getTopY() - 1;
        int heightCount = Math.max(1, maxY - minY + 1);
        ChunkGenerator generator = world.getChunkManager().getChunkGenerator();
        RegistryEntry<net.minecraft.world.biome.Biome> biomeEntry = world.getBiome(origin);
        GenerationSettings generationSettings = generator.getGenerationSettings(biomeEntry);
        Map<String, MutableOreProfile> profiles = new LinkedHashMap<>();
        Set<PlacedFeature> seenFeatures = Collections.newSetFromMap(new IdentityHashMap<>());

        for (RegistryEntryList<PlacedFeature> featureStep : generationSettings.getFeatures()) {
            for (RegistryEntry<PlacedFeature> featureEntry : featureStep) {
                PlacedFeature placedFeature = featureEntry.value();
                if (!seenFeatures.add(placedFeature)) {
                    continue;
                }

                ConfiguredFeature<?, ?> configuredFeature = placedFeature.feature().value();
                if (!(configuredFeature.config() instanceof OreFeatureConfig oreConfig)) {
                    continue;
                }

                Map<String, OreDescriptor> descriptors = describeOreTargets(oreConfig);
                if (descriptors.isEmpty()) {
                    continue;
                }

                float[] featureProfile = samplePlacementProfile(
                    world,
                    generator,
                    placedFeature,
                    featureEntry,
                    origin,
                    minY,
                    maxY
                );
                float yieldWeight = expectedYieldWeight(oreConfig);

                for (OreDescriptor descriptor : descriptors.values()) {
                    MutableOreProfile profile = profiles.computeIfAbsent(
                        descriptor.key,
                        ignored -> new MutableOreProfile(descriptor, new float[heightCount])
                    );
                    for (int i = 0; i < heightCount; i++) {
                        profile.scores[i] += featureProfile[i] * yieldWeight;
                    }
                }
            }
        }

        List<OreProfile> normalized = new ArrayList<>(profiles.size());
        for (MutableOreProfile profile : profiles.values()) {
            float peak = 0.0f;
            for (float value : profile.scores) {
                peak = Math.max(peak, value);
            }
            if (peak <= 0.0f) {
                continue;
            }
            for (int i = 0; i < profile.scores.length; i++) {
                profile.scores[i] = Math.max(0.0f, Math.min(1.0f, profile.scores[i] / peak));
            }
            normalized.add(new OreProfile(profile.descriptor, profile.scores));
        }

        normalized.sort((left, right) -> left.descriptor.key.compareTo(right.descriptor.key));
        String biomeLabel = biomeEntry.getKey()
            .map(key -> key.getValue().toString())
            .orElse("unknown");
        return new Snapshot(contextKey, minY, maxY, revision, List.copyOf(normalized), biomeLabel);
    }

    private static Map<String, OreDescriptor> describeOreTargets(OreFeatureConfig config) {
        Map<String, OreDescriptor> descriptors = new LinkedHashMap<>();
        for (OreFeatureConfig.Target target : config.targets) {
            BlockState state = target.state;
            Block block = state.getBlock();
            Identifier blockId = Registries.BLOCK.getId(block);
            String normalizedPath = normalizeOrePath(blockId.getPath());
            if (normalizedPath == null) {
                continue;
            }

            String key = blockId.getNamespace() + ":" + normalizedPath;
            OreDescriptor existing = descriptors.get(key);
            boolean preferred = isPreferredDisplayBlock(blockId.getPath());
            if (existing == null || preferred) {
                Item item = block.asItem();
                descriptors.put(key, new OreDescriptor(
                    key,
                    block.getTranslationKey(),
                    item == Items.AIR ? Items.AIR : item
                ));
            }
        }
        return descriptors;
    }

    static String normalizeOrePath(String path) {
        if ("ancient_debris".equals(path)) {
            return path;
        }
        if (!path.endsWith("_ore")) {
            return null;
        }
        for (String prefix : List.of("deepslate_", "stone_", "netherrack_", "blackstone_", "end_stone_")) {
            if (path.startsWith(prefix)) {
                return path.substring(prefix.length());
            }
        }
        return path;
    }

    private static boolean isPreferredDisplayBlock(String path) {
        return !(path.startsWith("deepslate_")
            || path.startsWith("stone_")
            || path.startsWith("netherrack_")
            || path.startsWith("blackstone_")
            || path.startsWith("end_stone_"));
    }

    private static float[] samplePlacementProfile(
        ServerWorld world,
        ChunkGenerator generator,
        PlacedFeature placedFeature,
        RegistryEntry<PlacedFeature> featureEntry,
        BlockPos playerOrigin,
        int minY,
        int maxY
    ) {
        float[] histogram = new float[Math.max(1, maxY - minY + 1)];
        FeaturePlacementContext context = new FeaturePlacementContext(world, generator, Optional.of(placedFeature));
        BlockPos chunkOrigin = new BlockPos(playerOrigin.getX() & ~15, minY, playerOrigin.getZ() & ~15);
        long seed = featureEntry.getKey()
            .map(key -> (long) key.getValue().toString().hashCode())
            .orElse((long) placedFeature.hashCode());
        Random random = Random.create(seed ^ 0x4F52454845494748L);

        for (int sample = 0; sample < PROFILE_SAMPLES; sample++) {
            Stream<BlockPos> positions = Stream.of(chunkOrigin);
            for (PlacementModifier modifier : placedFeature.placementModifiers()) {
                positions = positions.flatMap(pos -> modifier.getPositions(context, random, pos));
            }
            positions.limit(MAX_POSITIONS_PER_SAMPLE).forEach(pos -> {
                int y = pos.getY();
                if (y >= minY && y <= maxY) {
                    histogram[y - minY] += 1.0f;
                }
            });
        }

        float divisor = PROFILE_SAMPLES;
        for (int i = 0; i < histogram.length; i++) {
            histogram[i] /= divisor;
        }
        return histogram;
    }

    private static float expectedYieldWeight(OreFeatureConfig config) {
        float discardFactor = 1.0f - (Math.max(0.0f, Math.min(1.0f, config.discardOnAirChance)) * 0.5f);
        return Math.max(1.0f, config.size) * discardFactor;
    }

    @Override
    public long revision() {
        return snapshot.revision;
    }

    @Override
    public int minY() {
        return snapshot.minY;
    }

    @Override
    public int maxY() {
        return snapshot.maxY;
    }

    @Override
    public int oreCount() {
        return snapshot.ores.size();
    }

    @Override
    public String oreName(int index) {
        String path = Identifier.of(snapshot.ores.get(index).descriptor.key).getPath();
        String readable = path.replace('_', ' ');
        return readable.substring(0, 1).toUpperCase(Locale.ROOT) + readable.substring(1);
    }

    @Override
    public String oreKey(int index) {
        return snapshot.ores.get(index).descriptor.key;
    }

    @Override
    public String oreTranslationKey(int index) {
        return snapshot.ores.get(index).descriptor.translationKey;
    }

    @Override
    public Item oreItem(int index) {
        return snapshot.ores.get(index).descriptor.item;
    }

    @Override
    public void fillScores(int y, float[] outScores) {
        Snapshot current = snapshot;
        java.util.Arrays.fill(outScores, 0.0f);
        int clampedY = Math.max(current.minY, Math.min(current.maxY, y));
        int yIndex = clampedY - current.minY;
        int count = Math.min(outScores.length, current.ores.size());
        for (int i = 0; i < count; i++) {
            outScores[i] = current.ores.get(i).scores[yIndex];
        }
    }

    static final class Snapshot {
        private final String contextKey;
        private final int minY;
        private final int maxY;
        private final long revision;
        private final List<OreProfile> ores;
        private final String biomeLabel;

        private Snapshot(String contextKey, int minY, int maxY, long revision, List<OreProfile> ores) {
            this(contextKey, minY, maxY, revision, ores, "unknown");
        }

        private Snapshot(
            String contextKey,
            int minY,
            int maxY,
            long revision,
            List<OreProfile> ores,
            String biomeLabel
        ) {
            this.contextKey = contextKey;
            this.minY = minY;
            this.maxY = maxY;
            this.revision = revision;
            this.ores = ores;
            this.biomeLabel = biomeLabel;
        }
    }

    private record OreDescriptor(String key, String translationKey, Item item) {
    }

    private record OreProfile(OreDescriptor descriptor, float[] scores) {
    }

    private record MutableOreProfile(OreDescriptor descriptor, float[] scores) {
    }
}
