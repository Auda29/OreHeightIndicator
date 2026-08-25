package dev.wecke.oreheightindicator.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.wecke.oreheightindicator.data.OreDisplayCatalog;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ModConfig config = ModConfig.getCurrent();
            OreDisplayCatalog.rememberRegisteredBlocks();

            ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("Ore Height Indicator Config"));

            ConfigEntryBuilder entries = builder.entryBuilder();
            ConfigCategory hud = builder.getOrCreateCategory(Text.literal("HUD"));
            ConfigCategory displayedOres = builder.getOrCreateCategory(Text.literal("Displayed ores"));
            ConfigCategory data = builder.getOrCreateCategory(Text.literal("Data & Performance"));

            hud.addEntry(
                entries.startBooleanToggle(Text.literal("HUD Enabled"), config.hudEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Shows or hides the compact ore height overlay."))
                    .setSaveConsumer(value -> config.hudEnabled = value)
                    .build()
            );

            hud.addEntry(
                entries.startIntField(Text.literal("HUD X"), config.hudX)
                    .setDefaultValue(8)
                    .setMin(0)
                    .setTooltip(Text.literal("Horizontal HUD offset in pixels from the right edge."))
                    .setSaveConsumer(value -> config.hudX = value)
                    .build()
            );

            hud.addEntry(
                entries.startIntField(Text.literal("HUD Y"), config.hudY)
                    .setDefaultValue(8)
                    .setMin(0)
                    .setTooltip(Text.literal("Vertical HUD offset in pixels from the top edge."))
                    .setSaveConsumer(value -> config.hudY = value)
                    .build()
            );

            hud.addEntry(
                entries.startBooleanToggle(Text.literal("Show Entry Icons"), Boolean.TRUE.equals(config.showOreIcons))
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Show or hide item icons for each HUD row."))
                    .setSaveConsumer(value -> config.showOreIcons = value)
                    .build()
            );

            hud.addEntry(
                entries.startBooleanToggle(Text.literal("Show Suitability %"), Boolean.TRUE.equals(config.showSuitabilityPercent))
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Shows the current height relative to each ore's best detected height. This is not an absolute spawn chance."))
                    .setSaveConsumer(value -> config.showSuitabilityPercent = value)
                    .build()
            );

            hud.addEntry(
                entries.startBooleanToggle(Text.literal("Animate Reorder"), Boolean.TRUE.equals(config.animateReorder))
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Smooth row movement when ore ranking changes."))
                    .setSaveConsumer(value -> config.animateReorder = value)
                    .build()
            );

            hud.addEntry(
                entries.startFloatField(Text.literal("UI Scale"), config.uiScale)
                    .setDefaultValue(1.0f)
                    .setMin(0.5f)
                    .setMax(3.0f)
                    .setTooltip(Text.literal("Scales the complete HUD size. 1.0 = default size."))
                    .setSaveConsumer(value -> config.uiScale = value)
                    .build()
            );

            hud.addEntry(
                entries.startFloatField(Text.literal("Minimum Suitability %"), config.minimumPercent != null ? config.minimumPercent : 10.0f)
                    .setDefaultValue(10.0f)
                    .setMin(0.0f)
                    .setMax(100.0f)
                    .setTooltip(Text.literal("Hides standard ores below this share of their best detected height. Selected materials remain visible."))
                    .setSaveConsumer(value -> config.minimumPercent = value)
                    .build()
            );

            data.addEntry(
                entries.startIntField(Text.literal("Update Interval (ticks)"), config.updateIntervalTicks)
                    .setDefaultValue(6)
                    .setMin(1)
                    .setTooltip(Text.literal("How often the current height, biome and ore relevance are checked (20 ticks = 1 second)."))
                    .setSaveConsumer(value -> config.updateIntervalTicks = value)
                    .build()
            );

            data.addEntry(
                entries.startIntField(Text.literal("Max HUD Entries"), config.maxEntries)
                    .setDefaultValue(4)
                    .setMin(1)
                    .setTooltip(Text.literal("Maximum number of ore and material rows shown in the HUD list."))
                    .setSaveConsumer(value -> config.maxEntries = value)
                    .build()
            );

            displayedOres.setDescription(new Text[] {
                Text.literal("Use the search box above to find any registered block, including blocks added by mods."),
                Text.literal("Ores are enabled by default. Other blocks are opt-in and use active worldgen data or a nearby loaded-chunk sample.")
            });
            List<String> configuredKeys = new ArrayList<>(config.hiddenOreKeys());
            configuredKeys.addAll(config.trackedMaterialKeys());
            List<OreDisplayCatalog.OreOption> displayOptions = new ArrayList<>(
                OreDisplayCatalog.knownOresIncluding(configuredKeys)
            );
            displayOptions.sort(Comparator.comparing(
                option -> oreLabel(option).getString(),
                String.CASE_INSENSITIVE_ORDER
            ));

            if (displayOptions.isEmpty()) {
                displayedOres.addEntry(
                    entries.startTextDescription(
                        Text.literal("No registered blocks are available yet.")
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
        };
    }

    private static Text oreLabel(OreDisplayCatalog.OreOption option) {
        return option.translationKey().isBlank()
            ? Text.literal(option.fallbackName())
            : Text.translatable(option.translationKey());
    }
}
