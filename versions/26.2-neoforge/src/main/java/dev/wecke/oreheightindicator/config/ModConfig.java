package dev.wecke.oreheightindicator.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import dev.wecke.oreheightindicator.data.OreDisplayCatalog;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ModConfig current;

    public boolean hudEnabled = true;
    public int hudX = 8;
    public int hudY = 8;
    public int updateIntervalTicks = 6;
    public int maxEntries = 4;
    public Boolean showOreIcons = true;
    public Boolean showSuitabilityPercent = true;
    public Boolean animateReorder = true;
    public Float uiScale = 1.0f;
    public Float minimumPercent = 10.0f;
    public List<String> hiddenOres = new ArrayList<>();
    public List<String> trackedMaterials = new ArrayList<>();

    public boolean isOreVisible(String oreKey) {
        String normalized = normalizeOreKey(oreKey);
        return normalized == null || !hiddenOres.contains(normalized);
    }

    public void setOreVisible(String oreKey, boolean visible) {
        String normalized = normalizeOreKey(oreKey);
        if (normalized == null) {
            return;
        }

        if (visible) {
            hiddenOres.removeIf(normalized::equals);
        } else if (!hiddenOres.contains(normalized)) {
            hiddenOres.add(normalized);
            hiddenOres.sort(String::compareTo);
        }
    }

    public List<String> hiddenOreKeys() {
        return List.copyOf(hiddenOres);
    }

    public boolean isMaterialTracked(String materialKey) {
        String normalized = normalizeOreKey(materialKey);
        return normalized != null && trackedMaterials.contains(normalized);
    }

    public void setMaterialTracked(String materialKey, boolean tracked) {
        String normalized = normalizeOreKey(materialKey);
        if (normalized == null) return;
        if (tracked) {
            if (!trackedMaterials.contains(normalized)) {
                trackedMaterials.add(normalized);
                trackedMaterials.sort(String::compareTo);
            }
        } else {
            trackedMaterials.removeIf(normalized::equals);
        }
    }

    public boolean isDisplayed(String key) {
        return OreDisplayCatalog.isStandardOre(key) ? isOreVisible(key) : isMaterialTracked(key);
    }

    public List<String> trackedMaterialKeys() {
        return List.copyOf(trackedMaterials);
    }

    public static ModConfig load() {
        Path configPath = configPath();
        if (!Files.exists(configPath)) {
            ModConfig config = new ModConfig();
            config.save();
            current = config;
            return config;
        }

        try (Reader reader = Files.newBufferedReader(configPath)) {
            ModConfig config = GSON.fromJson(reader, ModConfig.class);
            if (config == null) {
                config = new ModConfig();
            }
            config.sanitize();
            current = config;
            return config;
        } catch (IOException | JsonParseException ignored) {
            ModConfig config = new ModConfig();
            config.save();
            current = config;
            return config;
        }
    }

    public static ModConfig getCurrent() {
        if (current == null) {
            current = load();
        }
        return current;
    }

    public void save() {
        sanitize();
        Path configPath = configPath();
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
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
        if (showOreIcons == null) {
            showOreIcons = true;
        }
        if (showSuitabilityPercent == null) {
            showSuitabilityPercent = true;
        }
        if (animateReorder == null) {
            animateReorder = true;
        }
        if (uiScale == null) {
            uiScale = 1.0f;
        }
        uiScale = Math.max(0.5f, Math.min(3.0f, uiScale));
        if (minimumPercent == null) {
            minimumPercent = 10.0f;
        }
        minimumPercent = Math.max(0.0f, Math.min(100.0f, minimumPercent));
        if (hiddenOres == null) {
            hiddenOres = new ArrayList<>();
        }
        hiddenOres = hiddenOres.stream()
            .map(ModConfig::normalizeOreKey)
            .filter(key -> key != null)
            .distinct()
            .sorted()
            .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        if (trackedMaterials == null) trackedMaterials = new ArrayList<>();
        trackedMaterials = trackedMaterials.stream()
            .map(ModConfig::normalizeOreKey)
            .filter(key -> key != null)
            .filter(key -> !OreDisplayCatalog.isStandardOre(key))
            .distinct()
            .sorted()
            .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private static String normalizeOreKey(String oreKey) {
        if (oreKey == null) {
            return null;
        }
        String normalized = oreKey.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve("oreheightindicator.json");
    }
}
