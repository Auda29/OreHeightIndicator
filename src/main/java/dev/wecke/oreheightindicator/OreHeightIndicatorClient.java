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

import java.lang.reflect.Constructor;

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

    private static KeyBinding createToggleHudKeyBinding() {
        final String translationKey = "key.oreheightindicator.toggle_hud";
        final String category = "category.oreheightindicator";
        final int keyCode = GLFW.GLFW_KEY_H;
        final InputUtil.Key key = InputUtil.Type.KEYSYM.createFromCode(keyCode);

        // Try known constructor shapes first.
        for (Constructor<?> constructor : KeyBinding.class.getConstructors()) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            Object[] args = new Object[parameterTypes.length];
            boolean supported = true;

            for (int i = 0; i < parameterTypes.length; i++) {
                Class<?> type = parameterTypes[i];
                String typeName = type.getName();

                if (type == String.class) {
                    args[i] = (i == 0) ? translationKey : category;
                } else if (type == int.class || type == Integer.class) {
                    args[i] = keyCode;
                } else if ("net.minecraft.client.util.InputUtil$Type".equals(typeName)) {
                    args[i] = InputUtil.Type.KEYSYM;
                } else if ("net.minecraft.client.util.InputUtil$Key".equals(typeName)) {
                    args[i] = key;
                } else {
                    supported = false;
                    break;
                }
            }

            if (!supported) {
                continue;
            }

            try {
                return (KeyBinding) constructor.newInstance(args);
            } catch (ReflectiveOperationException ignored) {
                // Try next compatible constructor candidate.
            }
        }

        System.err.println("[OreHeightIndicator] Could not create keybinding constructor for this Minecraft version. HUD toggle key is disabled.");
        return null;
    }
}
