package dev.wecke.oreheightindicator;

import dev.wecke.oreheightindicator.config.ModConfig;
import dev.wecke.oreheightindicator.data.DynamicWorldgenProviderStub;
import dev.wecke.oreheightindicator.data.OreDataProvider;
import dev.wecke.oreheightindicator.data.OreProbabilityService;
import dev.wecke.oreheightindicator.data.StaticVanilla1211Provider;
import dev.wecke.oreheightindicator.hud.OreHudRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class OreHeightIndicatorClient implements ClientModInitializer {
    private ModConfig config;
    private OreHudRenderer hudRenderer;
    private KeyBinding toggleHudKey;
    private int updateTickCounter = 0;

    @Override
    public void onInitializeClient() {
        config = ModConfig.getCurrent();

        OreDataProvider provider = config.useDynamicProvider
            ? new DynamicWorldgenProviderStub()
            : new StaticVanilla1211Provider();

        OreProbabilityService probabilityService = new OreProbabilityService(provider);
        hudRenderer = new OreHudRenderer(config, probabilityService);

        toggleHudKey = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                "key.oreheightindicator.toggle_hud",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.oreheightindicator"
            )
        );

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> hudRenderer.render(drawContext));
    }

    private void onClientTick(MinecraftClient client) {
        while (toggleHudKey.wasPressed()) {
            config.hudEnabled = !config.hudEnabled;
            config.save();
        }

        if (!config.hudEnabled || client.player == null) {
            return;
        }

        updateTickCounter++;
        if (updateTickCounter < config.updateIntervalTicks) {
            return;
        }
        updateTickCounter = 0;

        int currentY = (int) Math.floor(client.player.getY());
        hudRenderer.updateForY(currentY);
    }
}
