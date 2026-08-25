package dev.wecke.oreheightindicator.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrackedBlockSamplerTest {
    @Test
    void normalizesAndSmoothsObservedBlockCounts() {
        float[] profile = TrackedBlockSampler.normalizedProfile(new int[] {0, 0, 4, 0, 0, 0, 0});

        assertEquals(1.0f, profile[2]);
        assertTrue(profile[0] > 0.0f);
        assertTrue(profile[4] > 0.0f);
        assertEquals(0.0f, profile[5]);
    }

    @Test
    void emptySamplesStayAtZero() {
        float[] profile = TrackedBlockSampler.normalizedProfile(new int[] {0, 0, 0});

        assertEquals(0.0f, profile[0]);
        assertEquals(0.0f, profile[1]);
        assertEquals(0.0f, profile[2]);
    }
}
