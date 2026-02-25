package dev.wecke.oreheightindicator.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.text.Text;

public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ModConfig config = ModConfig.getCurrent();

            ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("Ore Height Indicator Config"));

            ConfigEntryBuilder entries = builder.entryBuilder();
            ConfigCategory hud = builder.getOrCreateCategory(Text.literal("HUD"));
            ConfigCategory data = builder.getOrCreateCategory(Text.literal("Data & Performance"));

            hud.addEntry(
                entries.startBooleanToggle(Text.literal("HUD Enabled"), config.hudEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Shows or hides the ore probability overlay on screen."))
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
                entries.startBooleanToggle(Text.literal("Show Ore Icons"), Boolean.TRUE.equals(config.showOreIcons))
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Show or hide ore item icons for each HUD row."))
                    .setSaveConsumer(value -> config.showOreIcons = value)
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
                entries.startFloatField(Text.literal("Minimum Percent"), config.minimumPercent != null ? config.minimumPercent : 0.5f)
                    .setDefaultValue(0.5f)
                    .setMin(0.0f)
                    .setMax(50.0f)
                    .setTooltip(Text.literal("Ores below this percentage are hidden. Set to 0 to show all."))
                    .setSaveConsumer(value -> config.minimumPercent = value)
                    .build()
            );

            data.addEntry(
                entries.startIntField(Text.literal("Update Interval (ticks)"), config.updateIntervalTicks)
                    .setDefaultValue(6)
                    .setMin(1)
                    .setTooltip(Text.literal("How often ore probabilities are recalculated (20 ticks = 1 second)."))
                    .setSaveConsumer(value -> config.updateIntervalTicks = value)
                    .build()
            );

            data.addEntry(
                entries.startIntField(Text.literal("Max Ore Entries"), config.maxEntries)
                    .setDefaultValue(6)
                    .setMin(1)
                    .setTooltip(Text.literal("Maximum number of ore rows shown in the HUD list."))
                    .setSaveConsumer(value -> config.maxEntries = value)
                    .build()
            );

            data.addEntry(
                entries.startBooleanToggle(Text.literal("Use Dynamic Provider (experimental)"), config.useDynamicProvider)
                    .setDefaultValue(false)
                    .setTooltip(
                        Text.literal("Reads ore data from worldgen dynamically."),
                        Text.literal("Requires restart to fully apply."),
                        Text.literal("Experimental MVP: vanilla ore features only."),
                        Text.literal("Falls back to static provider if initialization fails.")
                    )
                    .setSaveConsumer(value -> config.useDynamicProvider = value)
                    .build()
            );

            builder.setSavingRunnable(config::save);
            return builder.build();
        };
    }
}
