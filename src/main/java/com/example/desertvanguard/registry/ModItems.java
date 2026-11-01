package com.example.desertvanguard.registry;

import com.example.desertvanguard.DesertVanguardMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DesertVanguardMod.MODID);

    public static final RegistryObject<Item> DESERT_VANGUARD_SPAWN_EGG =
            ITEMS.register("desert_vanguard_spawn_egg", () ->
                    new ForgeSpawnEggItem(ModEntities.DESERT_VANGUARD, 0xD8CFA8, 0xB5823A, new Item.Properties()));
}
