package dev.wecke.oreheightindicator.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RuntimeWorldgenProviderTest {
    @Test
    void recognizesVanillaAndModdedOreBlockNames() {
        assertEquals("diamond_ore", RuntimeWorldgenProvider.normalizeOrePath("diamond_ore"));
        assertEquals("diamond_ore", RuntimeWorldgenProvider.normalizeOrePath("deepslate_diamond_ore"));
        assertEquals("tin_ore", RuntimeWorldgenProvider.normalizeOrePath("deepslate_tin_ore"));
        assertEquals("uranium_ore", RuntimeWorldgenProvider.normalizeOrePath("end_stone_uranium_ore"));
        assertEquals("ancient_debris", RuntimeWorldgenProvider.normalizeOrePath("ancient_debris"));
    }

    @Test
    void ignoresOreFeatureConfigUsedForOrdinaryStoneBlobs() {
        assertNull(RuntimeWorldgenProvider.normalizeOrePath("granite"));
        assertNull(RuntimeWorldgenProvider.normalizeOrePath("tuff"));
        assertNull(RuntimeWorldgenProvider.normalizeOrePath("clay"));
    }
}
