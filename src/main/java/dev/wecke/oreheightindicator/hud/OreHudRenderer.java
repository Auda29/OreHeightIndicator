package dev.wecke.oreheightindicator.hud;

import dev.wecke.oreheightindicator.config.ModConfig;
import dev.wecke.oreheightindicator.data.OreProbabilityService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class OreHudRenderer {
    private static final int BG_COLOR = 0x88000000;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    private final ModConfig config;
    private final OreProbabilityService probabilityService;
    private final List<String> cachedLines = new ArrayList<>();
    private int cachedY = Integer.MIN_VALUE;

    public OreHudRenderer(ModConfig config, OreProbabilityService probabilityService) {
        this.config = config;
        this.probabilityService = probabilityService;
    }

    public void updateForY(int y) {
        if (y == cachedY) {
            return;
        }
        if (!probabilityService.updateIfNeeded(y)) {
            return;
        }
        cachedY = y;
        rebuildLines();
    }

    public void render(DrawContext context) {
        if (!config.hudEnabled || cachedLines.isEmpty()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;

        int contentWidth = 0;
        for (String line : cachedLines) {
            contentWidth = Math.max(contentWidth, textRenderer.getWidth(line));
        }

        int x = config.hudX;
        int y = config.hudY;
        int lineHeight = textRenderer.fontHeight + 2;
        int height = (lineHeight * cachedLines.size()) + 4;
        int width = contentWidth + 8;

        context.fill(x, y, x + width, y + height, BG_COLOR);

        int textY = y + 2;
        for (String line : cachedLines) {
            context.drawText(textRenderer, Text.literal(line), x + 4, textY, TEXT_COLOR, false);
            textY += lineHeight;
        }
    }

    private void rebuildLines() {
        cachedLines.clear();
        cachedLines.add("Ore Height Indicator");
        cachedLines.add("Y: " + cachedY);

        int count = 0;
        for (OreProbabilityService.OreChance chance : probabilityService.sortedChances()) {
            if (count >= config.maxEntries) {
                break;
            }
            if (chance.percent() <= 0.0f) {
                continue;
            }
            cachedLines.add(String.format(Locale.ROOT, "%s: %.1f%%", chance.oreName(), chance.percent()));
            count++;
        }
    }
}
