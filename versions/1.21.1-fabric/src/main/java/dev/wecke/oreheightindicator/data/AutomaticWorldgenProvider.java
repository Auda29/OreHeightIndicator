package dev.wecke.oreheightindicator.data;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;

/**
 * Selects the most accurate source available without exposing a provider switch
 * to the user.
 */
public final class AutomaticWorldgenProvider implements OreDataProvider {
    private final RuntimeWorldgenProvider runtime = new RuntimeWorldgenProvider();
    private final ClasspathWorldgenProvider classpath = new ClasspathWorldgenProvider();
    private volatile OreDataProvider active = classpath;

    @Override
    public void refresh(MinecraftClient client) {
        classpath.refresh(client);
        runtime.refresh(client);
        active = client != null && client.getServer() != null && runtime.oreCount() > 0
            ? runtime
            : classpath;
    }

    private OreDataProvider active() {
        return active;
    }

    @Override
    public long revision() {
        OreDataProvider current = active();
        long sourceBit = current == runtime ? Long.MIN_VALUE : 0L;
        return sourceBit ^ current.revision();
    }

    @Override
    public int minY() {
        return active().minY();
    }

    @Override
    public int maxY() {
        return active().maxY();
    }

    @Override
    public int oreCount() {
        return active().oreCount();
    }

    @Override
    public String oreName(int index) {
        return active().oreName(index);
    }

    @Override
    public String oreKey(int index) {
        return active().oreKey(index);
    }

    @Override
    public String oreTranslationKey(int index) {
        return active().oreTranslationKey(index);
    }

    @Override
    public Item oreItem(int index) {
        return active().oreItem(index);
    }

    @Override
    public void fillScores(int y, float[] outScores) {
        active().fillScores(y, outScores);
    }
}
