package dev.gogeta.ascension;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, GogetaAscension.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<GetsugaProjectile>> GETSUGA = ENTITIES.register("getsuga_projectile",
        () -> EntityType.Builder.<GetsugaProjectile>of(GetsugaProjectile::new, MobCategory.MISC)
            .sized(0.8f, 0.8f).clientTrackingRange(10).updateInterval(1).noSave()
            .build("getsuga_projectile"));

    private ModEntities() {}
}
