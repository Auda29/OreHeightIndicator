package dev.wecke.oreheightindicator.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DynamicWorldgenProviderTest {
    private static final int MIN_Y = -64;
    private static final int MAX_Y = 320;
    private static final int ORE_COUNT = 8;
    private static final int HEIGHT_COUNT = (MAX_Y - MIN_Y) + 1;

    @Test
    void metadataMatchesExpectedValues() {
        DynamicWorldgenProvider provider = new DynamicWorldgenProvider(emptyScores());

        assertEquals(MIN_Y, provider.minY());
        assertEquals(MAX_Y, provider.maxY());
        assertEquals(ORE_COUNT, provider.oreCount());
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
    void fillScoresClampsYAndReturnsDeterministicValues() {
        float[][] scores = emptyScores();
        scores[0][0] = 11.0f; // Coal at min Y
        scores[1][64] = 7.0f; // Copper at Y=0
        scores[7][HEIGHT_COUNT - 1] = 5.0f; // Emerald at max Y
        DynamicWorldgenProvider provider = new DynamicWorldgenProvider(scores);

        float[] out = new float[provider.oreCount()];
        provider.fillScores(-999, out);
        assertEquals(11.0f, out[0], 1.0e-6f);
        assertEquals(0.0f, out[1], 1.0e-6f);

        provider.fillScores(0, out);
        assertEquals(7.0f, out[1], 1.0e-6f);
        float copperAtZero = out[1];

        provider.fillScores(999, out);
        assertEquals(5.0f, out[7], 1.0e-6f);

        float[] outAgain = new float[provider.oreCount()];
        provider.fillScores(0, outAgain);
        assertEquals(copperAtZero, outAgain[1], 1.0e-6f);
    }

    private static float[][] emptyScores() {
        return new float[ORE_COUNT][HEIGHT_COUNT];
    }
}
