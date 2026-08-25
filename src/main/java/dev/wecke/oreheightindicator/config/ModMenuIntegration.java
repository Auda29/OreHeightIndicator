package dev.wecke.oreheightindicator.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.wecke.oreheightindicator.data.OreDisplayCatalog;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.DropdownBoxEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ModMenuIntegration implements ModMenuApi {
    private static final int ADD_BUTTON_WIDTH = 88;

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> buildScreen(parent, false);
    }

    private static Screen buildScreen(Screen parent, boolean focusDisplayedOres) {
            ModConfig config = ModConfig.getCurrent();
            OreDisplayCatalog.rememberRegisteredBlocks();

            ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("config.oreheightindicator.title"));

            ConfigEntryBuilder entries = builder.entryBuilder();
            ConfigCategory hud = builder.getOrCreateCategory(Text.translatable("config.oreheightindicator.category.hud"));
            ConfigCategory displayedOres = builder.getOrCreateCategory(Text.translatable("config.oreheightindicator.category.displayed_ores"));
            ConfigCategory data = builder.getOrCreateCategory(Text.translatable("config.oreheightindicator.category.data"));
            if (focusDisplayedOres) {
                builder.setFallbackCategory(displayedOres);
            }

            hud.addEntry(
                entries.startBooleanToggle(Text.translatable("config.oreheightindicator.hud_enabled"), config.hudEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Text.translatable("config.oreheightindicator.hud_enabled.tooltip"))
                    .setSaveConsumer(value -> config.hudEnabled = value)
                    .build()
            );

            hud.addEntry(
                entries.startIntField(Text.translatable("config.oreheightindicator.hud_x"), config.hudX)
                    .setDefaultValue(8)
                    .setMin(0)
                    .setTooltip(Text.translatable("config.oreheightindicator.hud_x.tooltip"))
                    .setSaveConsumer(value -> config.hudX = value)
                    .build()
            );

            hud.addEntry(
                entries.startIntField(Text.translatable("config.oreheightindicator.hud_y"), config.hudY)
                    .setDefaultValue(8)
                    .setMin(0)
                    .setTooltip(Text.translatable("config.oreheightindicator.hud_y.tooltip"))
                    .setSaveConsumer(value -> config.hudY = value)
                    .build()
            );

            hud.addEntry(
                entries.startBooleanToggle(Text.translatable("config.oreheightindicator.show_icons"), Boolean.TRUE.equals(config.showOreIcons))
                    .setDefaultValue(true)
                    .setTooltip(Text.translatable("config.oreheightindicator.show_icons.tooltip"))
                    .setSaveConsumer(value -> config.showOreIcons = value)
                    .build()
            );

            hud.addEntry(
                entries.startBooleanToggle(Text.translatable("config.oreheightindicator.show_suitability"), Boolean.TRUE.equals(config.showSuitabilityPercent))
                    .setDefaultValue(true)
                    .setTooltip(Text.translatable("config.oreheightindicator.show_suitability.tooltip"))
                    .setSaveConsumer(value -> config.showSuitabilityPercent = value)
                    .build()
            );

            hud.addEntry(
                entries.startBooleanToggle(Text.translatable("config.oreheightindicator.animate_reorder"), Boolean.TRUE.equals(config.animateReorder))
                    .setDefaultValue(true)
                    .setTooltip(Text.translatable("config.oreheightindicator.animate_reorder.tooltip"))
                    .setSaveConsumer(value -> config.animateReorder = value)
                    .build()
            );

            hud.addEntry(
                entries.startFloatField(Text.translatable("config.oreheightindicator.ui_scale"), config.uiScale)
                    .setDefaultValue(1.0f)
                    .setMin(0.5f)
                    .setMax(3.0f)
                    .setTooltip(Text.translatable("config.oreheightindicator.ui_scale.tooltip"))
                    .setSaveConsumer(value -> config.uiScale = value)
                    .build()
            );

            hud.addEntry(
                entries.startFloatField(Text.translatable("config.oreheightindicator.minimum_suitability"), config.minimumPercent != null ? config.minimumPercent : 10.0f)
                    .setDefaultValue(10.0f)
                    .setMin(0.0f)
                    .setMax(100.0f)
                    .setTooltip(Text.translatable("config.oreheightindicator.minimum_suitability.tooltip"))
                    .setSaveConsumer(value -> config.minimumPercent = value)
                    .build()
            );

            data.addEntry(
                entries.startIntField(Text.translatable("config.oreheightindicator.update_interval"), config.updateIntervalTicks)
                    .setDefaultValue(6)
                    .setMin(1)
                    .setTooltip(Text.translatable("config.oreheightindicator.update_interval.tooltip"))
                    .setSaveConsumer(value -> config.updateIntervalTicks = value)
                    .build()
            );

            data.addEntry(
                entries.startIntField(Text.translatable("config.oreheightindicator.max_entries"), config.maxEntries)
                    .setDefaultValue(4)
                    .setMin(1)
                    .setTooltip(Text.translatable("config.oreheightindicator.max_entries.tooltip"))
                    .setSaveConsumer(value -> config.maxEntries = value)
                    .build()
            );

            displayedOres.setDescription(new Text[] {
                Text.translatable("config.oreheightindicator.displayed_ores.description"),
                Text.translatable("config.oreheightindicator.displayed_ores.instructions")
            });
            List<OreDisplayCatalog.OreOption> searchableBlocks = new ArrayList<>(
                OreDisplayCatalog.searchableBlocksExcluding(config.trackedMaterialKeys())
            );
            searchableBlocks.sort(Comparator.comparing(
                ModMenuIntegration::searchLabel,
                String.CASE_INSENSITIVE_ORDER
            ));
            Map<String, String> searchableKeys = new LinkedHashMap<>();
            for (OreDisplayCatalog.OreOption option : searchableBlocks) {
                searchableKeys.put(searchLabel(option), option.key());
            }
            if (!searchableKeys.isEmpty()) {
                AddBlockTopCell topCell = new AddBlockTopCell(searchableKeys, key -> {
                    config.setMaterialTracked(key, true);
                    config.save();
                    MinecraftClient.getInstance().setScreen(buildScreen(parent, true));
                });
                displayedOres.addEntry(
                    entries.startDropdownMenu(Text.translatable("config.oreheightindicator.add_block"), topCell)
                        .setDefaultValue("")
                        .setSelections(searchableKeys.keySet())
                        .setSuggestionMode(true)
                        .setTooltip(Text.translatable("config.oreheightindicator.add_block.tooltip"))
                        .build()
                );
            }
            List<String> configuredKeys = new ArrayList<>(config.hiddenOreKeys());
            configuredKeys.addAll(config.trackedMaterialKeys());
            List<OreDisplayCatalog.OreOption> displayOptions = new ArrayList<>(
                OreDisplayCatalog.displayedOptionsIncluding(configuredKeys)
            );
            displayOptions.sort(Comparator.comparing(
                option -> oreLabel(option).getString(),
                String.CASE_INSENSITIVE_ORDER
            ));

            if (displayOptions.isEmpty()) {
                displayedOres.addEntry(
                    entries.startTextDescription(
                        Text.translatable("config.oreheightindicator.no_blocks")
                    ).build()
                );
            } else {
                for (OreDisplayCatalog.OreOption option : displayOptions) {
                    boolean visible = option.standardOre()
                        ? config.isOreVisible(option.key())
                        : config.isMaterialTracked(option.key());
                    displayedOres.addEntry(
                        entries.startBooleanToggle(oreLabel(option), visible)
                            .setDefaultValue(option.standardOre())
                            .setTooltip(Text.literal(option.key()))
                            .setSaveConsumer(selected -> {
                                if (option.standardOre()) {
                                    config.setOreVisible(option.key(), selected);
                                } else {
                                    config.setMaterialTracked(option.key(), selected);
                                }
                            })
                            .build()
                    );
                }
            }

            builder.setSavingRunnable(config::save);
            return builder.build();
    }

    private static Text oreLabel(OreDisplayCatalog.OreOption option) {
        return option.translationKey().isBlank()
            ? Text.literal(option.fallbackName())
            : Text.translatable(option.translationKey());
    }

    private static String searchLabel(OreDisplayCatalog.OreOption option) {
        return oreLabel(option).getString() + " [" + option.key() + "]";
    }

    private static final class AddBlockTopCell
        extends DropdownBoxEntry.DefaultSelectionTopCellElement<String> {
        private final Map<String, String> searchableKeys;
        private final java.util.function.Consumer<String> addBlock;
        private final ButtonWidget addButton;

        private AddBlockTopCell(
            Map<String, String> searchableKeys,
            java.util.function.Consumer<String> addBlock
        ) {
            super("", value -> value, Text::literal);
            this.searchableKeys = searchableKeys;
            this.addBlock = addBlock;
            this.addButton = ButtonWidget.builder(
                Text.translatable("config.oreheightindicator.add_block.action"),
                button -> addSelectedBlock()
            ).dimensions(0, 0, ADD_BUTTON_WIDTH, 20).build();
        }

        private void addSelectedBlock() {
            String key = searchableKeys.get(getValue());
            if (key == null) {
                return;
            }
            getParent().getConfigScreen().saveAll(false);
            addBlock.accept(key);
        }

        @Override
        public void render(
            DrawContext context,
            int x,
            int y,
            int width,
            int height,
            int mouseX,
            int mouseY,
            float delta
        ) {
            int fieldWidth = Math.max(40, width - ADD_BUTTON_WIDTH - 4);
            super.render(context, x, y, fieldWidth, height, mouseX, mouseY, delta);
            addButton.setX(x + width - ADD_BUTTON_WIDTH);
            addButton.setY(y);
            addButton.active = searchableKeys.containsKey(getValue());
            addButton.render(context, mouseX, mouseY, delta);
        }

        @Override
        public List<? extends Element> children() {
            List<Element> children = new ArrayList<>(super.children());
            children.add(addButton);
            return children;
        }
    }
}
