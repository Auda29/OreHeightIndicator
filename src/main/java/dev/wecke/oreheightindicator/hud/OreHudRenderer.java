package dev.wecke.oreheightindicator.hud;

import dev.wecke.oreheightindicator.config.ModConfig;
import dev.wecke.oreheightindicator.data.OreProbabilityService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class OreHudRenderer {
    private static final String TITLE_LINE = "Ore Height Indicator";
    private static final int BG_COLOR = 0x88000000;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int ICON_SIZE = 16;
    private static final int ICON_TEXT_GAP = 2;
    private static final int HEADER_LINE_COUNT = 2;
    private static final float REORDER_ANIMATION_SPEED = 12.0f;

    private final ModConfig config;
    private final OreProbabilityService probabilityService;
    private final List<AnimatedOreRow> animatedRows = new ArrayList<>();
    private final List<AnimatedOreRow> renderRows = new ArrayList<>();
    private int cachedY = Integer.MIN_VALUE;
    private long lastRenderNanos = 0L;

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
        if (!config.hudEnabled || cachedY == Integer.MIN_VALUE) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        int lineHeight = Math.max(textRenderer.fontHeight, ICON_SIZE) + 2;

        applyAnimationStep();

        int contentWidth = Math.max(
            textRenderer.getWidth(TITLE_LINE),
            textRenderer.getWidth("Y: " + cachedY)
        );
        boolean showIcons = Boolean.TRUE.equals(config.showOreIcons);
        for (AnimatedOreRow row : animatedRows) {
            int rowWidth = textRenderer.getWidth(row.label);
            if (showIcons) {
                rowWidth += ICON_SIZE + ICON_TEXT_GAP;
            }
            contentWidth = Math.max(contentWidth, rowWidth);
        }

        int height = (lineHeight * (HEADER_LINE_COUNT + animatedRows.size())) + 4;
        int width = contentWidth + 8;

        float scale = config.uiScale != null ? config.uiScale : 1.0f;
        scale = Math.max(0.5f, Math.min(3.0f, scale));
        int visualWidth = Math.round(width * scale);
        int y = config.hudY;
        // Interpret HUD X as right-edge margin so default placement is top-right.
        int x = Math.max(0, client.getWindow().getScaledWidth() - visualWidth - config.hudX);

        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.scale(scale, scale);

        int scaledX = Math.round(x / scale);
        int scaledY = Math.round(y / scale);
        context.fill(scaledX, scaledY, scaledX + width, scaledY + height, BG_COLOR);

        drawTextLine(context, textRenderer, scaledX, scaledY + 2, lineHeight, TITLE_LINE);
        drawTextLine(context, textRenderer, scaledX, scaledY + 2 + lineHeight, lineHeight, "Y: " + cachedY);

        renderRows.clear();
        renderRows.addAll(animatedRows);
        renderRows.sort(Comparator.comparingDouble(row -> row.currentIndex));
        for (AnimatedOreRow row : renderRows) {
            float rowTopFloat = scaledY + 2 + ((HEADER_LINE_COUNT + row.currentIndex) * lineHeight);
            int rowTop = Math.round(rowTopFloat);
            int textX = scaledX + 4;
            int textY = rowTop + ((lineHeight - textRenderer.fontHeight) / 2);

            if (showIcons) {
                int iconY = rowTop + ((lineHeight - ICON_SIZE) / 2);
                context.drawItem(row.icon, textX, iconY);
                textX += ICON_SIZE + ICON_TEXT_GAP;
            }
            context.drawText(textRenderer, Text.literal(row.label), textX, textY, TEXT_COLOR, false);
        }

        matrices.popMatrix();
    }

    private void rebuildLines() {
        List<AnimatedOreRow> nextRows = new ArrayList<>();
        int count = 0;
        for (OreProbabilityService.OreChance chance : probabilityService.sortedChances()) {
            if (count >= config.maxEntries) {
                break;
            }
            if (chance.percent() <= 0.0f) {
                continue;
            }

            String oreName = chance.oreName();
            String label = String.format(Locale.ROOT, "%s: %.1f%%", oreName, chance.percent());
            AnimatedOreRow existing = findAnimatedRow(oreName);

            if (existing == null) {
                existing = new AnimatedOreRow(oreName, label, iconForOre(oreName), count);
            } else {
                existing.label = label;
                existing.icon = iconForOre(oreName);
            }
            existing.targetIndex = count;
            nextRows.add(existing);
            count++;
        }

        animatedRows.clear();
        animatedRows.addAll(nextRows);
    }

    private static ItemStack iconForOre(String oreName) {
        return switch (oreName) {
            case "Coal" -> new ItemStack(Items.COAL_ORE);
            case "Copper" -> new ItemStack(Items.COPPER_ORE);
            case "Iron" -> new ItemStack(Items.IRON_ORE);
            case "Gold" -> new ItemStack(Items.GOLD_ORE);
            case "Redstone" -> new ItemStack(Items.REDSTONE_ORE);
            case "Lapis" -> new ItemStack(Items.LAPIS_ORE);
            case "Diamond" -> new ItemStack(Items.DIAMOND_ORE);
            case "Emerald" -> new ItemStack(Items.EMERALD_ORE);
            default -> ItemStack.EMPTY;
        };
    }

    private AnimatedOreRow findAnimatedRow(String oreName) {
        for (AnimatedOreRow row : animatedRows) {
            if (row.oreName.equals(oreName)) {
                return row;
            }
        }
        return null;
    }

    private void applyAnimationStep() {
        if (!Boolean.TRUE.equals(config.animateReorder)) {
            for (AnimatedOreRow row : animatedRows) {
                row.currentIndex = row.targetIndex;
            }
            lastRenderNanos = 0L;
            return;
        }

        long now = System.nanoTime();
        if (lastRenderNanos == 0L) {
            lastRenderNanos = now;
            return;
        }

        float deltaSeconds = (now - lastRenderNanos) / 1_000_000_000.0f;
        lastRenderNanos = now;
        float alpha = Math.min(1.0f, Math.max(0.0f, deltaSeconds * REORDER_ANIMATION_SPEED));

        for (AnimatedOreRow row : animatedRows) {
            row.currentIndex = row.currentIndex + ((row.targetIndex - row.currentIndex) * alpha);
        }
    }

    private static void drawTextLine(DrawContext context, TextRenderer textRenderer, int x, int rowTop, int lineHeight, String line) {
        int textY = rowTop + ((lineHeight - textRenderer.fontHeight) / 2);
        context.drawText(textRenderer, Text.literal(line), x + 4, textY, TEXT_COLOR, false);
    }

    private static final class AnimatedOreRow {
        private final String oreName;
        private String label;
        private ItemStack icon;
        private float currentIndex;
        private float targetIndex;

        private AnimatedOreRow(String oreName, String label, ItemStack icon, int startIndex) {
            this.oreName = oreName;
            this.label = label;
            this.icon = icon;
            this.currentIndex = startIndex;
            this.targetIndex = startIndex;
        }
    }
}
