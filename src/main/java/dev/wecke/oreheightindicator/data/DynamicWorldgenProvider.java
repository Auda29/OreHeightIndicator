package dev.wecke.oreheightindicator.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntToDoubleFunction;

/**
 * Experimental provider that derives ore score curves from vanilla placed-feature worldgen JSON.
 * This keeps fillScores(...) cheap by doing all heavy parsing once during construction.
 */
public final class DynamicWorldgenProvider implements OreDataProvider {
    private static final int MIN_Y = -64;
    private static final int MAX_Y = 320;
    private static final int HEIGHT_COUNT = (MAX_Y - MIN_Y) + 1;
    private static final String[] ORE_NAMES = {"Coal", "Copper", "Iron", "Gold", "Redstone", "Lapis", "Diamond", "Emerald"};
    private static final Map<String, String[]> ORE_TO_FEATURES = Map.of(
        "Coal", new String[] {"ore_coal_upper", "ore_coal_lower", "ore_coal_buried"},
        "Copper", new String[] {"ore_copper", "ore_copper_large"},
        "Iron", new String[] {"ore_iron_upper", "ore_iron_middle", "ore_iron_small"},
        "Gold", new String[] {"ore_gold", "ore_gold_lower", "ore_gold_extra"},
        "Redstone", new String[] {"ore_redstone", "ore_redstone_lower"},
        "Lapis", new String[] {"ore_lapis", "ore_lapis_buried"},
        "Diamond", new String[] {"ore_diamond", "ore_diamond_large", "ore_diamond_buried"},
        "Emerald", new String[] {"ore_emerald"}
    );

    private final float[][] scoresByOre;

    public DynamicWorldgenProvider() {
        this(buildRuntimeScores());
    }

    DynamicWorldgenProvider(float[][] scoresByOre) {
        validateScores(scoresByOre);
        this.scoresByOre = scoresByOre;
    }

    @Override
    public int minY() {
        return MIN_Y;
    }

    @Override
    public int maxY() {
        return MAX_Y;
    }

    @Override
    public int oreCount() {
        return ORE_NAMES.length;
    }

    @Override
    public String oreName(int index) {
        return ORE_NAMES[index];
    }

    @Override
    public void fillScores(int y, float[] outScores) {
        int clampedY = Math.max(MIN_Y, Math.min(MAX_Y, y));
        int yIndex = clampedY - MIN_Y;
        for (int i = 0; i < ORE_NAMES.length; i++) {
            outScores[i] = scoresByOre[i][yIndex];
        }
    }

    static float[][] buildRuntimeScores() {
        ResourceManager resourceManager = getResourceManagerOrThrow();
        float[][] scores = new float[ORE_NAMES.length][HEIGHT_COUNT];

        int oresWithData = 0;
        for (int oreIndex = 0; oreIndex < ORE_NAMES.length; oreIndex++) {
            String oreName = ORE_NAMES[oreIndex];
            String[] featureIds = ORE_TO_FEATURES.getOrDefault(oreName, new String[0]);
            boolean foundAny = false;

            for (String featureId : featureIds) {
                JsonObject featureJson = readPlacedFeature(resourceManager, featureId);
                if (featureJson == null) {
                    continue;
                }

                foundAny = true;
                HeightProfile heightProfile = readHeightProfile(featureJson);
                double countWeight = readCountWeight(featureJson);
                double sizeWeight = readConfiguredFeatureSizeWeight(resourceManager, featureJson);
                accumulate(scores[oreIndex], heightProfile, countWeight * sizeWeight);
            }

            if (foundAny) {
                oresWithData++;
            }
        }

        if (oresWithData < 4) {
            throw new IllegalStateException("Could not read enough vanilla placed features for dynamic provider.");
        }

        // Anchor dynamic curves against a known vanilla baseline so unsupported/biome-local
        // features cannot dominate globally with implausible values.
        applyStaticVanillaAnchor(scores);
        return scores;
    }

