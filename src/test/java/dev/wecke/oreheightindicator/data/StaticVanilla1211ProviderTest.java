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
    void deepLayerFavorsDiamondAndRedstoneOverSurfaceOres() {
        StaticVanilla1211Provider provider = new StaticVanilla1211Provider();
        float[] out = new float[provider.oreCount()];

        provider.fillScores(-48, out);
        float coal = out[0];
        float redstone = out[4];
        float diamond = out[6];

        assertTrue(redstone > coal);
        assertTrue(diamond > coal);
    }

    @Test
    void midLayerFavorsCopperAndIronOverDiamond() {
        StaticVanilla1211Provider provider = new StaticVanilla1211Provider();
        float[] out = new float[provider.oreCount()];

        provider.fillScores(48, out);
        float copper = out[1];
        float iron = out[2];
        float diamond = out[6];

        assertTrue(copper > diamond);
        assertTrue(iron > diamond);
    }

    @Test
    void highLayerFavorsCoalAndEmeraldOverDeepOres() {
        StaticVanilla1211Provider provider = new StaticVanilla1211Provider();
        float[] out = new float[provider.oreCount()];

        provider.fillScores(200, out);
        float coal = out[0];
        float redstone = out[4];
        float diamond = out[6];
        float emerald = out[7];

        assertTrue(coal > redstone);
        assertTrue(coal > diamond);
        assertTrue(emerald > redstone);
    }

    @Test
    void goldStopsAtHigherYLevels() {
        StaticVanilla1211Provider provider = new StaticVanilla1211Provider();
        float[] out = new float[provider.oreCount()];

        provider.fillScores(120, out);
        assertEquals(0.0f, out[3], 1.0e-6f);

        provider.fillScores(-56, out);
        assertTrue(out[3] > 0.0f);
    }
}
