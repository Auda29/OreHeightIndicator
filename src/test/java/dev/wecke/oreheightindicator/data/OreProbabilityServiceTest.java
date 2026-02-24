package dev.wecke.oreheightindicator.data;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OreProbabilityServiceTest {
    @Test
    void updateIfNeededReturnsTrueThenFalseForSameY() {
        FakeProvider provider = new FakeProvider(new float[] {2.0f, 1.0f, 1.0f}, 0, 10);
        OreProbabilityService service = new OreProbabilityService(provider);

        assertTrue(service.updateIfNeeded(5));
        assertFalse(service.updateIfNeeded(5));
        assertEquals(1, provider.fillScoresCalls);
    }

    @Test
    void updateIfNeededClampsYBeforeProviderCall() {
        FakeProvider provider = new FakeProvider(new float[] {1.0f, 1.0f, 1.0f}, 0, 10);
        OreProbabilityService service = new OreProbabilityService(provider);

        service.updateIfNeeded(-999);
        assertEquals(0, provider.lastYSeen);

        service.updateIfNeeded(999);
        assertEquals(10, provider.lastYSeen);
    }

    @Test
    void sortedChancesNormalizesAndSortsDescending() {
        FakeProvider provider = new FakeProvider(new float[] {2.0f, 1.0f, 1.0f}, 0, 10);
        OreProbabilityService service = new OreProbabilityService(provider);

        service.updateIfNeeded(3);
        List<OreProbabilityService.OreChance> chances = service.sortedChances();

        assertEquals(3, chances.size());
        assertEquals("A", chances.get(0).oreName());
        assertEquals(50.0f, chances.get(0).percent(), 1.0e-5f);
        assertEquals(25.0f, chances.get(1).percent(), 1.0e-5f);
        assertEquals(25.0f, chances.get(2).percent(), 1.0e-5f);

        float sum = 0.0f;
        for (OreProbabilityService.OreChance chance : chances) {
            sum += chance.percent();
        }
        assertEquals(100.0f, sum, 1.0e-3f);
        assertTrue(chances.get(0).percent() >= chances.get(1).percent());
        assertTrue(chances.get(1).percent() >= chances.get(2).percent());
    }

    @Test
    void sortedChancesAreZeroWhenProviderReturnsZeroScores() {
        FakeProvider provider = new FakeProvider(new float[] {0.0f, 0.0f, 0.0f}, 0, 10);
        OreProbabilityService service = new OreProbabilityService(provider);

        service.updateIfNeeded(5);
        for (OreProbabilityService.OreChance chance : service.sortedChances()) {
            assertEquals(0.0f, chance.percent(), 1.0e-6f);
        }
    }

    private static final class FakeProvider implements OreDataProvider {
        private final float[] scores;
        private final int minY;
        private final int maxY;
        private int lastYSeen = Integer.MIN_VALUE;
        private int fillScoresCalls = 0;

        private FakeProvider(float[] scores, int minY, int maxY) {
            this.scores = scores;
            this.minY = minY;
            this.maxY = maxY;
        }

        @Override
        public int minY() {
            return minY;
        }

        @Override
        public int maxY() {
            return maxY;
        }

        @Override
        public int oreCount() {
            return scores.length;
        }

        @Override
        public String oreName(int index) {
            return String.valueOf((char) ('A' + index));
        }

        @Override
        public void fillScores(int y, float[] outScores) {
            fillScoresCalls++;
            lastYSeen = y;
            System.arraycopy(scores, 0, outScores, 0, scores.length);
        }
    }
}
