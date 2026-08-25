package dev.wecke.oreheightindicator.data;

import dev.wecke.oreheightindicator.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class OreProbabilityService {
    private static final Comparator<OreChance> CHANCE_DESC =
        (left, right) -> Float.compare(right.relevance(), left.relevance());

    private final OreDataProvider provider;
    private final ModConfig config;
    private final TrackedBlockSampler blockSampler;
    private float[] scoreBuffer;
    private final List<OreChance> chancesByProviderIndex;
    private final List<OreChance> sampledChances;
    private final List<OreChance> sortedChances;
    private int lastY = Integer.MIN_VALUE;
    private long lastProviderRevision = Long.MIN_VALUE;
    private long lastSamplerRevision = Long.MIN_VALUE;

    public OreProbabilityService(OreDataProvider provider) {
        this(provider, null);
    }

    public OreProbabilityService(OreDataProvider provider, ModConfig config) {
        this.provider = provider;
        this.config = config;
        this.blockSampler = config == null ? null : new TrackedBlockSampler();
        this.scoreBuffer = new float[0];
        this.chancesByProviderIndex = new ArrayList<>();
        this.sampledChances = new ArrayList<>();
        this.sortedChances = new ArrayList<>();
        rebuildOreList();
    }

    public boolean updateIfNeeded(MinecraftClient client, int y) {
        provider.refresh(client);
        refreshTrackedBlocks(client);
        return updateIfNeeded(y);
    }

    public boolean updateIfNeeded(int y) {
        long providerRevision = provider.revision();
        long samplerRevision = blockSampler == null ? 0L : blockSampler.revision();
        if (y == lastY
            && providerRevision == lastProviderRevision
            && samplerRevision == lastSamplerRevision) {
            return false;
        }

        if (providerRevision != lastProviderRevision
            || samplerRevision != lastSamplerRevision
            || scoreBuffer.length != provider.oreCount()) {
            rebuildOreList();
            providerRevision = provider.revision();
            samplerRevision = blockSampler == null ? 0L : blockSampler.revision();
        }

        int clampedY = Math.max(provider.minY(), Math.min(provider.maxY(), y));
        provider.fillScores(clampedY, scoreBuffer);

        for (int i = 0; i < scoreBuffer.length; i++) {
            float relevance = Math.max(0.0f, Math.min(100.0f, scoreBuffer[i] * 100.0f));
            chancesByProviderIndex.get(i).setRelevance(relevance);
        }

        if (blockSampler != null) {
            for (OreChance chance : chancesByProviderIndex) {
                if (config != null && config.isMaterialTracked(chance.oreKey())) {
                    float sampledRelevance = blockSampler.scoreAt(chance.oreKey(), y) * 100.0f;
                    chance.setRelevance(combineTrackedRelevance(chance.relevance(), sampledRelevance));
                }
            }
            for (OreChance chance : sampledChances) {
                float relevance = blockSampler.scoreAt(chance.oreKey(), y) * 100.0f;
                chance.setRelevance(Math.max(0.0f, Math.min(100.0f, relevance)));
            }
        }

        sortedChances.clear();
        sortedChances.addAll(chancesByProviderIndex);
        sortedChances.addAll(sampledChances);
        sortedChances.sort(CHANCE_DESC);
        lastY = y;
        lastProviderRevision = providerRevision;
        lastSamplerRevision = samplerRevision;
        return true;
    }

    private void refreshTrackedBlocks(MinecraftClient client) {
        if (blockSampler == null || config == null) {
            return;
        }
        blockSampler.refresh(client, config.trackedMaterialKeys());
    }

    private void rebuildOreList() {
        int count = Math.max(0, provider.oreCount());
        scoreBuffer = new float[count];
        chancesByProviderIndex.clear();
        sampledChances.clear();
        sortedChances.clear();
        Set<String> providerKeys = new HashSet<>();
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
            providerKeys.add(chance.oreKey());
            OreDisplayCatalog.remember(
                chance.oreKey(),
                chance.oreName(),
                chance.translationKey()
            );
        }
        if (blockSampler != null) {
            for (TrackedBlockSampler.Profile profile : blockSampler.profiles()) {
                if (providerKeys.contains(profile.key())) {
                    continue;
                }
                OreChance chance = new OreChance(
                    profile.key(),
                    profile.name(),
                    profile.translationKey(),
                    profile.item(),
                    0.0f
                );
                sampledChances.add(chance);
                sortedChances.add(chance);
                OreDisplayCatalog.remember(
                    chance.oreKey(),
                    chance.oreName(),
                    chance.translationKey()
                );
            }
        }
    }

    public List<OreChance> sortedChances() {
        return sortedChances;
    }

    static float combineTrackedRelevance(float worldgenRelevance, float sampledRelevance) {
        return Math.max(
            Math.max(0.0f, Math.min(100.0f, worldgenRelevance)),
            Math.max(0.0f, Math.min(100.0f, sampledRelevance))
        );
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