    private static ResourceManager getResourceManagerOrThrow() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getResourceManager() == null) {
            throw new IllegalStateException("Minecraft resource manager is not ready.");
        }
        return client.getResourceManager();
    }

    private static JsonObject readPlacedFeature(ResourceManager resourceManager, String featureId) {
        Identifier id = Identifier.of("minecraft", "worldgen/placed_feature/" + featureId + ".json");
        Optional<Resource> resource = resourceManager.getResource(id);
        if (resource.isEmpty()) {
            return null;
        }
        try (Reader reader = new InputStreamReader(resource.get().getInputStream(), StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            return parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    private static void accumulate(float[] targetScores, HeightProfile profile, double countWeight) {
        if (profile == null || countWeight <= 0.0) {
            return;
        }
        for (int y = profile.minYInclusive; y <= profile.maxYInclusive; y++) {
            int yIndex = y - MIN_Y;
            if (yIndex < 0 || yIndex >= HEIGHT_COUNT) {
                continue;
            }
            double shape = profile.weightAt.applyAsDouble(y);
            if (shape <= 0.0) {
                continue;
            }
            targetScores[yIndex] += (float) (shape * countWeight);
        }
    }

    private static void applyStaticVanillaAnchor(float[][] scores) {
        StaticVanilla1211Provider staticProvider = new StaticVanilla1211Provider();
        float[] baseline = new float[staticProvider.oreCount()];
        for (int y = MIN_Y; y <= MAX_Y; y++) {
            staticProvider.fillScores(y, baseline);
            int yIndex = y - MIN_Y;
            for (int oreIndex = 0; oreIndex < ORE_NAMES.length; oreIndex++) {
                float anchor = Math.max(0.0f, baseline[oreIndex]);
                scores[oreIndex][yIndex] *= anchor;
            }
        }
    }

    private static HeightProfile readHeightProfile(JsonObject placedFeatureJson) {
        if (placedFeatureJson == null || !placedFeatureJson.has("placement") || !placedFeatureJson.get("placement").isJsonArray()) {
            return null;
        }

        for (JsonElement element : placedFeatureJson.getAsJsonArray("placement")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject placement = element.getAsJsonObject();
            if (!isType(placement, "minecraft:height_range")) {
                continue;
            }

            JsonObject heightObject = asObject(placement.get("height"));
            if (heightObject == null) {
                continue;
            }

            int min = readYOffset(heightObject.get("min_inclusive"));
            int max = readYOffset(heightObject.get("max_inclusive"));
            if (min > max) {
                int tmp = min;
                min = max;
                max = tmp;
            }

            final int profileMin = Math.max(MIN_Y, min);
            final int profileMax = Math.min(MAX_Y, max);
            if (profileMin > profileMax) {
                continue;
            }

            String distributionType = readString(heightObject, "type");
            IntToDoubleFunction fn = switch (distributionType) {
                case "minecraft:uniform" -> (y) -> 1.0;
                case "minecraft:trapezoid" -> trapezoid(profileMin, profileMax);
                case "minecraft:biased_to_bottom", "minecraft:very_biased_to_bottom" -> biasedToBottom(profileMin, profileMax);
                default -> trapezoid(profileMin, profileMax);
            };

            return new HeightProfile(profileMin, profileMax, fn);
        }
        return null;
    }

    private static IntToDoubleFunction trapezoid(int min, int max) {
        if (min == max) {
            return (y) -> 1.0;
        }
        double mid = (min + max) / 2.0;
        double leftSpan = Math.max(1.0, mid - min);
        double rightSpan = Math.max(1.0, max - mid);
        return (y) -> {
            if (y < min || y > max) {
                return 0.0;
            }
            if (y <= mid) {
                return Math.max(0.0, (y - min) / leftSpan);
            }
            return Math.max(0.0, (max - y) / rightSpan);
        };
    }

    private static IntToDoubleFunction biasedToBottom(int min, int max) {
        if (min == max) {
            return (y) -> 1.0;
        }
        double span = Math.max(1.0, max - min);
        return (y) -> {
            if (y < min || y > max) {
                return 0.0;
            }
            double t = (max - y) / span;
            return t * t;
        };
    }

    private static double readCountWeight(JsonObject placedFeatureJson) {
        if (placedFeatureJson == null || !placedFeatureJson.has("placement") || !placedFeatureJson.get("placement").isJsonArray()) {
            return 1.0;
        }

        for (JsonElement element : placedFeatureJson.getAsJsonArray("placement")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject placement = element.getAsJsonObject();
            String type = readString(placement, "type");
            if ("minecraft:count".equals(type)) {
                return Math.max(0.0, readIntProviderExpected(placement.get("count"), 1.0));
            }
            if ("minecraft:count_extra".equals(type)) {
                double count = Math.max(0.0, readIntProviderExpected(placement.get("count"), 1.0));
                double extraCount = Math.max(0.0, readIntProviderExpected(placement.get("extra_count"), 0.0));
                double extraChance = Math.max(0.0, Math.min(1.0, readNumberOrFallback(placement.get("extra_chance"), 0.0)));
                return count + (extraCount * extraChance);
            }
        }
        return 1.0;
    }

    private static boolean isType(JsonObject object, String expectedType) {
        return expectedType.equals(readString(object, "type"));
    }

    private static int readYOffset(JsonElement element) {
        if (element != null && element.isJsonPrimitive()) {
            try {
                String literal = element.getAsString();
                if ("minecraft:bottom".equals(literal)) {
                    return MIN_Y;
                }
                if ("minecraft:top".equals(literal)) {
                    return MAX_Y;
                }
            } catch (RuntimeException ignored) {
            }
        }

        JsonObject object = asObject(element);
        if (object == null) {
            return MIN_Y;
        }
        if (object.has("absolute")) {
            return (int) Math.round(readNumberOrFallback(object.get("absolute"), MIN_Y));
        }
        if (object.has("above_bottom")) {
            return MIN_Y + (int) Math.round(readNumberOrFallback(object.get("above_bottom"), 0.0));
        }
        if (object.has("below_top")) {
            return MAX_Y - (int) Math.round(readNumberOrFallback(object.get("below_top"), 0.0));
        }
        return MIN_Y;
    }

    private static JsonObject asObject(JsonElement element) {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static String readString(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonPrimitive()) {
            return "";
        }
        try {
            return object.get(key).getAsString();
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static double readNumberOrFallback(JsonElement element, double fallback) {
        if (element == null) {
            return fallback;
        }
        if (element.isJsonPrimitive()) {
            try {
                return element.getAsDouble();
            } catch (RuntimeException ignored) {
                return fallback;
            }
        }
        JsonObject object = asObject(element);
        if (object == null) {
            return fallback;
        }
        if (object.has("value")) {
            return readNumberOrFallback(object.get("value"), fallback);
        }
        if (object.has("inclusive")) {
            return readNumberOrFallback(object.get("inclusive"), fallback);
        }
        if (object.has("max_inclusive")) {
            return readNumberOrFallback(object.get("max_inclusive"), fallback);
        }
        if (object.has("min_inclusive")) {
            return readNumberOrFallback(object.get("min_inclusive"), fallback);
        }
        return fallback;
    }

    private static double readIntProviderExpected(JsonElement element, double fallback) {
        if (element == null) {
            return fallback;
        }
        if (element.isJsonPrimitive()) {
            return readNumberOrFallback(element, fallback);
        }

        JsonObject object = asObject(element);
        if (object == null) {
            return fallback;
        }

        String type = readString(object, "type");
        if ("minecraft:constant".equals(type)) {
            return readNumberOrFallback(object.get("value"), fallback);
        }
        if ("minecraft:uniform".equals(type)) {
            double min = readNumberOrFallback(object.get("min_inclusive"), fallback);
            double max = readNumberOrFallback(object.get("max_inclusive"), fallback);
            return (min + max) / 2.0;
        }
        if ("minecraft:biased_to_bottom".equals(type)) {
            double min = readNumberOrFallback(object.get("min_inclusive"), fallback);
            double max = readNumberOrFallback(object.get("max_inclusive"), fallback);
            return min + ((max - min) / 3.0);
        }
        return readNumberOrFallback(element, fallback);
    }

    private static double readConfiguredFeatureSizeWeight(ResourceManager resourceManager, JsonObject placedFeatureJson) {
        String configuredFeatureRef = readConfiguredFeatureRef(placedFeatureJson);
        if (configuredFeatureRef.isEmpty()) {
            return 1.0;
        }

        Identifier configuredId = Identifier.tryParse(configuredFeatureRef);
        if (configuredId == null) {
            return 1.0;
        }

        Identifier resourceId = Identifier.of(
            configuredId.getNamespace(),
            "worldgen/configured_feature/" + configuredId.getPath() + ".json"
        );
        Optional<Resource> resource = resourceManager.getResource(resourceId);
        if (resource.isEmpty()) {
            return 1.0;
        }

        try (Reader reader = new InputStreamReader(resource.get().getInputStream(), StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            JsonObject configured = parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
            if (configured == null) {
                return 1.0;
            }
            JsonObject config = asObject(configured.get("config"));
            if (config == null) {
                return 1.0;
            }

            double size = Math.max(1.0, readNumberOrFallback(config.get("size"), 1.0));
            double discard = Math.max(0.0, Math.min(1.0, readNumberOrFallback(config.get("discard_chance_on_air_exposure"), 0.0)));
            return size * (1.0 - (discard * 0.5));
        } catch (IOException | RuntimeException ignored) {
            return 1.0;
        }
    }

    private static String readConfiguredFeatureRef(JsonObject placedFeatureJson) {
        if (placedFeatureJson == null || !placedFeatureJson.has("feature")) {
            return "";
        }

        JsonElement feature = placedFeatureJson.get("feature");
        if (feature != null && feature.isJsonPrimitive()) {
            try {
                return feature.getAsString();
            } catch (RuntimeException ignored) {
                return "";
            }
        }
        return "";
    }

    private static void validateScores(float[][] scoresByOre) {
        if (scoresByOre == null || scoresByOre.length != ORE_NAMES.length) {
            throw new IllegalArgumentException("Dynamic score table does not match ore count.");
        }
        for (float[] oreScores : scoresByOre) {
            if (oreScores == null || oreScores.length != HEIGHT_COUNT) {
                throw new IllegalArgumentException("Dynamic score table does not match Y range.");
            }
        }
    }

    private record HeightProfile(int minYInclusive, int maxYInclusive, IntToDoubleFunction weightAt) {
    }
}
