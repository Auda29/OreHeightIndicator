package dev.wecke.oreheightindicator.data;

import org.junit.jupiter.api.Test;

import java.util.List;
import net.minecraft.resources.Identifier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClasspathWorldgenProviderTest {
    @Test
    void plainsProfileComesFromInstalledMinecraftWorldgenData() {
        ClasspathWorldgenProvider.Snapshot snapshot = ClasspathWorldgenProvider.buildSnapshot(
            "test-plains",
            Identifier.fromNamespaceAndPath("minecraft", "plains"),
            -64,
            319,
            1L
        );

        List<String> ores = ClasspathWorldgenProvider.oreKeys(snapshot);
        assertTrue(ores.contains("minecraft:coal_ore"));
        assertTrue(ores.contains("minecraft:diamond_ore"));
        assertTrue(ores.contains("minecraft:copper_ore"));
        assertFalse(ores.contains("minecraft:emerald_ore"));
        float diamondAtMinus55 = ClasspathWorldgenProvider.scoreAt(snapshot, "minecraft:diamond_ore", -55);
        float diamondAtMinus30 = ClasspathWorldgenProvider.scoreAt(snapshot, "minecraft:diamond_ore", -30);
        float diamondAtMinus16 = ClasspathWorldgenProvider.scoreAt(snapshot, "minecraft:diamond_ore", -16);
        assertTrue(diamondAtMinus55 > diamondAtMinus30);
        assertTrue(diamondAtMinus30 > diamondAtMinus16);
        assertTrue(ClasspathWorldgenProvider.scoreAt(snapshot, "minecraft:diamond_ore", 72) == 0.0f);
    }

    @Test
    void mountainAndNetherBiomesSelectTheirOwnOreFeatures() {
        ClasspathWorldgenProvider.Snapshot mountain = ClasspathWorldgenProvider.buildSnapshot(
            "test-mountain",
            Identifier.fromNamespaceAndPath("minecraft", "stony_peaks"),
            -64,
            319,
            2L
        );
        ClasspathWorldgenProvider.Snapshot nether = ClasspathWorldgenProvider.buildSnapshot(
            "test-nether",
            Identifier.fromNamespaceAndPath("minecraft", "nether_wastes"),
            0,
            127,
            3L
        );

        assertTrue(ClasspathWorldgenProvider.oreKeys(mountain).contains("minecraft:emerald_ore"));
        assertTrue(ClasspathWorldgenProvider.oreKeys(nether).contains("minecraft:nether_quartz_ore"));
        assertTrue(ClasspathWorldgenProvider.oreKeys(nether).contains("minecraft:ancient_debris"));
        assertFalse(ClasspathWorldgenProvider.oreKeys(nether).contains("minecraft:diamond_ore"));
    }
}
