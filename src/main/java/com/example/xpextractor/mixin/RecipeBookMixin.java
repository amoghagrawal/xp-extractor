package com.example.xpextractor.mixin;

import com.example.xpextractor.XPExtractor;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(ServerPlayerEntity.class)
public class RecipeBookMixin {

    @Inject(method = "onSpawn", at = @At("RETURN"))
    private void unlockAllRecipesOnSpawn(CallbackInfo ci) {
        try {
            ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
            XPExtractor.LOGGER.info("Forcefully unlocking XP Extractor recipe for player: " + player.getName().getString());
            
            RecipeManager recipeManager = player.getServer().getRecipeManager();
            
            List<Identifier> recipeIds = new ArrayList<>();
            recipeIds.add(Identifier.of(XPExtractor.MOD_ID, "xp_extractor"));
            recipeIds.add(Identifier.of("minecraft", "xp_extractor"));
            
            List<RecipeEntry<?>> recipesToUnlock = new ArrayList<>();
            for (Identifier id : recipeIds) {
                recipeManager.get(id).ifPresent(recipesToUnlock::add);
            }
            
            if (!recipesToUnlock.isEmpty()) {
                player.unlockRecipes(recipesToUnlock);
                XPExtractor.LOGGER.info("Successfully unlocked " + recipesToUnlock.size() + " recipes for " + player.getName().getString());
            }
        } catch (Exception e) {
            XPExtractor.LOGGER.error("Failed to unlock recipes via mixin", e);
        }
    }
    
    @Inject(method = "copyFrom", at = @At("RETURN"))
    private void keepRecipesUnlockedOnCopy(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        
        player.getServer().execute(() -> {
            try {
                XPExtractor.LOGGER.info("Ensuring XP Extractor recipe remains unlocked after player data copy");
                
                RecipeManager recipeManager = player.getServer().getRecipeManager();
                
                List<Identifier> recipeIds = new ArrayList<>();
                recipeIds.add(Identifier.of(XPExtractor.MOD_ID, "xp_extractor"));
                recipeIds.add(Identifier.of("minecraft", "xp_extractor"));
                
                List<RecipeEntry<?>> recipesToUnlock = new ArrayList<>();
                for (Identifier id : recipeIds) {
                    recipeManager.get(id).ifPresent(recipesToUnlock::add);
                }
                
                if (!recipesToUnlock.isEmpty()) {
                    player.unlockRecipes(recipesToUnlock);
                }
            } catch (Exception e) {
                XPExtractor.LOGGER.error("Failed to keep recipes unlocked during player data copy", e);
            }
        });
    }
}