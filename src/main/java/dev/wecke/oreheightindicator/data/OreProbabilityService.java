package dev.wecke.oreheightindicator.data;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class OreProbabilityService {
    private static final Comparator<OreChance> CHANCE_DESC =
        (left, right) -> Float.compare(right.relevance(), left.relevance());

    private final OreDataProvider provider;
    private float[] scoreBuffer;
    private final List<OreChance> chancesByProviderIndex;
    private final List<OreChance> sortedChances;
    private int lastY = Integer.MIN_VALUE;
    private long lastRevision = Long.MIN_VALUE;

    public OreProbabilityService(OreDataProvider provider) {
        this.provider = provider;
        this.scoreBuffer = new float[0];
        this.chancesByProviderIndex = new ArrayList<>();
        this.sortedChances = new ArrayList<>();
        rebuildOreList();
    }

    public boolean updateIfNeeded(MinecraftClient client, int y) {
        provider.refresh(client);
        return updateIfNeeded(y);
    }

    public boolean updateIfNeeded(int y) {
        long revision = provider.revision();
        if (y == lastY && revision == lastRevision) {
            return false;
        }

        if (revision != lastRevision || scoreBuffer.length != provider.oreCount()) {
            rebuildOreList();
            revision = provider.revision();
        }

        int clampedY = Math.max(provider.minY(), Math.min(provider.maxY(), y));
        provider.fillScores(clampedY, scoreBuffer);

        for (int i = 0; i < scoreBuffer.length; i++) {
            float relevance = Math.max(0.0f, Math.min(100.0f, scoreBuffer[i] * 100.0f));
            chancesByProviderIndex.get(i).setRelevance(relevance);
        }

        sortedChances.clear();
        sortedChances.addAll(chancesByProviderIndex);
        sortedChances.sort(CHANCE_DESC);
        lastY = y;
        lastRevision = revision;
        return true;
    }

    private void rebuildOreList() {
        int count = Math.max(0, provider.oreCount());
        scoreBuffer = new float[count];
        chancesByProviderIndex.clear();
        sortedChances.clear();
        for (int i = 0; i < count; i++) {
            OreChance chance = new OreChance(
                provider.oreKey(i),
                provider.oreName(i),
                provider.oreTranslationKey(i),
                provider.oreItem(i),
                0.0f
            );
            chancesByProviderIndex.add(chance);
            sortedChances.add(chance);
        }
    }

    public List<OreChance> sortedChances() {
        return sortedChances;
    }

    public static final class OreChance {
        private final String oreKey;
        private final String oreName;
        private final String translationKey;
        private final Item iconItem;
        private float relevance;

        private OreChance(
            String oreKey,
            String oreName,
            String translationKey,
            Item iconItem,
            float relevance
        ) {
            this.oreKey = oreKey;
            this.oreName = oreName;
            this.translationKey = translationKey;
            this.iconItem = iconItem;
            this.relevance = relevance;
        }

        public String oreKey() {
            return oreKey;
        }

        public String oreName() {
            return oreName;
        }

        public String translationKey() {
            return translationKey;
        }

        public Item iconItem() {
            return iconItem;
        }

        public float relevance() {
            return relevance;
        }

        /**
         * Kept for source compatibility with the first public version.
         */
        public float percent() {
            return relevance;
        }

        private void setRelevance(float relevance) {
            this.relevance = relevance;
        }
    }
}
