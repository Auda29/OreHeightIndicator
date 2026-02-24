package dev.wecke.oreheightindicator.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("oreheightindicator.json");

    public boolean hudEnabled = true;
    public int hudX = 8;
    public int hudY = 8;
    public int updateIntervalTicks = 6;
    public int maxEntries = 6;
    public boolean useDynamicProvider = false;

    public static ModConfig load() {
        if (!Files.exists(CONFIG_PATH)) {
            ModConfig config = new ModConfig();
            config.save();
            return config;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            ModConfig config = GSON.fromJson(reader, ModConfig.class);
            if (config == null) {
                config = new ModConfig();
            }
            config.sanitize();
            return config;
        } catch (IOException | JsonParseException ignored) {
            ModConfig config = new ModConfig();
            config.save();
            return config;
        }
    }

    public void save() {
        sanitize();
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException ignored) {
        }
    }

    private void sanitize() {
        updateIntervalTicks = Math.max(1, updateIntervalTicks);
        maxEntries = Math.max(1, maxEntries);
        hudX = Math.max(0, hudX);
        hudY = Math.max(0, hudY);
    }
}
