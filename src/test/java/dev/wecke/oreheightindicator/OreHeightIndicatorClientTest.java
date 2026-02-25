package dev.wecke.oreheightindicator;

import dev.wecke.oreheightindicator.data.OreDataProvider;
import dev.wecke.oreheightindicator.data.StaticVanilla1211Provider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class OreHeightIndicatorClientTest {
    @Test
    void createProviderReturnsStaticWhenDynamicDisabled() {
        OreDataProvider provider = OreHeightIndicatorClient.createProvider(false, () -> {
            throw new IllegalStateException("must not be called");
        });

        assertInstanceOf(StaticVanilla1211Provider.class, provider);
    }

    @Test
    void createProviderFallsBackToStaticWhenDynamicInitializationFails() {
        OreDataProvider provider = OreHeightIndicatorClient.createProvider(true, () -> {
            throw new IllegalStateException("simulated init failure");
        });

        assertInstanceOf(StaticVanilla1211Provider.class, provider);
    }

    @Test
    void createProviderUsesDynamicWhenInitializationSucceeds() {
        OreDataProvider fakeDynamic = new FakeProvider();

        OreDataProvider provider = OreHeightIndicatorClient.createProvider(true, () -> fakeDynamic);

        assertSame(fakeDynamic, provider);
    }

    private static final class FakeProvider implements OreDataProvider {
        @Override
        public int minY() {
            return 0;
        }

        @Override
        public int maxY() {
            return 0;
        }

        @Override
        public int oreCount() {
            return 1;
        }

        @Override
        public String oreName(int index) {
            return "X";
        }

        @Override
        public void fillScores(int y, float[] outScores) {
            outScores[0] = 1.0f;
        }
    }
}
