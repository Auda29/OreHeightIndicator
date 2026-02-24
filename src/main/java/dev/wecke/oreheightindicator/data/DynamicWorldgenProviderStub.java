package dev.wecke.oreheightindicator.data;

public final class DynamicWorldgenProviderStub implements OreDataProvider {
    private final StaticVanilla1211Provider names = new StaticVanilla1211Provider();

    @Override
    public int minY() {
        return names.minY();
    }

    @Override
    public int maxY() {
        return names.maxY();
    }

    @Override
    public int oreCount() {
        return names.oreCount();
    }

    @Override
    public String oreName(int index) {
        return names.oreName(index);
    }

    @Override
    public void fillScores(int y, float[] outScores) {
        for (int i = 0; i < outScores.length; i++) {
            outScores[i] = 0.0f;
        }
    }
}
