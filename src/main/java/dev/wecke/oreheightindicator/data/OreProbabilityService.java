package dev.wecke.oreheightindicator.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class OreProbabilityService {
    private static final Comparator<OreChance> CHANCE_DESC = (left, right) -> Float.compare(right.percent(), left.percent());

    private final OreDataProvider provider;
    private final float[] scoreBuffer;
    private final List<OreChance> sortedChances;
    private int lastY = Integer.MIN_VALUE;

    public OreProbabilityService(OreDataProvider provider) {
        this.provider = provider;
        this.scoreBuffer = new float[provider.oreCount()];
        this.sortedChances = new ArrayList<>(provider.oreCount());
        for (int i = 0; i < provider.oreCount(); i++) {
            sortedChances.add(new OreChance(provider.oreName(i), 0.0f));
        }
    }

    public boolean updateIfNeeded(int y) {
        if (y == lastY) {
            return false;
        }

        int clampedY = Math.max(provider.minY(), Math.min(provider.maxY(), y));
        provider.fillScores(clampedY, scoreBuffer);

        float total = 0.0f;
        for (float score : scoreBuffer) {
            if (score > 0.0f) {
                total += score;
            }
        }

        for (int i = 0; i < scoreBuffer.length; i++) {
            float normalized = total > 0.0f ? (scoreBuffer[i] / total) * 100.0f : 0.0f;
            sortedChances.get(i).setPercent(normalized);
        }

        sortedChances.sort(CHANCE_DESC);
        lastY = y;
        return true;
    }

    public List<OreChance> sortedChances() {
        return sortedChances;
    }

    public static final class OreChance {
        private final String oreName;
        private float percent;

        private OreChance(String oreName, float percent) {
            this.oreName = oreName;
            this.percent = percent;
        }

        public String oreName() {
            return oreName;
        }

        public float percent() {
            return percent;
        }

        private void setPercent(float percent) {
            this.percent = percent;
        }
    }
}
