package dev.gogeta.ascension;

import dev.gogeta.ascension.client.ClientSetup;
import dev.gogeta.ascension.net.ActionPayload;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(GogetaAscension.MOD_ID)
public final class GogetaAscension {
    public static final String MOD_ID = "gogeta_ascension";

    public GogetaAscension(IEventBus modBus, ModContainer container) {
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        container.registerConfig(ModConfig.Type.COMMON, GogetaConfig.SPEC);
        modBus.addListener(this::registerPayloads);
        modBus.addListener(this::addCreative);
        NeoForge.EVENT_BUS.register(new GameEvents());
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.init(modBus);
        }
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar("1");
        reg.playToServer(ActionPayload.TYPE, ActionPayload.CODEC, ActionPayload::handle);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ModItems.BANKAI_BLADE.get());
        }
    }
}
