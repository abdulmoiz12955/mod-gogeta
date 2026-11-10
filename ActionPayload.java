package dev.gogeta.ascension.net;

import dev.gogeta.ascension.Attacks;
import dev.gogeta.ascension.GogetaAscension;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: a key was pressed (0 next form, 1 prev form, 2 Stardust, 3 Kame, 4 Big Bang, 5 Meteor). */
public record ActionPayload(int action) implements CustomPacketPayload {
    public static final Type<ActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(GogetaAscension.MOD_ID, "action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ActionPayload> CODEC =
        StreamCodec.composite(ByteBufCodecs.VAR_INT, ActionPayload::action, ActionPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ActionPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp && payload.action() >= 0 && payload.action() <= 5)
                Attacks.perform(sp, payload.action());
        });
    }
}
