package dev.wecke.oreheightindicator.hud;

import dev.wecke.oreheightindicator.config.ModConfig;
import dev.wecke.oreheightindicator.data.OreProbabilityService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class OreHudRenderer {
    private static final int BG_COLOR = 0x88000000;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int ICON_SIZE = 16;
    private static final int ICON_TEXT_GAP = 2;

    private final ModConfig config;
    private final OreProbabilityService probabilityService;
    private final List<String> cachedLines = new ArrayList<>();
    private final List<ItemStack> cachedIcons = new ArrayList<>();
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
        for (int i = 0; i < cachedLines.size(); i++) {
            String line = cachedLines.get(i);
            int lineWidth = textRenderer.getWidth(line);
            ItemStack icon = cachedIcons.get(i);
            if (!icon.isEmpty()) {
                lineWidth += ICON_SIZE + ICON_TEXT_GAP;
            }
            contentWidth = Math.max(contentWidth, lineWidth);
        }

        int x = config.hudX;
        int y = config.hudY;
        int lineHeight = Math.max(textRenderer.fontHeight, ICON_SIZE) + 2;
        int height = (lineHeight * cachedLines.size()) + 4;
        int width = contentWidth + 8;

        context.fill(x, y, x + width, y + height, BG_COLOR);

        for (int i = 0; i < cachedLines.size(); i++) {
            int rowTop = y + 2 + (i * lineHeight);
            int textY = rowTop + ((lineHeight - textRenderer.fontHeight) / 2);
            int textX = x + 4;

            ItemStack icon = cachedIcons.get(i);
            if (!icon.isEmpty()) {
                int iconY = rowTop + ((lineHeight - ICON_SIZE) / 2);
                context.drawItem(icon, textX, iconY);
                textX += ICON_SIZE + ICON_TEXT_GAP;
            }

            context.drawText(textRenderer, Text.literal(cachedLines.get(i)), textX, textY, TEXT_COLOR, false);
        }
    }

    private void rebuildLines() {
        cachedLines.clear();
        cachedIcons.clear();
        cachedLines.add("Ore Height Indicator");
        cachedIcons.add(ItemStack.EMPTY);
        cachedLines.add("Y: " + cachedY);
        cachedIcons.add(ItemStack.EMPTY);

        int count = 0;
        for (OreProbabilityService.OreChance chance : probabilityService.sortedChances()) {
            if (count >= config.maxEntries) {
                break;
            }
            if (chance.percent() <= 0.0f) {
                continue;
            }
            cachedLines.add(String.format(Locale.ROOT, "%s: %.1f%%", chance.oreName(), chance.percent()));
            cachedIcons.add(iconForOre(chance.oreName()));
            count++;
        }
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
}
