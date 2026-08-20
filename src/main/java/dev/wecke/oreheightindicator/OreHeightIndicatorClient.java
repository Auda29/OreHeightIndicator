package dev.wecke.oreheightindicator;

import dev.wecke.oreheightindicator.config.ModConfig;
import dev.wecke.oreheightindicator.data.OreDataProvider;
import dev.wecke.oreheightindicator.data.OreProbabilityService;
import dev.wecke.oreheightindicator.data.AutomaticWorldgenProvider;
import dev.wecke.oreheightindicator.hud.OreHudRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class OreHeightIndicatorClient implements ClientModInitializer {
    private static final KeyBinding.Category KEY_CATEGORY = KeyBinding.Category.create(
        Identifier.of("oreheightindicator", "main")
    );

    private ModConfig config;
    private OreHudRenderer hudRenderer;
    private KeyBinding toggleHudKey;
    private int updateTickCounter = 0;

    @Override
    public void onInitializeClient() {
        config = ModConfig.getCurrent();

        OreDataProvider provider = new AutomaticWorldgenProvider();

        OreProbabilityService probabilityService = new OreProbabilityService(provider);
        hudRenderer = new OreHudRenderer(config, probabilityService);

        KeyBinding createdToggleKey = createToggleHudKeyBinding();
        if (createdToggleKey != null) {
            toggleHudKey = KeyBindingHelper.registerKeyBinding(createdToggleKey);
        }

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> hudRenderer.render(drawContext));
    }

    private void onClientTick(MinecraftClient client) {
        while (toggleHudKey != null && toggleHudKey.wasPressed()) {
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

    private static KeyBinding createToggleHudKeyBinding() {
        return new KeyBinding(
            "key.oreheightindicator.toggle_hud",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            KEY_CATEGORY
        );
    }
}
