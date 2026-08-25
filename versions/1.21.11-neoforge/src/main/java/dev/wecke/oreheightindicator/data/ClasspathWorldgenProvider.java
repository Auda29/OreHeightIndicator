package dev.wecke.oreheightindicator.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntToDoubleFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/**
 * Reads built-in worldgen data from the installed Minecraft and mod classpath.
 * This is the automatic fallback when no integrated server registry is present.
 */
public final class ClasspathWorldgenProvider implements OreDataProvider {
    private static final Snapshot EMPTY = new Snapshot("empty", -64, 319, 0L, List.of());

    private volatile Snapshot snapshot = EMPTY;
    private volatile String requestedContext = "";

    @Override
    public void refresh(Minecraft client) {
        if (client == null || client.player == null || client.level == null) {
            return;
        }

        Identifier biomeId = client.level.getBiome(client.player.blockPosition())
            .unwrapKey()
            .map(key -> key.identifier())
            .orElse(null);
        if (biomeId == null) {
            return;
        }

        int minY = client.level.getMinY();
        int maxY = client.level.getMaxY();
        String contextKey = client.level.dimension().identifier() + "|" + biomeId + "|" + minY + "|" + maxY;
        if (contextKey.equals(requestedContext) || contextKey.equals(snapshot.contextKey)) {
            return;
        }

        requestedContext = contextKey;
        CompletableFuture
            .supplyAsync(() -> buildSnapshot(contextKey, biomeId, minY, maxY, snapshot.revision + 1L))
            .thenAccept(rebuilt -> {
                if (contextKey.equals(requestedContext) && !rebuilt.ores.isEmpty()) {
                    snapshot = rebuilt;
                } else if (contextKey.equals(requestedContext)) {
                    requestedContext = "";
                }
            })
            .exceptionally(error -> {
                if (contextKey.equals(requestedContext)) {
                    requestedContext = "";
                }
                return null;
            });
    }

    static Snapshot buildSnapshot(String contextKey, Identifier biomeId, int minY, int maxY, long revision) {
        JsonObject biome = readWorldgenJson("biome", biomeId);
        if (biome == null || !biome.has("features")) {
            return new Snapshot(contextKey, minY, maxY, revision, List.of());
        }

        int heightCount = Math.max(1, maxY - minY + 1);
        Map<String, MutableProfile> profiles = new LinkedHashMap<>();
        List<JsonObject> placedFeatures = new ArrayList<>();
        collectPlacedFeatures(biome.get("features"), placedFeatures);

        for (JsonObject placed : placedFeatures) {
            JsonObject configured = resolveConfiguredFeature(placed.get("feature"));
            if (configured == null || !isOreFeature(configured)) {
                continue;
            }

            JsonObject config = object(configured.get("config"));
            if (config == null) {
                continue;
            }
            Map<String, Descriptor> descriptors = readDescriptors(config);
            if (descriptors.isEmpty()) {
                continue;
            }

            HeightProfile height = readHeightProfile(placed, minY, maxY);
            if (height == null) {
                continue;
            }
            double placementWeight = readPlacementWeight(placed);
            double size = Math.max(1.0, number(config.get("size"), 1.0));
            double discard = clamp(number(config.get("discard_chance_on_air_exposure"), 0.0), 0.0, 1.0);
            float featureWeight = (float) (placementWeight * size * (1.0 - (discard * 0.5)));

            for (Descriptor descriptor : descriptors.values()) {
                MutableProfile profile = profiles.computeIfAbsent(
                    descriptor.key,
                    ignored -> new MutableProfile(descriptor, new float[heightCount])
                );
                for (int y = height.minY; y <= height.maxY; y++) {
                    profile.scores[y - minY] += (float) (height.weight.applyAsDouble(y) * featureWeight);
                }
            }
        }

        List<Profile> normalized = new ArrayList<>(profiles.size());
        for (MutableProfile profile : profiles.values()) {
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
            normalized.add(new Profile(profile.descriptor, profile.scores));
        }
        normalized.sort((left, right) -> left.descriptor.key.compareTo(right.descriptor.key));
        return new Snapshot(contextKey, minY, maxY, revision, List.copyOf(normalized));
    }

    static List<String> oreKeys(Snapshot snapshot) {
        return snapshot.ores.stream().map(profile -> profile.descriptor.key).toList();
    }

