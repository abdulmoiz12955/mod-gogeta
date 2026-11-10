package dev.gogeta.ascension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class Fx {
    public static void dust(ServerLevel l, double x, double y, double z, float r, float g, float b, float scale, int count, double spread) {
        l.sendParticles(new DustParticleOptions(new Vector3f(r, g, b), scale), x, y, z, count, spread, spread, spread, 0.0);
    }

    public static void rainbow(ServerLevel l, double x, double y, double z, float hue, float scale, int count, double spread) {
        int rgb = Mth.hsvToRgb(hue - (float) Math.floor(hue), 1f, 1f);
        dust(l, x, y, z, (rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f, scale, count, spread);
    }

    /**
     * Removes blocks whose distance from c is in (inner, outer]. Skips air, unbreakable blocks (bedrock)
     * and blocks with block entities (chests etc., to protect storage). Blocks higher than c.y + yCut are kept (crater shape).
     * @return blocks removed
     */
    public static int carve(ServerLevel level, Vec3 c, double inner, double outer, double yCut, int budget) {
        if (!GogetaConfig.TERRAIN_DESTRUCTION.get()) return 0;
        int R = (int) Math.ceil(outer);
        int cx = Mth.floor(c.x), cy = Mth.floor(c.y), cz = Mth.floor(c.z);
        double in2 = inner * inner, out2 = outer * outer;
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState();
        int count = 0;
        for (int dy = -R; dy <= R; dy++) {
            if (dy > yCut) continue;
            for (int dx = -R; dx <= R; dx++) {
                for (int dz = -R; dz <= R; dz++) {
                    double d2 = dx * dx + dy * dy + dz * dz;
                    if (d2 > out2 || d2 <= in2) continue;
                    mp.set(cx + dx, cy + dy, cz + dz);
                    if (level.isOutsideBuildHeight(mp)) continue;
                    BlockState s = level.getBlockState(mp);
                    if (s.isAir()) continue;
                    if (s.getDestroySpeed(level, mp) < 0) continue;
                    if (level.getBlockEntity(mp) != null) continue;
                    level.setBlock(mp, air, 2);
                    if (++count >= budget) return count;
                }
            }
        }
        return count;
    }

    private Fx() {}
}
