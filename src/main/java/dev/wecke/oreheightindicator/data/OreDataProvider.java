package dev.wecke.oreheightindicator.data;

public interface OreDataProvider {
    int minY();

    int maxY();

    int oreCount();

    String oreName(int index);

    void fillScores(int y, float[] outScores);
}
