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

        assertTrue(redstone > coal, "redstone should exceed coal at Y=-48");
        assertTrue(diamond > coal, "diamond should exceed coal at Y=-48");
    }

    @Test
    void midLayerFavorsCopperAndIronOverDiamond() {
        StaticVanilla1211Provider provider = new StaticVanilla1211Provider();
        float[] out = new float[provider.oreCount()];

        provider.fillScores(48, out);
        float copper = out[1];
        float iron = out[2];
        float diamond = out[6];

        assertTrue(copper > diamond, "copper should exceed diamond at Y=48");
        assertTrue(iron > diamond, "iron should exceed diamond at Y=48");
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

        assertTrue(coal > redstone, "coal should exceed redstone at Y=200");
        assertTrue(coal > diamond, "coal should exceed diamond at Y=200");
        assertTrue(emerald > redstone, "emerald should exceed redstone at Y=200");
    }

    @Test
    void goldStopsAtHigherYLevels() {
        StaticVanilla1211Provider provider = new StaticVanilla1211Provider();
        float[] out = new float[provider.oreCount()];

        provider.fillScores(250, out);
        assertEquals(0.0f, out[3], 1.0e-6f, "gold should be zero at Y=250");

        provider.fillScores(-56, out);
        assertTrue(out[3] > 0.0f, "gold should be positive at Y=-56");
    }

    @Test
    void peakYLevelsMatchWikiData() {
        StaticVanilla1211Provider provider = new StaticVanilla1211Provider();
        float[] out = new float[provider.oreCount()];

        // Copper peaks at Y=44 and is the global peak (1.0)
        provider.fillScores(44, out);
        float copperPeak = out[1];
        assertEquals(1.0f, copperPeak, 0.01f, "copper should be 1.0 at Y=44 (global peak)");

        // Coal peaks around Y=45 (~0.868 of global peak)
        provider.fillScores(45, out);
        float coalPeak = out[0];
        assertEquals(0.868f, coalPeak, 0.02f, "coal should peak near Y=45");

        // Diamond peaks around Y=-59 (~0.306 of global peak)
        provider.fillScores(-59, out);
        float diamondPeak = out[6];
        assertEquals(0.306f, diamondPeak, 0.02f, "diamond should peak near Y=-59");

        // Iron peaks around Y=14 (~0.763 of global peak)
        provider.fillScores(14, out);
        float ironPeak = out[2];
        assertEquals(0.763f, ironPeak, 0.02f, "iron should peak near Y=14");
    }

    @Test
    void outsideOreRangeReturnsZero() {
        StaticVanilla1211Provider provider = new StaticVanilla1211Provider();
        float[] out = new float[provider.oreCount()];

        // Diamond doesn't spawn above Y=16
        provider.fillScores(50, out);
        assertEquals(0.0f, out[6], 1.0e-6f, "diamond should be zero at Y=50");

        // Coal doesn't spawn below Y=-38
        provider.fillScores(-50, out);
        assertEquals(0.0f, out[0], 1.0e-6f, "coal should be zero at Y=-50");

        // Copper doesn't spawn below Y=-20
        provider.fillScores(-30, out);
        assertEquals(0.0f, out[1], 1.0e-6f, "copper should be zero at Y=-30");
    }
}
