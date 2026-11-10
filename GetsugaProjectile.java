package dev.gogeta.ascension;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

/** Black-and-gold Getsuga projectile. Server-side simulation; particles are sent from the server. */
public class GetsugaProjectile extends Entity {
    public static final float DAMAGE = 800f;
    private static final int MAX_AGE = 80;

    private UUID ownerUUID;
    private Vec3 velocity = Vec3.ZERO;
    private int age = 0;

    public GetsugaProjectile(EntityType<? extends GetsugaProjectile> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public void setOwnerUUID(UUID id) { this.ownerUUID = id; }
    public void setVelocity(Vec3 v) { this.velocity = v; }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (++age > MAX_AGE) { discard(); return; }

        Player owner = ownerUUID != null ? sl.getPlayerByUUID(ownerUUID) : null;
        Vec3 from = position(), to = from.add(velocity);

        // trail
        for (int i = 0; i < 6; i++) {
            double x = from.x + (random.nextDouble() - 0.5) * 1.6, y = from.y + 0.4 + (random.nextDouble() - 0.5) * 1.6, z = from.z + (random.nextDouble() - 0.5) * 1.6;
            if (i % 2 == 0) Fx.dust(sl, x, y, z, 0.05f, 0.05f, 0.05f, 2.4f, 1, 0.0);
            else Fx.dust(sl, x, y, z, 1.0f, 0.78f, 0.1f, 2.0f, 1, 0.0);
        }

        Vec3 hitPos = null;
        BlockHitResult bhr = sl.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (bhr.getType() != HitResult.Type.MISS) hitPos = bhr.getLocation();
        EntityHitResult ehr = ProjectileUtil.getEntityHitResult(sl, this, from, hitPos != null ? hitPos : to,
            new AABB(from, to).inflate(1.0), e -> e instanceof LivingEntity && e != owner && e.isAlive());
        if (ehr != null) hitPos = ehr.getLocation();

        if (hitPos != null) {
            int radius = Math.min(10, GogetaConfig.MAX_RADIUS.get());
            ServerPlayer sp = owner instanceof ServerPlayer s ? s : null;
            Tasks.add(sl, new ExpandingBlast(sp, hitPos, radius, 8, DAMAGE, ExpandingBlast.Style.BLACK_GOLD, null, true));
            discard();
            return;
        }
        setPos(to);
    }
}
