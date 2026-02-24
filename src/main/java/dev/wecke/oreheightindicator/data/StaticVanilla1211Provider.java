package dev.wecke.oreheightindicator.data;

public final class StaticVanilla1211Provider implements OreDataProvider {
    private static final int MIN_Y = -64;
    private static final int MAX_Y = 320;

    private static final Curve[] CURVES = new Curve[] {
        new Curve("Coal", 0, 320, 180, 1.0f),
        new Curve("Copper", -16, 112, 48, 0.9f),
        new Curve("Iron", -24, 256, 16, 0.95f),
        new Curve("Gold", -64, 48, -16, 0.8f),
        new Curve("Redstone", -64, 16, -48, 1.0f),
        new Curve("Lapis", -64, 64, 0, 0.8f),
        new Curve("Diamond", -64, 16, -56, 1.0f),
        new Curve("Emerald", -16, 320, 224, 0.85f)
    };

    @Override
    public int minY() {
        return MIN_Y;
    }

    @Override
    public int maxY() {
        return MAX_Y;
    }

    @Override
    public int oreCount() {
        return CURVES.length;
    }

    @Override
    public String oreName(int index) {
        return CURVES[index].name;
    }

    @Override
    public void fillScores(int y, float[] outScores) {
        int clampedY = Math.max(MIN_Y, Math.min(MAX_Y, y));
        for (int i = 0; i < CURVES.length; i++) {
            outScores[i] = CURVES[i].scoreAt(clampedY);
        }
    }

    private static final class Curve {
        private final String name;
        private final int min;
        private final int max;
        private final int peak;
        private final float peakScore;

        private Curve(String name, int min, int max, int peak, float peakScore) {
            this.name = name;
            this.min = min;
            this.max = max;
            this.peak = peak;
            this.peakScore = peakScore;
        }

        private float scoreAt(int y) {
            if (y < min || y > max) {
                return 0.0f;
            }
            if (peak <= min || peak >= max) {
                return peakScore;
            }

            if (y == peak) {
                return peakScore;
            }

            float value;
            if (y < peak) {
                value = peakScore * ((float) (y - min) / (float) (peak - min));
            } else {
                value = peakScore * ((float) (max - y) / (float) (max - peak));
            }
            return Math.max(0.0f, value);
        }
    }
}
