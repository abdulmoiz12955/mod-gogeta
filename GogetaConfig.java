package dev.gogeta.ascension;

import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.ArrayList;
import java.util.List;

public final class GogetaConfig {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue TERRAIN_DESTRUCTION = B
        .comment("Set to false to stop all attacks and the Getsuga from destroying blocks (protects builds). Damage still happens.")
        .define("terrainDestruction", true);

    public static final ModConfigSpec.IntValue MAX_RADIUS = B
        .comment("Hard cap on any blast radius in blocks. Lower this on weak PCs.")
        .defineInRange("maxBlastRadius", 16, 1, 48);

    public static final ModConfigSpec.IntValue BLOCKS_PER_TICK = B
        .comment("Max blocks removed per tick by one blast (keeps the server from freezing).")
        .defineInRange("maxBlocksPerTick", 4000, 100, 50000);

    public static final List<ModConfigSpec.DoubleValue> MULTIPLIERS = new ArrayList<>();
    static {
        B.push("damageMultipliers");
        for (Form f : Form.values()) {
            MULTIPLIERS.add(B.defineInRange(f.id, f.defaultMultiplier, 0.0, 100000.0));
        }
        B.pop();
    }

    public static final ModConfigSpec SPEC = B.build();

    private GogetaConfig() {}
}
