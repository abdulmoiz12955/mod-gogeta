package dev.gogeta.ascension;

import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/** The six transformation states. Stored as player tags "gogeta_form_<id>" (same as the original ZIP). */
public enum Form {
    BASE("base", "Base Gogeta", 1.0, 3, 0.85f, 0.92f, 1.00f),
    SSJ("ssj", "Super Saiyan", 3.0, 4, 1.00f, 0.85f, 0.10f),
    SSJ2("ssj2", "Super Saiyan 2", 7.0, 6, 1.00f, 0.92f, 0.25f),
    BLUE("blue", "Super Saiyan Blue", 20.0, 9, 0.10f, 0.55f, 1.00f),
    SS4("ss4", "Super Saiyan 4", 30.0, 12, 0.90f, 0.10f, 0.10f),
    ULTRA("ultra", "Ultra Instinct-inspired", 75.0, 16, 0.85f, 0.90f, 1.00f);

    public static final String TAG_PREFIX = "gogeta_form_";

    public final String id;
    public final String display;
    public final double defaultMultiplier;
    public final int baseBlastRadius;
    public final float r, g, b;

    Form(String id, String display, double mult, int radius, float r, float g, float b) {
        this.id = id; this.display = display; this.defaultMultiplier = mult;
        this.baseBlastRadius = radius; this.r = r; this.g = g; this.b = b;
    }

    public double multiplier() { return GogetaConfig.MULTIPLIERS.get(ordinal()).get(); }

    public int blastRadius() { return Math.min(baseBlastRadius, GogetaConfig.MAX_RADIUS.get()); }

    @Nullable
    public static Form byId(String id) {
        for (Form f : values()) if (f.id.equals(id)) return f;
        return null;
    }

    @Nullable
    public static Form of(Player p) {
        Form found = null;
        for (Form f : values()) if (p.getTags().contains(TAG_PREFIX + f.id)) found = f;
        return found;
    }

    public static void set(Player p, @Nullable Form form) {
        for (Form f : values()) p.removeTag(TAG_PREFIX + f.id);
        if (form != null) p.addTag(TAG_PREFIX + form.id);
    }
}
