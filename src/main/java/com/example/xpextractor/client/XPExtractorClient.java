package com.example.xpextractor.client;

import com.example.xpextractor.XPExtractor;
import com.example.xpextractor.registry.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class XPExtractorClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        XPExtractor.LOGGER.info("Initializing XP Extractor Client!");
        
        ModelPredicateProviderRegistry.register(
            ModItems.XP_EXTRACTOR,
            Identifier.of("minecraft", "angle"),
            (stack, world, entity, seed) -> {
                return ModelPredicateProviderRegistry.get(
                    new ItemStack(Items.COMPASS), 
                    Identifier.of("minecraft", "angle")
                ).call(stack, world, entity, seed);
            }
        );
    }
} 