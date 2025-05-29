package com.example.xpextractor.registry;

import com.example.xpextractor.XPExtractor;
import com.example.xpextractor.item.XPExtractorItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public class ModItems {
    public static final Item XP_EXTRACTOR = registerItem("xp_extractor",
            new XPExtractorItem(new Item.Settings()
                    .maxCount(1)
                    .fireproof()
                    .rarity(Rarity.UNCOMMON)));

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(XPExtractor.MOD_ID, name), item);
    }

    public static void registerModItems() {
        XPExtractor.LOGGER.info("Registering mod items for " + XPExtractor.MOD_ID);
        
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(XP_EXTRACTOR);
        });
        
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> {
            entries.add(XP_EXTRACTOR);
        });
    }
}