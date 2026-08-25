package dev.wecke.oreheightindicator.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

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
        return ores;
    }

    public static synchronized void rememberRegisteredBlocks() {
        for (ResourceLocation blockId : BuiltInRegistries.BLOCK.keySet()) {
            Block block = BuiltInRegistries.BLOCK.getOptional(blockId).orElse(null);
            if (block != null) rememberRegisteredBlock(blockId.toString(), block.getDescriptionId());
        }
    }

    static synchronized void rememberRegisteredBlock(String rawKey, String translationKey) {
        ResourceLocation blockId = ResourceLocation.tryParse(rawKey);
        if (blockId == null) return;
        String normalizedOrePath = RuntimeWorldgenProvider.normalizeOrePath(blockId.getPath());
        String key = normalizedOrePath == null ? blockId.toString() : blockId.getNamespace() + ":" + normalizedOrePath;
        OreOption existing = KNOWN_TARGETS.get(key);
        boolean preferred = normalizedOrePath == null || preferredOreBlock(blockId.getPath());
        if (existing == null || preferred) {
            KNOWN_TARGETS.put(key, new OreOption(key, fallbackName(key), translationKey == null ? "" : translationKey, isStandardOre(key)));
        }
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

    public static synchronized List<OreOption> displayedOptionsIncluding(Collection<String> additionalKeys) {
        Set<String> selectedKeys = normalizedKeys(additionalKeys);
        return knownOresIncluding(selectedKeys).stream()
            .filter(option -> option.standardOre() || selectedKeys.contains(option.key()))
            .toList();
    }

    public static synchronized List<OreOption> searchableBlocksExcluding(Collection<String> excludedKeys) {
        Set<String> excluded = normalizedKeys(excludedKeys);
        return KNOWN_TARGETS.values().stream()
            .filter(option -> !option.standardOre())
            .filter(option -> !excluded.contains(option.key()))
            .toList();
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

    private static Set<String> normalizedKeys(Collection<String> keys) {
        Set<String> normalized = new HashSet<>();
        for (String key : keys) {
            if (key != null && !key.isBlank()) {
                normalized.add(key.trim().toLowerCase(Locale.ROOT));
            }
        }
        return normalized;
    }

    private static boolean preferredOreBlock(String path) {
        return !(path.startsWith("deepslate_") || path.startsWith("stone_") || path.startsWith("netherrack_")
            || path.startsWith("blackstone_") || path.startsWith("end_stone_"));
    }

    public record OreOption(String key, String fallbackName, String translationKey, boolean standardOre) {
    }
}
