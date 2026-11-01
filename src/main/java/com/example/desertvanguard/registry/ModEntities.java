package com.example.desertvanguard.registry;

import com.example.desertvanguard.DesertVanguardMod;
import com.example.desertvanguard.entity.DesertVanguardEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, DesertVanguardMod.MODID);

    public static final RegistryObject<EntityType<DesertVanguardEntity>> DESERT_VANGUARD =
            ENTITIES.register("desert_vanguard", () ->
                    EntityType.Builder.<DesertVanguardEntity>of(DesertVanguardEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 2.0F)
                            .clientTrackingRange(8)
                            .build(DesertVanguardMod.MODID + ":desert_vanguard"));
}
