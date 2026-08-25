package dev.wecke.oreheightindicator.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModConfigTest {
    @Test
    void oresAreVisibleByDefaultAndCanBeHiddenAgain() {
        ModConfig config = new ModConfig();

        assertTrue(Boolean.TRUE.equals(config.showSuitabilityPercent));
        assertTrue(config.isOreVisible("minecraft:copper_ore"));

        config.setOreVisible("minecraft:copper_ore", false);
        assertFalse(config.isOreVisible("minecraft:copper_ore"));

        config.setOreVisible("minecraft:copper_ore", true);
        assertTrue(config.isOreVisible("minecraft:copper_ore"));
    }

    @Test
    void oreKeysAreNormalizedBeforeTheyAreStored() {
        ModConfig config = new ModConfig();

        config.setOreVisible(" ExampleMod:Tin_Ore ", false);

        assertFalse(config.isOreVisible("examplemod:tin_ore"));
        assertTrue(config.hiddenOreKeys().contains("examplemod:tin_ore"));
    }

    @Test
    void additionalMaterialsAreOptInAndPersistAsNormalizedKeys() {
        ModConfig config = new ModConfig();
        assertFalse(config.isDisplayed("minecraft:andesite"));
        config.setMaterialTracked(" Minecraft:Andesite ", true);
        assertTrue(config.isMaterialTracked("minecraft:andesite"));
        assertTrue(config.isDisplayed("minecraft:andesite"));
        assertTrue(config.trackedMaterialKeys().contains("minecraft:andesite"));
        config.setMaterialTracked("minecraft:andesite", false);
        assertFalse(config.isDisplayed("minecraft:andesite"));
    }
}
