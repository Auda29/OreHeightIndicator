package dev.wecke.oreheightindicator.data;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;

public interface OreDataProvider {
    /**
     * Lets a world-aware provider refresh its immutable snapshot. Static providers
     * do not need to do anything here.
     */
    default void refresh(Minecraft client) {
    }

    /**
     * Changes whenever the available ores or their profiles change.
     */
    default long revision() {
        return 0L;
    }

    int minY();

    int maxY();

    int oreCount();

    String oreName(int index);

    default String oreKey(int index) {
        return oreName(index);
    }

    default String oreTranslationKey(int index) {
        return "";
    }

    default Item oreItem(int index) {
        return null;
    }

    void fillScores(int y, float[] outScores);
}
