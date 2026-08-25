package dev.wecke.oreheightindicator.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.wecke.oreheightindicator.data.OreDisplayCatalog;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ModConfig config = ModConfig.getCurrent();
            OreDisplayCatalog.rememberRegisteredBlocks();

            ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.oreheightindicator.title"));

            ConfigEntryBuilder entries = builder.entryBuilder();
            ConfigCategory hud = builder.getOrCreateCategory(Component.translatable("config.oreheightindicator.category.hud"));
            ConfigCategory displayedOres = builder.getOrCreateCategory(Component.translatable("config.oreheightindicator.category.displayed_ores"));
            ConfigCategory data = builder.getOrCreateCategory(Component.translatable("config.oreheightindicator.category.data"));

            hud.addEntry(
                entries.startBooleanToggle(Component.translatable("config.oreheightindicator.hud_enabled"), config.hudEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Component.translatable("config.oreheightindicator.hud_enabled.tooltip"))
                    .setSaveConsumer(value -> config.hudEnabled = value)
                    .build()
            );

            hud.addEntry(
                entries.startIntField(Component.translatable("config.oreheightindicator.hud_x"), config.hudX)
                    .setDefaultValue(8)
                    .setMin(0)
                    .setTooltip(Component.translatable("config.oreheightindicator.hud_x.tooltip"))
                    .setSaveConsumer(value -> config.hudX = value)
                    .build()
            );

            hud.addEntry(
                entries.startIntField(Component.translatable("config.oreheightindicator.hud_y"), config.hudY)
                    .setDefaultValue(8)
                    .setMin(0)
                    .setTooltip(Component.translatable("config.oreheightindicator.hud_y.tooltip"))
                    .setSaveConsumer(value -> config.hudY = value)
                    .build()
            );

            hud.addEntry(
                entries.startBooleanToggle(Component.translatable("config.oreheightindicator.show_icons"), Boolean.TRUE.equals(config.showOreIcons))
                    .setDefaultValue(true)
                    .setTooltip(Component.translatable("config.oreheightindicator.show_icons.tooltip"))
                    .setSaveConsumer(value -> config.showOreIcons = value)
                    .build()
            );

            hud.addEntry(
                entries.startBooleanToggle(Component.translatable("config.oreheightindicator.show_suitability"), Boolean.TRUE.equals(config.showSuitabilityPercent))
                    .setDefaultValue(true)
                    .setTooltip(Component.translatable("config.oreheightindicator.show_suitability.tooltip"))
                    .setSaveConsumer(value -> config.showSuitabilityPercent = value)
                    .build()
            );

            hud.addEntry(
                entries.startBooleanToggle(Component.translatable("config.oreheightindicator.animate_reorder"), Boolean.TRUE.equals(config.animateReorder))
                    .setDefaultValue(true)
                    .setTooltip(Component.translatable("config.oreheightindicator.animate_reorder.tooltip"))
                    .setSaveConsumer(value -> config.animateReorder = value)
                    .build()
            );

            hud.addEntry(
                entries.startFloatField(Component.translatable("config.oreheightindicator.ui_scale"), config.uiScale)
                    .setDefaultValue(1.0f)
                    .setMin(0.5f)
                    .setMax(3.0f)
                    .setTooltip(Component.translatable("config.oreheightindicator.ui_scale.tooltip"))
                    .setSaveConsumer(value -> config.uiScale = value)
                    .build()
            );

            hud.addEntry(
                entries.startFloatField(Component.translatable("config.oreheightindicator.minimum_suitability"), config.minimumPercent != null ? config.minimumPercent : 10.0f)
                    .setDefaultValue(10.0f)
                    .setMin(0.0f)
                    .setMax(100.0f)
                    .setTooltip(Component.translatable("config.oreheightindicator.minimum_suitability.tooltip"))
                    .setSaveConsumer(value -> config.minimumPercent = value)
                    .build()
            );

            data.addEntry(
                entries.startIntField(Component.translatable("config.oreheightindicator.update_interval"), config.updateIntervalTicks)
                    .setDefaultValue(6)
                    .setMin(1)
                    .setTooltip(Component.translatable("config.oreheightindicator.update_interval.tooltip"))
                    .setSaveConsumer(value -> config.updateIntervalTicks = value)
                    .build()
            );

            data.addEntry(
                entries.startIntField(Component.translatable("config.oreheightindicator.max_entries"), config.maxEntries)
                    .setDefaultValue(4)
                    .setMin(1)
                    .setTooltip(Component.translatable("config.oreheightindicator.max_entries.tooltip"))
                    .setSaveConsumer(value -> config.maxEntries = value)
                    .build()
            );

            displayedOres.setDescription(new Component[] {
                Component.translatable("config.oreheightindicator.displayed_ores.description"),
                Component.translatable("config.oreheightindicator.displayed_ores.instructions")
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
                displayedOres.addEntry(
                    entries.startStringDropdownMenu(Component.translatable("config.oreheightindicator.add_block"), "")
                        .setDefaultValue("")
                        .setSelections(searchableKeys.keySet())
                        .setSuggestionMode(true)
                        .setTooltip(Component.translatable("config.oreheightindicator.add_block.tooltip"))
                        .setSaveConsumer(selected -> {
                            String key = searchableKeys.get(selected);
                            if (key != null) config.setMaterialTracked(key, true);
                        })
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
                        Component.translatable("config.oreheightindicator.no_blocks")
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
                            .setTooltip(Component.literal(option.key()))
                            .setSaveConsumer(selected -> {
                                if (option.standardOre()) config.setOreVisible(option.key(), selected);
                                else config.setMaterialTracked(option.key(), selected);
                            })
                            .build()
                    );
                }
            }

            builder.setSavingRunnable(config::save);
            return builder.build();
        };
    }

    private static Component oreLabel(OreDisplayCatalog.OreOption option) {
        return option.translationKey().isBlank()
            ? Component.literal(option.fallbackName())
            : Component.translatable(option.translationKey());
    }

    private static String searchLabel(OreDisplayCatalog.OreOption option) {
        return oreLabel(option).getString() + " [" + option.key() + "]";
    }
}
