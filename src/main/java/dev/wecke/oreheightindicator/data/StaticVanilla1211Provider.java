package dev.wecke.oreheightindicator.data;

public final class StaticVanilla1211Provider implements OreDataProvider {
    private static final int MIN_Y = -64;
    private static final int MAX_Y = 320;

    private static final OreCurve[] CURVES = new OreCurve[] {
        // Wiki-based approximations from https://minecraft.wiki/w/Ore and /Ore_(feature)
        // for Java 1.21.x static baseline (overworld, biome-agnostic).
        new OreCurve("Coal", 1.0f,
            Source.triangle(0, 192, 96, 0.65f),
            Source.uniform(136, 320, 0.35f)
        ),
        new OreCurve("Copper", 0.9f,
            Source.triangle(-16, 112, 48, 1.0f)
        ),
        new OreCurve("Iron", 0.95f,
            Source.uniform(-64, 72, 0.45f),
            Source.triangle(-24, 56, 16, 0.35f),
            Source.triangle(80, 320, 200, 0.20f)
        ),
        new OreCurve("Gold", 0.8f,
            Source.triangle(-64, 32, -16, 0.75f),
            Source.uniform(-64, -48, 0.25f)
        ),
        new OreCurve("Redstone", 1.0f,
            Source.uniform(-64, 15, 0.55f),
            Source.triangle(-64, -32, -48, 0.45f)
        ),
        new OreCurve("Lapis", 0.8f,
            Source.triangle(-32, 32, 0, 0.65f),
            Source.uniform(-64, 64, 0.35f)
        ),
        new OreCurve("Diamond", 1.0f,
            Source.triangle(-64, 16, -56, 0.80f),
            Source.uniform(-64, -4, 0.20f)
        ),
        new OreCurve("Emerald", 0.85f,
            Source.triangle(-16, 320, 85, 1.0f)
        )
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

    private static final class OreCurve {
        private final String name;
        private final float peakScore;
        private final Source[] sources;

        private OreCurve(String name, float peakScore, Source... sources) {
            this.name = name;
            this.peakScore = peakScore;
            this.sources = sources;
        }

        private float scoreAt(int y) {
            float sum = 0.0f;
            for (Source source : sources) {
                sum += source.scoreAt(y);
            }
            if (sum <= 0.0f) {
                return 0.0f;
            }
            return Math.min(peakScore, sum);
        }
    }

    private enum Shape {
        TRIANGLE,
        UNIFORM
    }

    private static final class Source {
        private final Shape shape;
        private final int min;
        private final int max;
        private final int peak;
        private final float weight;

        private Source(Shape shape, int min, int max, int peak, float weight) {
            this.shape = shape;
            this.min = min;
            this.max = max;
            this.peak = peak;
            this.weight = weight;
        }

        private static Source triangle(int min, int max, int peak, float weight) {
            return new Source(Shape.TRIANGLE, min, max, peak, weight);
        }

        private static Source uniform(int min, int max, float weight) {
            return new Source(Shape.UNIFORM, min, max, 0, weight);
        }

        private float scoreAt(int y) {
            if (y < min || y > max) {
                return 0.0f;
            }
            if (shape == Shape.UNIFORM) {
                return weight;
            }
            if (peak <= min || peak >= max) {
                return weight;
            }

            float value;
            if (y == peak) {
                value = weight;
            } else if (y < peak) {
                value = weight * ((float) (y - min) / (float) (peak - min));
            } else {
                value = weight * ((float) (max - y) / (float) (max - peak));
            }
            return Math.max(0.0f, value);
        }
    }
}
