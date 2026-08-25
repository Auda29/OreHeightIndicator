package dev.wecke.oreheightindicator.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalizationTest {
    private static final Set<String> REQUIRED_KEYS = Set.of(
        "key.oreheightindicator.toggle_hud",
        "category.oreheightindicator",
        "key.category.oreheightindicator.main",
        "config.oreheightindicator.title",
        "config.oreheightindicator.category.hud",
        "config.oreheightindicator.category.displayed_ores",
        "config.oreheightindicator.category.data",
        "config.oreheightindicator.hud_enabled",
        "config.oreheightindicator.hud_enabled.tooltip",
        "config.oreheightindicator.hud_x",
        "config.oreheightindicator.hud_x.tooltip",
        "config.oreheightindicator.hud_y",
        "config.oreheightindicator.hud_y.tooltip",
        "config.oreheightindicator.show_icons",
        "config.oreheightindicator.show_icons.tooltip",
        "config.oreheightindicator.show_suitability",
        "config.oreheightindicator.show_suitability.tooltip",
        "config.oreheightindicator.animate_reorder",
        "config.oreheightindicator.animate_reorder.tooltip",
        "config.oreheightindicator.ui_scale",
        "config.oreheightindicator.ui_scale.tooltip",
        "config.oreheightindicator.minimum_suitability",
        "config.oreheightindicator.minimum_suitability.tooltip",
        "config.oreheightindicator.update_interval",
        "config.oreheightindicator.update_interval.tooltip",
        "config.oreheightindicator.max_entries",
        "config.oreheightindicator.max_entries.tooltip",
        "config.oreheightindicator.displayed_ores.description",
        "config.oreheightindicator.displayed_ores.instructions",
        "config.oreheightindicator.add_block",
        "config.oreheightindicator.add_block.tooltip",
        "config.oreheightindicator.add_block.action",
        "config.oreheightindicator.no_blocks"
    );

    @Test
    void englishAndGermanContainTheSameCompleteKeySet() throws IOException {
        JsonObject english = readLanguage("en_us");
        JsonObject german = readLanguage("de_de");

        assertEquals(english.keySet(), german.keySet());
        assertTrue(english.keySet().containsAll(REQUIRED_KEYS));
        assertEquals(REQUIRED_KEYS.size(), english.keySet().size());
        for (String key : REQUIRED_KEYS) {
            assertFalse(english.get(key).getAsString().isBlank(), key + " is blank in en_us");
            assertFalse(german.get(key).getAsString().isBlank(), key + " is blank in de_de");
        }
    }

    private static JsonObject readLanguage(String language) throws IOException {
        String path = "assets/oreheightindicator/lang/" + language + ".json";
        InputStream stream = LocalizationTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, path + " is missing");
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
