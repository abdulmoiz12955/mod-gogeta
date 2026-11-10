package dev.gogeta.ascension;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class GameEvents {
    private static final String FLIGHT_TAG = "gogeta_blade_flight";
    private static final float FLY_SPEED = 0.3f; // vanilla 0.05
    private static final double UI_DODGE_CHANCE = 0.80;
    private static final float UI_REDUCTION = 0.05f; // takes 5% of damage when it fails to dodge

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post e) { Tasks.tick(); }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent e) { Tasks.clear(); }

    // ------------------------------------------------------------ damage
    @SubscribeEvent
    public void onDamage(LivingIncomingDamageEvent e) {
        LivingEntity victim = e.getEntity();
        DamageSource src = e.getSource();

        // melee multiplier (only direct player melee; our attacks use magic damage and are pre-scaled)
        if (src.getEntity() instanceof ServerPlayer atk && src.getDirectEntity() == atk && src.is(DamageTypes.PLAYER_ATTACK)) {
            Form f = Form.of(atk);
            if (f != null) e.setAmount((float) (e.getAmount() * f.multiplier()));
        }

        // Ultra Instinct: auto-dodge or heavy reduction. /kill and the void still work.
        if (victim instanceof ServerPlayer sp && Form.of(sp) == Form.ULTRA && !src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            ServerLevel l = sp.serverLevel();
            if (sp.getRandom().nextDouble() < UI_DODGE_CHANCE) {
                e.setCanceled(true);
                Vec3 side = sp.getLookAngle().cross(new Vec3(0, 1, 0)).normalize().scale(sp.getRandom().nextBoolean() ? 0.6 : -0.6);
                for (int i = 0; i < 12; i++)
                    Fx.dust(l, sp.getX(), sp.getY() + 1, sp.getZ(), 0.85f, 0.9f, 1f, 1.5f, 2, 0.4);
                l.sendParticles(ParticleTypes.END_ROD, sp.getX(), sp.getY() + 1, sp.getZ(), 10, 0.3, 0.5, 0.3, 0.05);
                l.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6f, 1.8f);
                sp.setDeltaMovement(sp.getDeltaMovement().add(side.x, 0.1, side.z));
                sp.hurtMarked = true;
            } else {
                e.setAmount(e.getAmount() * UI_REDUCTION);
            }
        }
    }

    // ------------------------------------------------------------ per-player tick
    private static boolean hasBlade(ServerPlayer p) {
        return p.getInventory().contains((ItemStack s) -> s.is(ModItems.BANKAI_BLADE.get()));
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.isSpectator()) return;
        ServerLevel l = p.serverLevel();
        long t = l.getGameTime();
        boolean has = hasBlade(p);
        boolean holding = p.getMainHandItem().is(ModItems.BANKAI_BLADE.get());

        // Flight from carrying the blade
        var ab = p.getAbilities();
        if (has) {
            if (!ab.mayfly || Math.abs(ab.getFlyingSpeed() - FLY_SPEED) > 1e-3f) {
                ab.mayfly = true;
                ab.setFlyingSpeed(FLY_SPEED);
                p.onUpdateAbilities();
            }
            if (!p.getTags().contains(FLIGHT_TAG)) p.addTag(FLIGHT_TAG);
        } else if (p.getTags().contains(FLIGHT_TAG)) {
            p.removeTag(FLIGHT_TAG);
            ab.setFlyingSpeed(0.05f);
            if (!p.isCreative()) { ab.mayfly = false; ab.flying = false; }
            p.onUpdateAbilities();
        }

        // Strength while holding
        if (holding && t % 20 == 0)
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 1, true, false, false));

        if (t % 3 != 0) return;

        // Form aura
        Form f = Form.of(p);
        if (f != null) {
            Fx.dust(l, p.getX(), p.getY() + 1, p.getZ(), f.r, f.g, f.b, 1.6f, 6, 0.45);
            if (f == Form.SSJ2) l.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 1, p.getZ(), 2, 0.5, 0.8, 0.5, 0.2);
            if (f == Form.SS4) l.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 0.3, p.getZ(), 2, 0.4, 0.3, 0.4, 0.02);
            if (f == Form.ULTRA) l.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 2, 0.4, 0.8, 0.4, 0.02);
        }
        // Silver blade aura (holding) and flight trail
        if (holding) Fx.dust(l, p.getX(), p.getY() + 1, p.getZ(), 0.8f, 0.82f, 0.88f, 1.4f, 4, 0.5);
        if (has && ab.flying) {
            l.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.8, p.getZ(), 3, 0.3, 0.4, 0.3, 0.01);
            Fx.dust(l, p.getX(), p.getY() + 1, p.getZ(), 0.85f, 0.88f, 0.95f, 1.8f, 5, 0.5);
        }
    }

    // ------------------------------------------------------------ commands
    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("gogeta")
            .then(Commands.literal("form")
                .then(Commands.argument("name", StringArgumentType.word()).executes(ctx -> {
                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer p)) return 0;
                    Form f = Form.byId(StringArgumentType.getString(ctx, "name").toLowerCase());
                    if (f == null) {
                        p.sendSystemMessage(Component.literal("Forms: base, ssj, ssj2, blue, ss4, ultra").withStyle(ChatFormatting.YELLOW));
                        return 0;
                    }
                    Attacks.setForm(p, f);
                    return 1;
                })))
            .then(Commands.literal("status").executes(ctx -> {
                if (!(ctx.getSource().getEntity() instanceof ServerPlayer p)) return 0;
                Form f = Form.of(p);
                p.sendSystemMessage(Component.literal(f == null ? "Current form: None"
                    : "Current form: " + f.display + " (x" + Attacks.trim(f.multiplier()) + ")").withStyle(ChatFormatting.GOLD));
                return 1;
            }))
            .then(Commands.literal("clear").executes(ctx -> {
                if (!(ctx.getSource().getEntity() instanceof ServerPlayer p)) return 0;
                Attacks.setForm(p, null);
                return 1;
            }))
            .then(Commands.literal("attack")
                .then(Commands.argument("name", StringArgumentType.word()).executes(ctx -> {
                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer p)) return 0;
                    int a = switch (StringArgumentType.getString(ctx, "name").toLowerCase()) {
                        case "stardust" -> Attacks.A_STARDUST;
                        case "kamehameha" -> Attacks.A_KAME;
                        case "bigbang" -> Attacks.A_BIGBANG;
                        case "meteor" -> Attacks.A_METEOR;
                        default -> -1;
                    };
                    if (a < 0) {
                        p.sendSystemMessage(Component.literal("Attacks: stardust, kamehameha, bigbang, meteor").withStyle(ChatFormatting.YELLOW));
                        return 0;
                    }
                    Attacks.perform(p, a);
                    return 1;
                })))
            .then(Commands.literal("sword").requires(s -> s.hasPermission(2)).executes(ctx -> {
                if (!(ctx.getSource().getEntity() instanceof ServerPlayer p)) return 0;
                p.getInventory().add(new ItemStack(ModItems.BANKAI_BLADE.get()));
                return 1;
            })));
    }
}
