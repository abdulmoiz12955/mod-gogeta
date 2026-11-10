package dev.gogeta.ascension;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** A sphere that grows over several ticks, removing terrain on its shell and damaging everything it touches once. */
public final class ExpandingBlast implements Tasks.Task {
    public enum Style { RAINBOW, SOLID, BLACK_GOLD }

    private final ServerPlayer owner;
    private final Vec3 c;
    private final double radius;
    private final int ticks;
    private final float damage;
    private final Style style;
    private final float[] rgb;
    private final boolean crater;
    private final Set<UUID> hit = new HashSet<>();
    private int t = 0;

    public ExpandingBlast(ServerPlayer owner, Vec3 c, double radius, int ticks, float damage, Style style, float[] rgb, boolean crater) {
        this.owner = owner; this.c = c; this.radius = radius; this.ticks = Math.max(1, ticks);
        this.damage = damage; this.style = style; this.rgb = rgb; this.crater = crater;
    }

    @Override
    public boolean tick(ServerLevel level) {
        t++;
        double r = radius * t / ticks, prev = radius * (t - 1) / ticks;
        if (t == 1) level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 4f, 0.6f);

        // visuals: random points on the shell
        int pts = Math.min(60, (int) (r * 6) + 10);
        for (int i = 0; i < pts; i++) {
            double x = level.random.nextGaussian(), y = level.random.nextGaussian(), z = level.random.nextGaussian();
            double len = Math.sqrt(x * x + y * y + z * z);
            if (len < 1e-4) continue;
            x = c.x + x / len * r; y = c.y + y / len * r; z = c.z + z / len * r;
            switch (style) {
                case RAINBOW -> Fx.rainbow(level, x, y, z, level.random.nextFloat(), 2.0f, 1, 0.1);
                case SOLID -> Fx.dust(level, x, y, z, rgb[0], rgb[1], rgb[2], 2.0f, 1, 0.1);
                case BLACK_GOLD -> {
                    if (level.random.nextBoolean()) Fx.dust(level, x, y, z, 0.05f, 0.05f, 0.05f, 2.2f, 1, 0.1);
                    else Fx.dust(level, x, y, z, 1.0f, 0.78f, 0.1f, 2.0f, 1, 0.1);
                }
            }
        }

        // damage (each entity once)
        DamageSource src = owner != null ? level.damageSources().indirectMagic(owner, owner) : level.damageSources().magic();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r))) {
            if (e == owner || hit.contains(e.getUUID()) || e.distanceToSqr(c) > r * r) continue;
            if (e instanceof Player && !level.getServer().isPvpAllowed()) continue;
            hit.add(e.getUUID());
            e.invulnerableTime = 0;
            e.hurt(src, damage);
            Vec3 d = e.position().subtract(c);
            if (d.lengthSqr() > 1e-6) {
                d = d.normalize().scale(1.2);
                e.setDeltaMovement(e.getDeltaMovement().add(d.x, 0.6, d.z));
                e.hurtMarked = true;
            }
        }

        // terrain
        double yCut = crater ? radius * 0.35 : Double.MAX_VALUE;
        Fx.carve(level, c, prev, r, yCut, GogetaConfig.BLOCKS_PER_TICK.get());

        if (t >= ticks) {
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 2, radius * 0.3, radius * 0.2, radius * 0.3, 0.0);
            level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 5f, 0.5f);
            return true;
        }
        return false;
    }
}
