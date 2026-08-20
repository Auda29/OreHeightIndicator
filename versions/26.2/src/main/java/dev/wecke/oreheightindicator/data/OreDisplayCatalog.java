package dev.wecke.oreheightindicator.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.resources.Identifier;

public final class OreDisplayCatalog {
    private static final Map<String, OreOption> KNOWN_ORES = createInitialCatalog();

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
            ores.put(key, new OreOption(key, fallbackName(key), "block.minecraft." + path));
        }
        return ores;
    }

    public static synchronized void remember(String key, String fallbackName, String translationKey) {
        if (key == null || key.isBlank()) {
            return;
        }
        KNOWN_ORES.put(key, new OreOption(
            key,
            fallbackName == null || fallbackName.isBlank() ? fallbackName(key) : fallbackName,
            translationKey == null ? "" : translationKey
        ));
    }

    public static synchronized List<OreOption> knownOresIncluding(Collection<String> additionalKeys) {
        Map<String, OreOption> combined = new LinkedHashMap<>(KNOWN_ORES);
        for (String key : additionalKeys) {
            if (key != null && !key.isBlank()) {
                combined.putIfAbsent(key, new OreOption(key, fallbackName(key), ""));
            }
        }
        return List.copyOf(combined.values());
    }

    static String fallbackName(String key) {
        Identifier id = Identifier.tryParse(key);
        String path = id == null ? key : id.getPath();
        List<String> words = new ArrayList<>();
        for (String word : path.split("_")) {
            if (!word.isEmpty()) {
                words.add(word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1));
            }
        }
        return words.isEmpty() ? key : String.join(" ", words);
    }

    public record OreOption(String key, String fallbackName, String translationKey) {
    }
}
