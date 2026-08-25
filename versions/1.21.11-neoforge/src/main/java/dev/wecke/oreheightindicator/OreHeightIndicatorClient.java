package dev.wecke.oreheightindicator;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wecke.oreheightindicator.config.ModConfig;
import dev.wecke.oreheightindicator.config.ModMenuIntegration;
import dev.wecke.oreheightindicator.data.AutomaticWorldgenProvider;
import dev.wecke.oreheightindicator.data.OreDataProvider;
import dev.wecke.oreheightindicator.data.OreProbabilityService;
import dev.wecke.oreheightindicator.hud.OreHudRenderer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

@Mod(value = OreHeightIndicatorClient.MOD_ID, dist = Dist.CLIENT)
public final class OreHeightIndicatorClient {
    public static final String MOD_ID = "oreheightindicator";
    private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath(MOD_ID, "main")
    );
    private static final KeyMapping TOGGLE_HUD_KEY = new KeyMapping(
        "key.oreheightindicator.toggle_hud",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_H,
        KEY_CATEGORY
    );

    private final ModConfig config;
    private final OreHudRenderer hudRenderer;
    private int updateTickCounter;

    public OreHeightIndicatorClient(IEventBus modBus, ModContainer container) {
        config = ModConfig.getCurrent();
        OreDataProvider provider = new AutomaticWorldgenProvider();
        hudRenderer = new OreHudRenderer(config, new OreProbabilityService(provider, config));

        modBus.addListener(this::registerKeyMappings);
        modBus.addListener(this::registerGuiLayers);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);

        if (ModList.get().isLoaded("cloth_config")) {
            container.registerExtensionPoint(
                IConfigScreenFactory.class,
                (modContainer, parent) -> ModMenuIntegration.create(parent)
            );
        }
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_HUD_KEY);
    }

    private void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
            VanillaGuiLayers.CHAT,
            Identifier.fromNamespaceAndPath(MOD_ID, "ore_guide"),
            (graphics, deltaTracker) -> hudRenderer.render(graphics)
        );
    }

    private void onClientTick(ClientTickEvent.Post event) {
        while (TOGGLE_HUD_KEY.consumeClick()) {
            config.hudEnabled = !config.hudEnabled;
            config.save();
        }

        Minecraft client = Minecraft.getInstance();
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
}
