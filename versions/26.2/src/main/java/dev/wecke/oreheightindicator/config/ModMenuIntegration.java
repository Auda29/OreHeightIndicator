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
import java.util.List;

public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ModConfig config = ModConfig.getCurrent();

            ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("Ore Height Indicator Config"));

            ConfigEntryBuilder entries = builder.entryBuilder();
            ConfigCategory hud = builder.getOrCreateCategory(Component.literal("HUD"));
            ConfigCategory displayedOres = builder.getOrCreateCategory(Component.literal("Displayed ores"));
            ConfigCategory data = builder.getOrCreateCategory(Component.literal("Data & Performance"));

            hud.addEntry(
                entries.startBooleanToggle(Component.literal("HUD Enabled"), config.hudEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Component.literal("Shows or hides the compact ore height overlay."))
                    .setSaveConsumer(value -> config.hudEnabled = value)
                    .build()
            );

            hud.addEntry(
                entries.startIntField(Component.literal("HUD X"), config.hudX)
                    .setDefaultValue(8)
                    .setMin(0)
                    .setTooltip(Component.literal("Horizontal HUD offset in pixels from the right edge."))
                    .setSaveConsumer(value -> config.hudX = value)
                    .build()
            );

            hud.addEntry(
                entries.startIntField(Component.literal("HUD Y"), config.hudY)
                    .setDefaultValue(8)
                    .setMin(0)
                    .setTooltip(Component.literal("Vertical HUD offset in pixels from the top edge."))
                    .setSaveConsumer(value -> config.hudY = value)
                    .build()
            );

            hud.addEntry(
                entries.startBooleanToggle(Component.literal("Show Entry Icons"), Boolean.TRUE.equals(config.showOreIcons))
                    .setDefaultValue(true)
                    .setTooltip(Component.literal("Show or hide item icons for each HUD row."))
                    .setSaveConsumer(value -> config.showOreIcons = value)
                    .build()
            );

            hud.addEntry(
                entries.startBooleanToggle(Component.literal("Show Suitability %"), Boolean.TRUE.equals(config.showSuitabilityPercent))
                    .setDefaultValue(true)
                    .setTooltip(Component.literal("Shows the current height relative to each ore's best detected height. This is not an absolute spawn chance."))
                    .setSaveConsumer(value -> config.showSuitabilityPercent = value)
                    .build()
            );

            hud.addEntry(
                entries.startBooleanToggle(Component.literal("Animate Reorder"), Boolean.TRUE.equals(config.animateReorder))
                    .setDefaultValue(true)
                    .setTooltip(Component.literal("Smooth row movement when ore ranking changes."))
                    .setSaveConsumer(value -> config.animateReorder = value)
                    .build()
            );

            hud.addEntry(
                entries.startFloatField(Component.literal("UI Scale"), config.uiScale)
                    .setDefaultValue(1.0f)
                    .setMin(0.5f)
                    .setMax(3.0f)
                    .setTooltip(Component.literal("Scales the complete HUD size. 1.0 = default size."))
                    .setSaveConsumer(value -> config.uiScale = value)
                    .build()
            );

            hud.addEntry(
                entries.startFloatField(Component.literal("Minimum Suitability %"), config.minimumPercent != null ? config.minimumPercent : 10.0f)
                    .setDefaultValue(10.0f)
                    .setMin(0.0f)
                    .setMax(100.0f)
                    .setTooltip(Component.literal("Hides standard ores below this share of their best detected height. Selected materials remain visible."))
                    .setSaveConsumer(value -> config.minimumPercent = value)
                    .build()
            );

            data.addEntry(
                entries.startIntField(Component.literal("Update Interval (ticks)"), config.updateIntervalTicks)
                    .setDefaultValue(6)
                    .setMin(1)
                    .setTooltip(Component.literal("How often the current height, biome and ore relevance are checked (20 ticks = 1 second)."))
                    .setSaveConsumer(value -> config.updateIntervalTicks = value)
                    .build()
            );

            data.addEntry(
                entries.startIntField(Component.literal("Max HUD Entries"), config.maxEntries)
                    .setDefaultValue(4)
                    .setMin(1)
                    .setTooltip(Component.literal("Maximum number of ore and material rows shown in the HUD list."))
                    .setSaveConsumer(value -> config.maxEntries = value)
                    .build()
            );

            displayedOres.setDescription(new Component[] {
                Component.literal("Use the search box above to find ores or detected worldgen materials such as Andesite."),
                Component.literal("Ores are enabled by default. Additional materials appear after you enable them.")
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
                        Component.literal("Enter a world once to detect its worldgen materials.")
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
}
