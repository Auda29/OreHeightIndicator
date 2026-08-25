package dev.wecke.oreheightindicator.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public final class OreDisplayCatalog {
    private static final Map<String, OreOption> KNOWN_TARGETS = createInitialCatalog();

    private OreDisplayCatalog() {
    }

    private static Map<String, OreOption> createInitialCatalog() {
        Map<String, OreOption> ores = new LinkedHashMap<>();
        for (String path : List.of(
            "coal_ore",
            "copper_ore",
            "iron_ore",
            "gold_ore",
            "redstone_ore",
            "lapis_ore",
            "diamond_ore",
            "emerald_ore",
            "nether_gold_ore",
            "nether_quartz_ore",
            "ancient_debris"
        )) {
            String key = "minecraft:" + path;
            ores.put(key, new OreOption(key, fallbackName(key), "block.minecraft." + path, true));
        }
        for (String path : List.of("andesite", "diorite", "granite", "tuff")) {
            String key = "minecraft:" + path;
            ores.put(key, new OreOption(key, fallbackName(key), "block.minecraft." + path, false));
        }
        return ores;
    }

    public static synchronized void remember(String key, String fallbackName, String translationKey) {
        if (key == null || key.isBlank()) {
            return;
        }
        KNOWN_TARGETS.put(key, new OreOption(
            key,
            fallbackName == null || fallbackName.isBlank() ? fallbackName(key) : fallbackName,
            translationKey == null ? "" : translationKey,
            isStandardOre(key)
        ));
    }

    public static synchronized List<OreOption> knownOresIncluding(Collection<String> additionalKeys) {
        Map<String, OreOption> combined = new LinkedHashMap<>(KNOWN_TARGETS);
        for (String key : additionalKeys) {
            if (key != null && !key.isBlank()) {
                combined.putIfAbsent(key, new OreOption(key, fallbackName(key), "", isStandardOre(key)));
            }
        }
        return List.copyOf(combined.values());
    }

    public static boolean isStandardOre(String key) {
        if (key == null || key.isBlank()) return false;
        ResourceLocation id = ResourceLocation.tryParse(key);
        String path = id == null ? key : id.getPath();
        return "ancient_debris".equals(path) || path.endsWith("_ore");
    }

    static String fallbackName(String key) {
        ResourceLocation id = ResourceLocation.tryParse(key);
        String path = id == null ? key : id.getPath();
        List<String> words = new ArrayList<>();
        for (String word : path.split("_")) {
            if (!word.isEmpty()) {
                words.add(word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1));
            }
        }
        return words.isEmpty() ? key : String.join(" ", words);
    }

    public record OreOption(String key, String fallbackName, String translationKey, boolean standardOre) {
    }
}