    static float scoreAt(Snapshot snapshot, String oreKey, int y) {
        int yIndex = Math.max(snapshot.minY, Math.min(snapshot.maxY, y)) - snapshot.minY;
        return snapshot.ores.stream()
            .filter(profile -> profile.descriptor.key.equals(oreKey))
            .findFirst()
            .map(profile -> profile.scores[yIndex])
            .orElse(0.0f);
    }

    private static void collectPlacedFeatures(JsonElement element, List<JsonObject> out) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectPlacedFeatures(child, out);
            }
            return;
        }
        if (element.isJsonPrimitive()) {
            Identifier id = Identifier.tryParse(element.getAsString());
            JsonObject placed = id == null ? null : readWorldgenJson("placed_feature", id);
            if (placed != null) {
                out.add(placed);
            }
            return;
        }
        if (element.isJsonObject() && element.getAsJsonObject().has("feature")) {
            out.add(element.getAsJsonObject());
        }
    }

    private static JsonObject resolveConfiguredFeature(JsonElement feature) {
        if (feature == null || feature.isJsonNull()) {
            return null;
        }
        if (feature.isJsonObject()) {
            return feature.getAsJsonObject();
        }
        if (feature.isJsonPrimitive()) {
            Identifier id = Identifier.tryParse(feature.getAsString());
            return id == null ? null : readWorldgenJson("configured_feature", id);
        }
        return null;
    }

    private static boolean isOreFeature(JsonObject configured) {
        String type = string(configured, "type");
        if ("minecraft:ore".equals(type) || "minecraft:scattered_ore".equals(type)) {
            return true;
        }
        JsonObject config = object(configured.get("config"));
        return config != null && config.has("targets") && config.has("size");
    }

    private static Map<String, Descriptor> readDescriptors(JsonObject config) {
        Map<String, Descriptor> descriptors = new LinkedHashMap<>();
        JsonArray targets = array(config.get("targets"));
        if (targets == null) {
            return descriptors;
        }
        for (JsonElement targetElement : targets) {
            JsonObject target = object(targetElement);
            JsonObject state = target == null ? null : object(target.get("state"));
            String rawId = state == null ? "" : string(state, "Name");
            Identifier blockId = Identifier.tryParse(rawId);
            if (blockId == null) {
                continue;
            }
            String normalizedPath = RuntimeWorldgenProvider.normalizeOrePath(blockId.getPath());
            if (normalizedPath == null) {
                continue;
            }
            String key = blockId.getNamespace() + ":" + normalizedPath;
            Descriptor existing = descriptors.get(key);
            if (existing == null || preferredBlock(blockId.getPath())) {
                descriptors.put(key, new Descriptor(
                    key,
                    "block." + blockId.getNamespace() + "." + blockId.getPath(),
                    blockId
                ));
            }
        }
        return descriptors;
    }

    private static HeightProfile readHeightProfile(JsonObject placed, int worldMinY, int worldMaxY) {
        JsonArray placements = array(placed.get("placement"));
        if (placements == null) {
            return null;
        }
        for (JsonElement element : placements) {
            JsonObject placement = object(element);
            if (placement == null || !"minecraft:height_range".equals(string(placement, "type"))) {
                continue;
            }
            JsonObject height = object(placement.get("height"));
            if (height == null) {
                return null;
            }
            int distributionMin = readYOffset(height.get("min_inclusive"), worldMinY, worldMaxY);
            int distributionMax = readYOffset(height.get("max_inclusive"), worldMinY, worldMaxY);
            int min = Math.max(worldMinY, distributionMin);
            int max = Math.min(worldMaxY, distributionMax);
            if (min > max) {
                return null;
            }
            String type = string(height, "type");
            if ("minecraft:uniform".equals(type)) {
                return new HeightProfile(min, max, ignored -> 1.0);
            }
            return new HeightProfile(min, max, triangular(distributionMin, distributionMax));
        }
        return null;
    }

    private static IntToDoubleFunction triangular(int min, int max) {
        if (min >= max) {
            return ignored -> 1.0;
        }
        double midpoint = (min + max) / 2.0;
        double leftSpan = Math.max(1.0, midpoint - min + 1.0);
        double rightSpan = Math.max(1.0, max - midpoint + 1.0);
        return y -> y <= midpoint
            ? Math.max(0.0, (y - min + 1.0) / leftSpan)
            : Math.max(0.0, (max - y + 1.0) / rightSpan);
    }

    private static double readPlacementWeight(JsonObject placed) {
        JsonArray placements = array(placed.get("placement"));
        if (placements == null) {
            return 1.0;
        }
        double weight = 1.0;
        for (JsonElement element : placements) {
            JsonObject placement = object(element);
            if (placement == null) {
                continue;
            }
            String type = string(placement, "type");
            if ("minecraft:count".equals(type)) {
                weight *= expectedInt(placement.get("count"), 1.0);
            } else if ("minecraft:rarity_filter".equals(type)) {
                weight *= 1.0 / Math.max(1.0, number(placement.get("chance"), 1.0));
            } else if ("minecraft:count_extra".equals(type)) {
                double base = expectedInt(placement.get("count"), 1.0);
                double extra = expectedInt(placement.get("extra_count"), 0.0);
                double chance = clamp(number(placement.get("extra_chance"), 0.0), 0.0, 1.0);
                weight *= base + (extra * chance);
            }
        }
        return Math.max(0.0, weight);
    }

    private static double expectedInt(JsonElement element, double fallback) {
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        if (element.isJsonPrimitive()) {
            return number(element, fallback);
        }
        JsonObject object = object(element);
        if (object == null) {
            return fallback;
        }
        String type = string(object, "type");
        if ("minecraft:constant".equals(type)) {
            return number(object.get("value"), fallback);
        }
        double min = number(object.get("min_inclusive"), fallback);
        double max = number(object.get("max_inclusive"), fallback);
        if ("minecraft:biased_to_bottom".equals(type)) {
            return min + ((max - min) / 3.0);
        }
        return (min + max) / 2.0;
    }

    private static int readYOffset(JsonElement element, int worldMinY, int worldMaxY) {
        if (element != null && element.isJsonPrimitive()) {
            String value = element.getAsString();
            if ("minecraft:bottom".equals(value)) {
                return worldMinY;
            }
            if ("minecraft:top".equals(value)) {
                return worldMaxY;
            }
        }
        JsonObject offset = object(element);
        if (offset == null) {
            return worldMinY;
        }
        if (offset.has("absolute")) {
            return (int) Math.round(number(offset.get("absolute"), worldMinY));
        }
        if (offset.has("above_bottom")) {
            return worldMinY + (int) Math.round(number(offset.get("above_bottom"), 0.0));
        }
        if (offset.has("below_top")) {
            return worldMaxY - (int) Math.round(number(offset.get("below_top"), 0.0));
        }
        return worldMinY;
    }

    private static JsonObject readWorldgenJson(String category, Identifier id) {
        String path = "data/" + id.getNamespace() + "/worldgen/" + category + "/" + id.getPath() + ".json";
        ClassLoader loader = ClasspathWorldgenProvider.class.getClassLoader();
        try (InputStream stream = loader.getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonElement parsed = JsonParser.parseReader(reader);
                return parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
            }
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    private static boolean preferredBlock(String path) {
        return !(path.startsWith("deepslate_")
            || path.startsWith("stone_")
            || path.startsWith("netherrack_")
            || path.startsWith("blackstone_")
            || path.startsWith("end_stone_"));
    }

    private static JsonObject object(JsonElement element) {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static JsonArray array(JsonElement element) {
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
    }

    private static String string(JsonObject object, String key) {
        try {
            return object != null && object.has(key) ? object.get(key).getAsString() : "";
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static double number(JsonElement element, double fallback) {
        try {
            return element == null ? fallback : element.getAsDouble();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
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
        String path = Identifier.parse(snapshot.ores.get(index).descriptor.key).getPath().replace('_', ' ');
        return path.substring(0, 1).toUpperCase(Locale.ROOT) + path.substring(1);
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
        Identifier blockId = snapshot.ores.get(index).descriptor.blockId;
        return BuiltInRegistries.BLOCK.getOptional(blockId).map(block -> block.asItem()).orElse(null);
    }

    @Override
    public void fillScores(int y, float[] outScores) {
        Snapshot current = snapshot;
        java.util.Arrays.fill(outScores, 0.0f);
        int yIndex = Math.max(current.minY, Math.min(current.maxY, y)) - current.minY;
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
        private final List<Profile> ores;

        private Snapshot(String contextKey, int minY, int maxY, long revision, List<Profile> ores) {
            this.contextKey = contextKey;
            this.minY = minY;
            this.maxY = maxY;
            this.revision = revision;
            this.ores = ores;
        }
    }

    private record Descriptor(String key, String translationKey, Identifier blockId) {
    }

    private record Profile(Descriptor descriptor, float[] scores) {
    }

    private record MutableProfile(Descriptor descriptor, float[] scores) {
    }

    private record HeightProfile(int minY, int maxY, IntToDoubleFunction weight) {
    }
}
