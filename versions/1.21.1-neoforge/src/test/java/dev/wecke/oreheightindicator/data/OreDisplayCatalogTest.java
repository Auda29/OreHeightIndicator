package dev.wecke.oreheightindicator.data;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OreDisplayCatalogTest {
    @Test
    void includesAllVanillaOresBeforeWorldDetection() {
        List<OreDisplayCatalog.OreOption> options = OreDisplayCatalog.knownOresIncluding(List.of());

        for (String key : List.of(
            "minecraft:coal_ore",
            "minecraft:copper_ore",
            "minecraft:iron_ore",
            "minecraft:gold_ore",
            "minecraft:redstone_ore",
            "minecraft:lapis_ore",
            "minecraft:diamond_ore",
            "minecraft:emerald_ore",
            "minecraft:nether_gold_ore",
            "minecraft:nether_quartz_ore",
            "minecraft:ancient_debris"
        )) {
            option(options, key);
        }

        assertEquals("block.minecraft.diamond_ore", option(options, "minecraft:diamond_ore").translationKey());
    }

    @Test
    void combinesDetectedOresWithPreviouslyHiddenKeys() {
        OreDisplayCatalog.remember(
            "examplemod:tin_ore",
            "Tin Ore",
            "block.examplemod.tin_ore"
        );

        List<OreDisplayCatalog.OreOption> options = OreDisplayCatalog.knownOresIncluding(
            List.of("oldmod:silver_ore")
        );

        OreDisplayCatalog.OreOption tin = option(options, "examplemod:tin_ore");
        assertEquals("Tin Ore", tin.fallbackName());
        assertEquals("block.examplemod.tin_ore", tin.translationKey());

        OreDisplayCatalog.OreOption silver = option(options, "oldmod:silver_ore");
        assertEquals("Silver Ore", silver.fallbackName());
        assertEquals("", silver.translationKey());
    }

    private static OreDisplayCatalog.OreOption option(
        List<OreDisplayCatalog.OreOption> options,
        String key
    ) {
        return options.stream()
            .filter(option -> option.key().equals(key))
            .findFirst()
            .orElseThrow();
    }
}
