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

            ConfigCategory general = builder.getOrCreateCategory(Text.literal("General"));
            ConfigEntryBuilder entries = builder.entryBuilder();

            general.addEntry(
                entries.startBooleanToggle(Text.literal("HUD Enabled"), config.hudEnabled)
                    .setDefaultValue(true)
                    .setSaveConsumer(value -> config.hudEnabled = value)
                    .build()
            );

            general.addEntry(
                entries.startIntField(Text.literal("HUD X"), config.hudX)
                    .setDefaultValue(8)
                    .setMin(0)
                    .setSaveConsumer(value -> config.hudX = value)
                    .build()
            );

            general.addEntry(
                entries.startIntField(Text.literal("HUD Y"), config.hudY)
                    .setDefaultValue(8)
                    .setMin(0)
                    .setSaveConsumer(value -> config.hudY = value)
                    .build()
            );

            general.addEntry(
                entries.startIntField(Text.literal("Update Interval (ticks)"), config.updateIntervalTicks)
                    .setDefaultValue(6)
                    .setMin(1)
                    .setSaveConsumer(value -> config.updateIntervalTicks = value)
                    .build()
            );

            general.addEntry(
                entries.startIntField(Text.literal("Max Ore Entries"), config.maxEntries)
                    .setDefaultValue(6)
                    .setMin(1)
                    .setSaveConsumer(value -> config.maxEntries = value)
                    .build()
            );

            general.addEntry(
                entries.startBooleanToggle(Text.literal("Use Dynamic Provider (experimental)"), config.useDynamicProvider)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("Current implementation is a stub and may show no ore data."))
                    .setSaveConsumer(value -> config.useDynamicProvider = value)
                    .build()
            );

            builder.setSavingRunnable(config::save);
            return builder.build();
        };
    }
}
