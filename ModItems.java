package dev.gogeta.ascension;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GogetaAscension.MOD_ID);

    /** Player base attack damage is 1, so +79 gives 80 damage per normal hit. */
    public static final DeferredItem<BankaiBlade> BANKAI_BLADE = ITEMS.register("bankai_blade",
        () -> new BankaiBlade(new Item.Properties()
            .stacksTo(1).rarity(Rarity.EPIC).fireResistant()
            .attributes(ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(ResourceLocation.fromNamespaceAndPath(GogetaAscension.MOD_ID, "bankai_damage"), 79.0, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                    new AttributeModifier(ResourceLocation.fromNamespaceAndPath(GogetaAscension.MOD_ID, "bankai_speed"), -2.4, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND)
                .build())));

    private ModItems() {}
}
