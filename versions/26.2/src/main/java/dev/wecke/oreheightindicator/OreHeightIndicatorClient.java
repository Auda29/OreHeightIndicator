package dev.wecke.oreheightindicator;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wecke.oreheightindicator.config.ModConfig;
import dev.wecke.oreheightindicator.data.OreDataProvider;
import dev.wecke.oreheightindicator.data.OreProbabilityService;
import dev.wecke.oreheightindicator.data.AutomaticWorldgenProvider;
import dev.wecke.oreheightindicator.hud.OreHudRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class OreHeightIndicatorClient implements ClientModInitializer {
    private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath("oreheightindicator", "main")
    );

    private ModConfig config;
    private OreHudRenderer hudRenderer;
    private KeyMapping toggleHudKey;
    private int updateTickCounter = 0;

    @Override
    public void onInitializeClient() {
        config = ModConfig.getCurrent();

        OreDataProvider provider = new AutomaticWorldgenProvider();

        OreProbabilityService probabilityService = new OreProbabilityService(provider, config);
        hudRenderer = new OreHudRenderer(config, probabilityService);

        KeyMapping createdToggleKey = createToggleHudKeyBinding();
        if (createdToggleKey != null) {
            toggleHudKey = KeyMappingHelper.registerKeyMapping(createdToggleKey);
        }

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.CHAT,
            Identifier.fromNamespaceAndPath("oreheightindicator", "ore_guide"),
            (graphics, tickCounter) -> hudRenderer.render(graphics)
        );
    }

    private void onClientTick(Minecraft client) {
        while (toggleHudKey != null && toggleHudKey.consumeClick()) {
            config.hudEnabled = !config.hudEnabled;
            config.save();
        }

        if (client.player == null) {
            return;
        }

        updateTickCounter++;
        if (updateTickCounter < config.updateIntervalTicks) {
            return;
        }
        updateTickCounter = 0;

        int currentY = (int) Math.floor(client.player.getY());
        hudRenderer.update(client, currentY);
    }

    private static KeyMapping createToggleHudKeyBinding() {
        return new KeyMapping(
            "key.oreheightindicator.toggle_hud",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            KEY_CATEGORY
        );
    }
}
