package dev.gogeta.ascension;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Attacks {
    public static final int A_NEXT = 0, A_PREV = 1, A_STARDUST = 2, A_KAME = 3, A_BIGBANG = 4, A_METEOR = 5;

    // base damage per attack (multiplied by the form multiplier)
    private static final float STARDUST_DMG = 60f, KAME_DMG = 30f, BIGBANG_DMG = 50f, METEOR_HIT = 6f, METEOR_FINISH = 25f;

    private static final Map<String, Long> COOLDOWNS = new HashMap<>();

    public static void perform(ServerPlayer p, int action) {
        switch (action) {
            case A_NEXT -> cycle(p, 1);
            case A_PREV -> cycle(p, -1);
            case A_STARDUST -> stardust(p);
            case A_KAME -> beam(p, false);
            case A_BIGBANG -> beam(p, true);
            case A_METEOR -> meteor(p);
            default -> {}
        }
    }

    // ---------------------------------------------------------------- forms
    public static void cycle(ServerPlayer p, int dir) {
        Form cur = Form.of(p);
        Form[] v = Form.values();
        int idx = (cur == null ? -1 : cur.ordinal()) + dir;
        if (idx >= v.length) idx = -1;
        if (idx < -1) idx = v.length - 1;
        setForm(p, idx < 0 ? null : v[idx]);
    }

    public static void setForm(ServerPlayer p, Form f) {
        Form.set(p, f);
        ServerLevel l = p.serverLevel();
        if (f == null) {
            p.displayClientMessage(Component.literal("Transformation cleared."), true);
            return;
        }
        p.displayClientMessage(Component.literal(f.display + "  (x" + trim(f.multiplier()) + " melee damage)"), true);
        l.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.6f, 1.4f);
        for (int i = 0; i < 40; i++) {
            double a = i / 40.0 * Math.PI * 2;
            Fx.dust(l, p.getX() + Math.cos(a) * 1.5, p.getY() + 0.1, p.getZ() + Math.sin(a) * 1.5, f.r, f.g, f.b, 2f, 3, 0.1);
        }
        l.sendParticles(ParticleTypes.FLASH, p.getX(), p.getY() + 1, p.getZ(), 1, 0, 0, 0, 0);
    }

    public static String trim(double d) { return d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d); }

    // ---------------------------------------------------------------- helpers
    private static boolean ready(ServerPlayer p, String id, int cd) {
        long now = p.serverLevel().getGameTime();
        String key = p.getUUID() + id;
        Long until = COOLDOWNS.get(key);
        if (until != null && now < until) {
            p.displayClientMessage(Component.literal("Recharging... " + ((until - now) / 20 + 1) + "s"), true);
            return false;
        }
        COOLDOWNS.put(key, now + cd);
        return true;
    }

    private static Form need(ServerPlayer p) {
        Form f = Form.of(p);
        if (f == null) p.displayClientMessage(Component.literal("Transform first (G key or /gogeta form <name>)"), true);
        return f;
    }

    private static DamageSource src(ServerLevel l, ServerPlayer p) { return l.damageSources().indirectMagic(p, p); }

    private static void hurt(ServerLevel l, ServerPlayer p, LivingEntity e, float dmg) {
        if (e instanceof net.minecraft.world.entity.player.Player && !l.getServer().isPvpAllowed()) return;
        e.invulnerableTime = 0;
        e.hurt(src(l, p), dmg);
    }

    // ---------------------------------------------------------------- Stardust Breaker
    private static void stardust(ServerPlayer p) {
        Form f = need(p);
        if (f == null || !ready(p, "stardust", 240)) return;
        ServerLevel l = p.serverLevel();
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        BlockHitResult hit = l.clip(new ClipContext(eye, eye.add(look.scale(80)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 target = hit.getLocation();
        double radius = Math.min(f.blastRadius() * 1.5, GogetaConfig.MAX_RADIUS.get());
        float dmg = (float) (STARDUST_DMG * f.multiplier());
        Vec3 ballStart = eye.add(0, 1.5, 0).add(look.scale(1.0));

        Tasks.add(l, new Tasks.Task() {
            int t = 0; Vec3 pos = ballStart;
            public boolean tick(ServerLevel level) {
                t++;
                if (t <= 20) { // charge: ball grows above the player
                    pos = p.getEyePosition().add(0, 1.5, 0);
                    float size = 1f + t * 0.1f;
                    for (int i = 0; i < 8; i++)
                        Fx.rainbow(level, pos.x, pos.y, pos.z, level.random.nextFloat(), size, 1, 0.4 + t * 0.03);
                    if (t == 1) level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 2f, 1.2f);
                    return false;
                }
                Vec3 to = target.subtract(pos);
                double dist = to.length();
                if (dist <= 2.5) {
                    Tasks.add(level, new ExpandingBlast(p, target, radius, 24, dmg, ExpandingBlast.Style.RAINBOW, null, false));
                    return true;
                }
                pos = pos.add(to.scale(2.5 / dist));
                for (int i = 0; i < 10; i++)
                    Fx.rainbow(level, pos.x, pos.y, pos.z, level.random.nextFloat(), 2.5f, 1, 0.6);
                return t > 200;
            }
        });
    }

    // ---------------------------------------------------------------- Kamehameha / Big Bang Kamehameha
    private static void beam(ServerPlayer p, boolean big) {
        Form f = need(p);
        if (f == null || !ready(p, big ? "bigbang" : "kame", big ? 200 : 80)) return;
        ServerLevel l = p.serverLevel();
        Vec3 dir = p.getLookAngle().normalize();
        Vec3 origin = p.getEyePosition().add(dir.scale(1.5)).add(0, -0.3, 0);
        double maxLen = big ? 64 : 48;
        double width = big ? 2.5 + f.ordinal() * 0.3 : 1.2 + f.ordinal() * 0.15;
        float dmg = (float) ((big ? BIGBANG_DMG : KAME_DMG) * f.multiplier());
        double blastR = Math.max(3, f.blastRadius() * (big ? 1.0 : 0.6));
        float[] col = big ? new float[]{0.55f, 0.8f, 1.0f} : new float[]{0.3f, 0.65f, 1.0f};

        Tasks.add(l, new Tasks.Task() {
            double dist = 0; final Set<UUID> hitSet = new HashSet<>();
            public boolean tick(ServerLevel level) {
                if (dist == 0) level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2f, big ? 0.6f : 0.9f);
                for (int i = 0; i < 4; i++) {
                    dist += 1.0;
                    Vec3 pos = origin.add(dir.scale(dist));
                    Fx.dust(level, pos.x, pos.y, pos.z, col[0], col[1], col[2], (float) Math.min(4.0, width + 1), 4, width * 0.4);
                    Fx.dust(level, pos.x, pos.y, pos.z, 1f, 1f, 1f, (float) Math.min(3.0, width), 2, width * 0.15);
                    boolean stop = dist >= maxLen;
                    BlockPos bp = BlockPos.containing(pos);
                    BlockState bs = level.getBlockState(bp);
                    if (!bs.getCollisionShape(level, bp).isEmpty()) stop = true;
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(width))) {
                        if (e == p || hitSet.contains(e.getUUID())) continue;
                        hitSet.add(e.getUUID());
                        hurt(level, p, e, dmg);
                        stop = true;
                    }
                    if (stop) {
                        Tasks.add(level, new ExpandingBlast(p, pos, blastR, big ? 10 : 6, dmg * 0.5f, ExpandingBlast.Style.SOLID, col, false));
                        return true;
                    }
                }
                return false;
            }
        });
    }

    // ---------------------------------------------------------------- Meteor Combination
    private static void meteor(ServerPlayer p) {
        Form f = need(p);
        if (f == null) return;
        ServerLevel l = p.serverLevel();
        Vec3 eye = p.getEyePosition(), end = eye.add(p.getLookAngle().scale(24));
        EntityHitResult r = ProjectileUtil.getEntityHitResult(l, p, eye, end, new AABB(eye, end).inflate(1.5),
            e -> e instanceof LivingEntity && e != p && e.isAlive());
        if (r == null) { p.displayClientMessage(Component.literal("Aim at a target within 24 blocks."), true); return; }
        if (!ready(p, "meteor", 160)) return;
        LivingEntity target = (LivingEntity) r.getEntity();
        float hitDmg = (float) (METEOR_HIT * f.multiplier()), finDmg = (float) (METEOR_FINISH * f.multiplier());
        double finR = Math.max(3, f.blastRadius() * 0.7);

        Tasks.add(l, new Tasks.Task() {
            int t = 0;
            public boolean tick(ServerLevel level) {
                t++;
                if (!target.isAlive() && t < 26) t = 26;
                if (t <= 24 && t % 2 == 0) {
                    double a = level.random.nextDouble() * Math.PI * 2;
                    Vec3 tp = target.position();
                    p.teleportTo(tp.x + Math.cos(a) * 2.0, tp.y, tp.z + Math.sin(a) * 2.0);
                    target.setDeltaMovement(Vec3.ZERO);
                    target.hurtMarked = true;
                    hurt(level, p, target, hitDmg);
                    level.sendParticles(ParticleTypes.CRIT, tp.x, tp.y + 1, tp.z, 20, 0.5, 0.5, 0.5, 0.3);
                    Fx.dust(level, tp.x, tp.y + 1, tp.z, f.r, f.g, f.b, 2f, 10, 0.5);
                    level.playSound(null, tp.x, tp.y, tp.z, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.5f, 0.8f + level.random.nextFloat() * 0.4f);
                }
                if (t == 26) {
                    Vec3 tp = target.position();
                    Tasks.add(level, new ExpandingBlast(p, tp, finR, 8, finDmg, ExpandingBlast.Style.SOLID, new float[]{f.r, f.g, f.b}, true));
                    return true;
                }
                return false;
            }
        });
    }

    private Attacks() {}
}
