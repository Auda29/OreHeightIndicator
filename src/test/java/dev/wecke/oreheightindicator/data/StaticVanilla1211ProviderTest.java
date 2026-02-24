package dev.wecke.oreheightindicator.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaticVanilla1211ProviderTest {
    @Test
    void metadataMatchesExpectedValues() {
        StaticVanilla1211Provider provider = new StaticVanilla1211Provider();

        assertEquals(-64, provider.minY());
        assertEquals(320, provider.maxY());
        assertEquals(8, provider.oreCount());
        assertEquals("Coal", provider.oreName(0));
        assertEquals("Copper", provider.oreName(1));
        assertEquals("Iron", provider.oreName(2));
        assertEquals("Gold", provider.oreName(3));
        assertEquals("Redstone", provider.oreName(4));
        assertEquals("Lapis", provider.oreName(5));
        assertEquals("Diamond", provider.oreName(6));
        assertEquals("Emerald", provider.oreName(7));
    }

    @Test
    void fillScoresClampsInputOutsideAllowedYRange() {
        StaticVanilla1211Provider provider = new StaticVanilla1211Provider();

        float[] below = new float[provider.oreCount()];
        float[] atMin = new float[provider.oreCount()];
        float[] above = new float[provider.oreCount()];
        float[] atMax = new float[provider.oreCount()];

        provider.fillScores(-999, below);
        provider.fillScores(provider.minY(), atMin);
        provider.fillScores(999, above);
        provider.fillScores(provider.maxY(), atMax);

        assertArrayEquals(atMin, below, 1.0e-6f);
        assertArrayEquals(atMax, above, 1.0e-6f);
    }

    @Test
    void fillScoresReturnsExpectedPeakValues() {
        StaticVanilla1211Provider provider = new StaticVanilla1211Provider();
        float[] out = new float[provider.oreCount()];

        provider.fillScores(180, out);
        assertEquals(1.0f, out[0], 1.0e-6f);
        provider.fillScores(48, out);
        assertEquals(0.9f, out[1], 1.0e-6f);
        provider.fillScores(16, out);
        assertEquals(0.95f, out[2], 1.0e-6f);
        provider.fillScores(-16, out);
        assertEquals(0.8f, out[3], 1.0e-6f);
        provider.fillScores(-48, out);
        assertEquals(1.0f, out[4], 1.0e-6f);
        provider.fillScores(0, out);
        assertEquals(0.8f, out[5], 1.0e-6f);
        provider.fillScores(-56, out);
        assertEquals(1.0f, out[6], 1.0e-6f);
        provider.fillScores(224, out);
        assertEquals(0.85f, out[7], 1.0e-6f);
    }

    @Test
    void fillScoresReturnsZeroAtConfiguredBoundaries() {
        StaticVanilla1211Provider provider = new StaticVanilla1211Provider();
        float[] out = new float[provider.oreCount()];

        provider.fillScores(0, out);
        assertEquals(0.0f, out[0], 1.0e-6f);
        provider.fillScores(320, out);
        assertEquals(0.0f, out[0], 1.0e-6f);

        provider.fillScores(-16, out);
        assertEquals(0.0f, out[1], 1.0e-6f);
        provider.fillScores(112, out);
        assertEquals(0.0f, out[1], 1.0e-6f);
    }

    @Test
    void fillScoresMatchesKnownMidpointSamples() {
        StaticVanilla1211Provider provider = new StaticVanilla1211Provider();
        float[] out = new float[provider.oreCount()];

        provider.fillScores(90, out);
        assertEquals(0.5f, out[0], 1.0e-6f);
        provider.fillScores(250, out);
        assertEquals(0.5f, out[0], 1.0e-6f);

        provider.fillScores(-32, out);
        assertEquals(0.4f, out[5], 1.0e-6f);
        provider.fillScores(32, out);
        assertEquals(0.4f, out[5], 1.0e-6f);
    }

    @Test
    void yNegative56PrioritizesDiamondOverSurfaceOres() {
        StaticVanilla1211Provider provider = new StaticVanilla1211Provider();
        float[] out = new float[provider.oreCount()];
        provider.fillScores(-56, out);

        float diamond = out[6];
        float coal = out[0];
        assertTrue(diamond > coal);
        assertEquals(1.0f, diamond, 1.0e-6f);
    }
}
