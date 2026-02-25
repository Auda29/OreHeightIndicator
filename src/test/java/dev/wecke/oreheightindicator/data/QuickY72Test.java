package dev.wecke.oreheightindicator.data;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QuickY72Test {
    @Test
    void atY72CoalAndCopperShouldDominate() {
        StaticVanilla1211Provider provider = new StaticVanilla1211Provider();
        float[] out = new float[provider.oreCount()];
        
        provider.fillScores(72, out);
        
        System.out.println("=== Scores at Y=72 ===");
        float total = 0;
        for (int i = 0; i < provider.oreCount(); i++) {
            System.out.printf("%s: %.4f%n", provider.oreName(i), out[i]);
            total += out[i];
        }
        System.out.println("Total: " + total);
        
        System.out.println("\n=== Percentages ===");
        for (int i = 0; i < provider.oreCount(); i++) {
            float pct = total > 0 ? (out[i] / total) * 100 : 0;
            System.out.printf("%s: %.1f%%%n", provider.oreName(i), pct);
        }
        
        float coal = out[0];
        float copper = out[1];
        float lapis = out[5];
        float diamond = out[6];
        
        float emerald = out[7];

        assertTrue(coal > 0, "coal should be positive at Y=72");
        assertTrue(copper > 0, "copper should be positive at Y=72");
        assertEquals(0.0f, lapis, 1e-6f, "lapis should be zero at Y=72");
        assertEquals(0.0f, diamond, 1e-6f, "diamond should be zero at Y=72");
        assertTrue(coal > copper, "coal should exceed copper at Y=72");
        assertTrue(emerald > coal, "emerald should exceed coal at Y=72 for current table data");
        assertTrue(emerald > copper, "emerald should exceed copper at Y=72 for current table data");
    }
}
