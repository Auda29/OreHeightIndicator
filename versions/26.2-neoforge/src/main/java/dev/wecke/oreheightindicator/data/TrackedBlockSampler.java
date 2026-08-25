package dev.wecke.oreheightindicator.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

public final class TrackedBlockSampler {
    private static final int SAMPLE_RADIUS = 32;
    private static final int SAMPLE_STEP = 4;
    private static final int CELL_SIZE = 32;
    private static final int SMOOTHING_RADIUS = 2;
    private static final long RESAMPLE_INTERVAL_NANOS = 30_000_000_000L;

    private Map<String, Profile> profiles = Map.of();
    private String contextKey = "";
    private long nextRefreshNanos;
    private long revision;

    public void refresh(Minecraft client, Collection<String> requestedKeys) {
        if (client == null || client.player == null || client.level == null) {
            clear();
            return;
        }
        List<Target> targets = resolveTargets(requestedKeys);
        if (targets.isEmpty()) {
            clear();
            return;
        }

        BlockPos playerPos = client.player.blockPosition();
        int centerX = Math.floorDiv(playerPos.getX(), CELL_SIZE) * CELL_SIZE + (CELL_SIZE / 2);
        int centerZ = Math.floorDiv(playerPos.getZ(), CELL_SIZE) * CELL_SIZE + (CELL_SIZE / 2);
        String keys = targets.stream().map(Target::key).sorted().reduce((a, b) -> a + "," + b).orElse("");
        String nextContext = System.identityHashCode(client.level) + "|" + centerX + "|" + centerZ + "|" + keys;
        long now = System.nanoTime();
        if (nextContext.equals(contextKey) && now < nextRefreshNanos) {
            return;
        }

        profiles = sample(client, targets, centerX, centerZ);
        contextKey = nextContext;
        nextRefreshNanos = now + RESAMPLE_INTERVAL_NANOS;
        revision++;
    }

    public long revision() {
        return revision;
    }

    public List<Profile> profiles() {
        return List.copyOf(profiles.values());
    }

    public float scoreAt(String key, int y) {
        Profile profile = profiles.get(key);
        if (profile == null) {
            return 0.0f;
        }
        int index = Math.max(profile.minY, Math.min(profile.maxY, y)) - profile.minY;
        return profile.scores[index];
    }

    private void clear() {
        if (!profiles.isEmpty() || !contextKey.isEmpty()) {
            profiles = Map.of();
            contextKey = "";
            nextRefreshNanos = 0L;
            revision++;
        }
    }

    private static List<Target> resolveTargets(Collection<String> requestedKeys) {
        Map<String, Target> targets = new LinkedHashMap<>();
        for (String key : requestedKeys) {
            Identifier id = Identifier.tryParse(key);
            if (id == null) {
                continue;
            }
            BuiltInRegistries.BLOCK.getOptional(id).ifPresent(block -> {
                Item item = block.asItem();
                targets.put(id.toString(), new Target(
                    id.toString(),
                    readableName(id),
                    block.getDescriptionId(),
                    item == Items.AIR ? Items.AIR : item,
                    block
                ));
            });
        }
        return List.copyOf(targets.values());
    }

    private static Map<String, Profile> sample(Minecraft client, List<Target> targets, int centerX, int centerZ) {
        int minY = client.level.getMinY();
        int maxY = client.level.getMaxY();
        int height = Math.max(1, maxY - minY + 1);
        int[][] counts = new int[targets.size()][height];
        Map<Block, Integer> targetIndexes = new IdentityHashMap<>();
        for (int i = 0; i < targets.size(); i++) {
            targetIndexes.put(targets.get(i).block, i);
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = centerX - SAMPLE_RADIUS; x <= centerX + SAMPLE_RADIUS; x += SAMPLE_STEP) {
            for (int z = centerZ - SAMPLE_RADIUS; z <= centerZ + SAMPLE_RADIUS; z += SAMPLE_STEP) {
                for (int y = minY; y <= maxY; y++) {
                    cursor.set(x, y, z);
                    Integer targetIndex = targetIndexes.get(client.level.getBlockState(cursor).getBlock());
                    if (targetIndex != null) {
                        counts[targetIndex][y - minY]++;
                    }
                }
            }
        }

        Map<String, Profile> sampled = new HashMap<>();
        for (int i = 0; i < targets.size(); i++) {
            Target target = targets.get(i);
            sampled.put(target.key, new Profile(
                target.key,
                target.name,
                target.translationKey,
                target.item,
                minY,
                maxY,
                normalizedProfile(counts[i])
            ));
        }
        return Map.copyOf(sampled);
    }

    static float[] normalizedProfile(int[] counts) {
        float[] smoothed = new float[counts.length];
        float peak = 0.0f;
        for (int i = 0; i < counts.length; i++) {
            int sum = 0;
            for (int offset = -SMOOTHING_RADIUS; offset <= SMOOTHING_RADIUS; offset++) {
                int source = i + offset;
                if (source >= 0 && source < counts.length) {
                    sum += Math.max(0, counts[source]);
                }
            }
            smoothed[i] = sum;
            peak = Math.max(peak, smoothed[i]);
        }
        if (peak <= 0.0f) {
            return smoothed;
        }
        for (int i = 0; i < smoothed.length; i++) {
            smoothed[i] = Math.max(0.0f, Math.min(1.0f, smoothed[i] / peak));
        }
        return smoothed;
    }

    private static String readableName(Identifier id) {
        List<String> words = new ArrayList<>();
        for (String word : id.getPath().split("_")) {
            if (!word.isEmpty()) {
                words.add(word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1));
            }
        }
        return words.isEmpty() ? id.toString() : String.join(" ", words);
    }

    private record Target(String key, String name, String translationKey, Item item, Block block) {
    }

    public record Profile(
        String key,
        String name,
        String translationKey,
        Item item,
        int minY,
        int maxY,
        float[] scores
    ) {
    }
}
