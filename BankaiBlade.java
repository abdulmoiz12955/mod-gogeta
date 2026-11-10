package dev.gogeta.ascension;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BankaiBlade extends Item {
    public static final int CHARGE_TICKS = 100; // 5 seconds
    private static final int MAX_USE = 72000;

    public BankaiBlade(Properties props) { super(props); }

    /** Cuts every breakable block almost instantly (bedrock-type blocks stay unbreakable). */
    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return state.getDestroySpeed(EmptyBlockGetter.INSTANCE, BlockPos.ZERO) < 0 ? 1.0f : 1000.0f;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) { return true; }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }

    @Override
    public int getUseDuration(ItemStack stack) { return MAX_USE; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!(level instanceof ServerLevel sl)) return;
        int charged = MAX_USE - remaining;
        Vec3 p = entity.position().add(0, 1.0, 0);
        boolean ready = charged >= CHARGE_TICKS;
        if (charged % 2 == 0) {
            float pr = Math.min(1f, charged / (float) CHARGE_TICKS);
            for (int i = 0; i < 4; i++) {
                double a = level.random.nextDouble() * Math.PI * 2;
                double rad = 2.5 * (1.0 - pr) + 0.6;
                double x = p.x + Math.cos(a) * rad, z = p.z + Math.sin(a) * rad, y = p.y + level.random.nextDouble() * 1.8 - 0.9;
                if (ready) Fx.dust(sl, x, y, z, 1.0f, 0.78f, 0.1f, 1.5f, 1, 0.05);
                else Fx.dust(sl, x, y, z, 0.85f, 0.88f, 0.95f, 1.2f, 1, 0.05);
            }
        }
        if (charged == CHARGE_TICKS) {
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5f, 0.6f);
            if (entity instanceof Player pl) pl.displayClientMessage(Component.literal("Getsuga charged - release!"), true);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(level instanceof ServerLevel sl) || !(entity instanceof Player player)) return;
        int charged = MAX_USE - timeLeft;
        if (charged < CHARGE_TICKS) return; // not charged: nothing fires
        Vec3 look = player.getLookAngle();
        GetsugaProjectile proj = new GetsugaProjectile(ModEntities.GETSUGA.get(), sl);
        proj.setOwnerUUID(player.getUUID());
        proj.setPos(player.getEyePosition().add(look.scale(1.5)));
        proj.setVelocity(look.scale(2.0));
        sl.addFreshEntity(proj);
        sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 2f, 0.5f);
        player.getCooldowns().addCooldown(this, 40);
    }
}
