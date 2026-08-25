package dev.wecke.oreheightindicator.data;

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
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

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
    public void refresh(Minecraft client) {
        if (client == null || client.player == null || client.level == null) {
            return;
        }

        IntegratedServer server = client.getSingleplayerServer();
        if (server == null) {
            return;
        }

        BlockPos playerPos = client.player.blockPosition().immutable();
        ResourceKey<Level> dimensionKey = client.level.dimension();
        String biomeKey = client.level.getBiome(playerPos)
            .unwrapKey()
            .map(key -> key.location().toString())
            .orElse("unknown");
        String contextKey = System.identityHashCode(server.registryAccess())
            + "|" + dimensionKey.location()
            + "|" + biomeKey;

        if (contextKey.equals(requestedContext) || contextKey.equals(snapshot.contextKey)) {
            return;
        }

        requestedContext = contextKey;
        server.execute(() -> rebuildOnServerThread(server, dimensionKey, playerPos, contextKey));
    }

    private void rebuildOnServerThread(
        IntegratedServer server,
        ResourceKey<Level> dimensionKey,
        BlockPos playerPos,
        String contextKey
    ) {
        try {
            ServerLevel world = server.getLevel(dimensionKey);
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
                    "Loaded {} worldgen profiles from active worldgen for {} at {}",
                    rebuilt.ores.size(),
                    dimensionKey.location(),
                    rebuilt.biomeLabel
                );
            }
        } catch (RuntimeException ex) {
            if (contextKey.equals(requestedContext)) {
                requestedContext = "";
            }
            LOGGER.warn("Could not build worldgen profiles from active worldgen", ex);
        }
    }

    static Snapshot buildSnapshot(ServerLevel world, BlockPos origin, String contextKey, long revision) {
        int minY = world.getMinBuildHeight();
        int maxY = world.getMaxBuildHeight() - 1;
        int heightCount = Math.max(1, maxY - minY + 1);
        ChunkGenerator generator = world.getChunkSource().getGenerator();
        Holder<net.minecraft.world.level.biome.Biome> biomeEntry = world.getBiome(origin);
        BiomeGenerationSettings generationSettings = generator.getBiomeGenerationSettings(biomeEntry);
        Map<String, MutableOreProfile> profiles = new LinkedHashMap<>();
        Set<PlacedFeature> seenFeatures = Collections.newSetFromMap(new IdentityHashMap<>());

        for (HolderSet<PlacedFeature> featureStep : generationSettings.features()) {
            for (Holder<PlacedFeature> featureEntry : featureStep) {
                PlacedFeature placedFeature = featureEntry.value();
                if (!seenFeatures.add(placedFeature)) {
                    continue;
                }

                ConfiguredFeature<?, ?> configuredFeature = placedFeature.feature().value();
                if (!(configuredFeature.config() instanceof OreConfiguration oreConfig)) {
                    continue;
                }

                Map<String, OreDescriptor> descriptors = describeWorldgenTargets(oreConfig);
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
        String biomeLabel = biomeEntry.unwrapKey()
            .map(key -> key.location().toString())
            .orElse("unknown");
        return new Snapshot(contextKey, minY, maxY, revision, List.copyOf(normalized), biomeLabel);
    }

    private static Map<String, OreDescriptor> describeWorldgenTargets(OreConfiguration config) {
        Map<String, OreDescriptor> descriptors = new LinkedHashMap<>();
        for (OreConfiguration.TargetBlockState target : config.targetStates) {
            BlockState state = target.state;
            Block block = state.getBlock();
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
            String normalizedPath = normalizeOrePath(blockId.getPath());
            String key = normalizedPath == null
                ? blockId.toString()
                : blockId.getNamespace() + ":" + normalizedPath;
            OreDescriptor existing = descriptors.get(key);
            boolean preferred = normalizedPath != null && isPreferredDisplayBlock(blockId.getPath());
            if (existing == null || preferred) {
                Item item = block.asItem();
                descriptors.put(key, new OreDescriptor(
                    key,
                    block.getDescriptionId(),
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
        ServerLevel world,
        ChunkGenerator generator,
        PlacedFeature placedFeature,
        Holder<PlacedFeature> featureEntry,
        BlockPos playerOrigin,
        int minY,
        int maxY
    ) {
        float[] histogram = new float[Math.max(1, maxY - minY + 1)];
        PlacementContext context = new PlacementContext(world, generator, Optional.of(placedFeature));
        BlockPos chunkOrigin = new BlockPos(playerOrigin.getX() & ~15, minY, playerOrigin.getZ() & ~15);
        long seed = featureEntry.unwrapKey()
            .map(key -> (long) key.location().toString().hashCode())
            .orElse((long) placedFeature.hashCode());
        RandomSource random = RandomSource.create(seed ^ 0x4F52454845494748L);

        for (int sample = 0; sample < PROFILE_SAMPLES; sample++) {
            Stream<BlockPos> positions = Stream.of(chunkOrigin);
            for (PlacementModifier modifier : placedFeature.placement()) {
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

    private static float expectedYieldWeight(OreConfiguration config) {
        float discardFactor = 1.0f - (Math.max(0.0f, Math.min(1.0f, config.discardChanceOnAirExposure)) * 0.5f);
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
        String path = ResourceLocation.parse(snapshot.ores.get(index).descriptor.key).getPath();
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
