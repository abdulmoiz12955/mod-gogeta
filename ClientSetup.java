package dev.gogeta.ascension.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.gogeta.ascension.ModEntities;
import dev.gogeta.ascension.net.ActionPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ClientSetup {
    private static final String CAT = "key.categories.gogeta_ascension";
    private static final KeyMapping[] KEYS = {
        new KeyMapping("key.gogeta_ascension.next_form", InputConstants.KEY_G, CAT),
        new KeyMapping("key.gogeta_ascension.prev_form", InputConstants.KEY_V, CAT),
        new KeyMapping("key.gogeta_ascension.stardust", InputConstants.KEY_R, CAT),
        new KeyMapping("key.gogeta_ascension.kamehameha", InputConstants.KEY_Z, CAT),
        new KeyMapping("key.gogeta_ascension.bigbang", InputConstants.KEY_X, CAT),
        new KeyMapping("key.gogeta_ascension.meteor", InputConstants.KEY_C, CAT),
    };

    public static void init(IEventBus modBus) {
        modBus.addListener(ClientSetup::registerKeys);
        modBus.addListener(ClientSetup::registerRenderers);
        NeoForge.EVENT_BUS.addListener(ClientSetup::onClientTick);
    }

    private static void registerKeys(RegisterKeyMappingsEvent e) { for (KeyMapping k : KEYS) e.register(k); }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers e) {
        // The Getsuga is drawn purely with server-sent particles, so no model is needed.
        e.registerEntityRenderer(ModEntities.GETSUGA.get(), NoopRenderer::new);
    }

    private static void onClientTick(ClientTickEvent.Post e) {
        if (Minecraft.getInstance().player == null) return;
        for (int i = 0; i < KEYS.length; i++)
            while (KEYS[i].consumeClick()) PacketDistributor.sendToServer(new ActionPayload(i));
    }

    private ClientSetup() {}
}
