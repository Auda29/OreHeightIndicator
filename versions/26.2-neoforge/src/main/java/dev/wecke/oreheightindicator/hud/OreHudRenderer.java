package dev.wecke.oreheightindicator.hud;

import dev.wecke.oreheightindicator.config.ModConfig;
import dev.wecke.oreheightindicator.data.OreDisplayCatalog;
import dev.wecke.oreheightindicator.data.OreProbabilityService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class OreHudRenderer {
    private static final int BG_COLOR = 0x88000000;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int BAR_BG_COLOR = 0x66333333;
    private static final int BAR_COLOR = 0xFF55CC66;
    private static final int ICON_SIZE = 16;
    private static final int ICON_TEXT_GAP = 2;
    private static final int BAR_WIDTH = 24;
    private static final int BAR_HEIGHT = 4;
    private static final int PERCENT_BAR_WIDTH = 30;
    private static final int PERCENT_BAR_HEIGHT = 9;
    private static final int BAR_GAP = 4;
    private static final int HEADER_LINE_COUNT = 1;
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

    public void update(Minecraft client, int y) {
        probabilityService.updateIfNeeded(client, y);
        cachedY = y;
        rebuildLines();
    }

    public void render(GuiGraphicsExtractor context) {
        if (!config.hudEnabled || cachedY == Integer.MIN_VALUE) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        Font textRenderer = client.font;
        int lineHeight = Math.max(textRenderer.lineHeight, ICON_SIZE) + 2;

        applyAnimationStep();

        int contentWidth = textRenderer.width("Y " + cachedY);
        boolean showIcons = Boolean.TRUE.equals(config.showOreIcons);
        boolean showPercent = Boolean.TRUE.equals(config.showSuitabilityPercent);
        int barWidth = showPercent ? PERCENT_BAR_WIDTH : BAR_WIDTH;
        int barHeight = showPercent ? PERCENT_BAR_HEIGHT : BAR_HEIGHT;
        for (AnimatedOreRow row : animatedRows) {
            int rowWidth = textRenderer.width(row.label) + BAR_GAP + barWidth;
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
        int x = Math.max(0, client.getWindow().getGuiScaledWidth() - visualWidth - config.hudX);

        var matrices = context.pose();
        matrices.pushMatrix();
        matrices.scale(scale, scale);

        int scaledX = Math.round(x / scale);
        int scaledY = Math.round(y / scale);
        context.fill(scaledX, scaledY, scaledX + width, scaledY + height, BG_COLOR);

        drawTextLine(context, textRenderer, scaledX, scaledY + 2, lineHeight, "Y " + cachedY);

        renderRows.clear();
        renderRows.addAll(animatedRows);
        renderRows.sort(Comparator.comparingDouble(row -> row.currentIndex));
        for (AnimatedOreRow row : renderRows) {
            float rowTopFloat = scaledY + 2 + ((HEADER_LINE_COUNT + row.currentIndex) * lineHeight);
            int rowTop = Math.round(rowTopFloat);
            int textX = scaledX + 4;
            int textY = rowTop + ((lineHeight - textRenderer.lineHeight) / 2);

            if (showIcons) {
                int iconY = rowTop + ((lineHeight - ICON_SIZE) / 2);
                context.item(row.icon, textX, iconY);
                textX += ICON_SIZE + ICON_TEXT_GAP;
            }
            context.text(textRenderer, Component.literal(row.label), textX, textY, TEXT_COLOR, false);

            int barX = scaledX + width - 4 - barWidth;
            int barY = rowTop + ((lineHeight - barHeight) / 2);
            int filledWidth = Math.round(barWidth * Math.max(0.0f, Math.min(1.0f, row.relevance / 100.0f)));
            context.fill(barX, barY, barX + barWidth, barY + barHeight, BAR_BG_COLOR);
            if (filledWidth > 0) {
                context.fill(barX, barY, barX + filledWidth, barY + barHeight, BAR_COLOR);
            }
            if (showPercent) {
                String percent = Math.round(row.relevance) + "%";
                int percentX = barX + ((barWidth - textRenderer.width(percent)) / 2);
                int percentY = rowTop + ((lineHeight - textRenderer.lineHeight) / 2);
                context.text(textRenderer, Component.literal(percent), percentX, percentY, TEXT_COLOR, true);
            }
        }

        matrices.popMatrix();
    }

    private void rebuildLines() {
        List<AnimatedOreRow> nextRows = new ArrayList<>();
        int count = 0;
        for (OreProbabilityService.OreChance chance : selectDisplayedChances()) {

            String oreName = chance.oreName();
            String label = chance.translationKey().isEmpty()
                ? oreName
                : Component.translatable(chance.translationKey()).getString();
            AnimatedOreRow existing = findAnimatedRow(chance.oreKey());
            ItemStack icon = chance.iconItem() == null || chance.iconItem() == Items.AIR
                ? iconForOre(oreName)
                : new ItemStack(chance.iconItem());

            if (existing == null) {
                existing = new AnimatedOreRow(chance.oreKey(), label, icon, chance.relevance(), count);
            } else {
                existing.label = label;
                existing.icon = icon;
                existing.relevance = chance.relevance();
            }
            existing.targetIndex = count;
            nextRows.add(existing);
            count++;
        }

        animatedRows.clear();
        animatedRows.addAll(nextRows);
    }

    private List<OreProbabilityService.OreChance> selectDisplayedChances() {
        int limit = Math.max(1, config.maxEntries);
        List<OreProbabilityService.OreChance> selected = new ArrayList<>(limit);
        for (OreProbabilityService.OreChance chance : probabilityService.sortedChances()) {
            if (selected.size() >= limit) break;
            if (!OreDisplayCatalog.isStandardOre(chance.oreKey()) && config.isMaterialTracked(chance.oreKey())) selected.add(chance);
        }
        float threshold = config.minimumPercent != null ? config.minimumPercent : 10.0f;
        for (OreProbabilityService.OreChance chance : probabilityService.sortedChances()) {
            if (selected.size() >= limit) break;
            if (OreDisplayCatalog.isStandardOre(chance.oreKey())
                && config.isOreVisible(chance.oreKey())
                && chance.relevance() >= threshold) selected.add(chance);
        }
        selected.sort((left, right) -> Float.compare(right.relevance(), left.relevance()));
        return selected;
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

    private AnimatedOreRow findAnimatedRow(String oreKey) {
        for (AnimatedOreRow row : animatedRows) {
            if (row.oreKey.equals(oreKey)) {
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

    private static void drawTextLine(GuiGraphicsExtractor context, Font textRenderer, int x, int rowTop, int lineHeight, String line) {
        int textY = rowTop + ((lineHeight - textRenderer.lineHeight) / 2);
        context.text(textRenderer, Component.literal(line), x + 4, textY, TEXT_COLOR, false);
    }

    private static final class AnimatedOreRow {
        private final String oreKey;
        private String label;
        private ItemStack icon;
        private float relevance;
        private float currentIndex;
        private float targetIndex;

        private AnimatedOreRow(String oreKey, String label, ItemStack icon, float relevance, int startIndex) {
            this.oreKey = oreKey;
            this.label = label;
            this.icon = icon;
            this.relevance = relevance;
            this.currentIndex = startIndex;
            this.targetIndex = startIndex;
        }
    }
}
