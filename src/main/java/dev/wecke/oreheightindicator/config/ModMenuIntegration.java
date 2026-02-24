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
                    .setTooltip(Text.literal("Horizontal HUD offset in pixels from the left edge."))
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
                        Text.literal("Experimental: current implementation is a stub.")
                    )
                    .setSaveConsumer(value -> config.useDynamicProvider = value)
                    .build()
            );

            builder.setSavingRunnable(config::save);
            return builder.build();
        };
    }
}
