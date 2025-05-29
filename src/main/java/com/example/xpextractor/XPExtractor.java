package com.example.xpextractor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.xpextractor.config.ModConfig;
import com.example.xpextractor.fabric.JsonResourceHook;
import com.example.xpextractor.registry.ModItems;
import com.example.xpextractor.registry.ModRecipes;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementManager;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import com.mojang.brigadier.context.CommandContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class XPExtractor implements ModInitializer {
    public static final String MOD_ID = "xpextractor";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    @Override
    public void onInitialize() {
        LOGGER.info("Initializing XP Extractor mod");
        
        try {
            forceResetConfig();
            
            LOGGER.debug("Loading configuration...");
            ModConfig.loadConfig();
            LOGGER.debug("Configuration loaded successfully");
            
            LOGGER.debug("Registering mod content...");
            ModItems.registerModItems();
            LOGGER.info("XP Extractor items registered");
            
            ModRecipes.registerRecipes();
            LOGGER.info("XP Extractor recipes registered");
            
            JsonResourceHook.register();
            LOGGER.info("Registered JSON resource hook for recipe loading");
            
            registerCommands();
            
            ServerLifecycleEvents.SERVER_STARTED.register(server -> {
                LOGGER.info("Server started, checking recipe registration");
                
                checkRecipesThouroughly(server);
                
                ModRecipes.patchRecipesIntoManager(server.getRecipeManager(), server);
                
                forceUnlockRecipes(server);
            });
            
            ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
                if (success) {
                    LOGGER.info("Data pack reload completed successfully, re-applying recipe patches");
                    
                    ModRecipes.patchRecipesIntoManager(server.getRecipeManager(), server);
                }
            });
            
            LOGGER.info("XP Extractor initialization complete");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize XP Extractor mod!", e);
            LOGGER.error("The mod may not function correctly!");
        }
    }
    
    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                CommandManager.literal("xpextractor")
                    .then(CommandManager.literal("giveitem")
                        .executes(this::giveItemCommand)
                    )
                    .then(CommandManager.literal("help")
                        .executes(this::helpCommand)
                    )
            );
        });
        
        LOGGER.info("Registered XP Extractor commands");
    }
    
    private int helpCommand(CommandContext<ServerCommandSource> context) {
        try {
            ServerCommandSource source = context.getSource();
            source.sendFeedback(() -> Text.literal("§a§l[XP Extractor]§r §eCommands:"), false);
            source.sendFeedback(() -> Text.literal("§a/xpextractor giveitem§r - Give the XP Extractor item directly to the player"), false);
            source.sendFeedback(() -> Text.literal("§a/xpextractor help§r - Show this help message"), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing help command", e);
            return 0;
        }
    }
    
    private int giveItemCommand(CommandContext<ServerCommandSource> context) {
        try {
            ServerCommandSource source = context.getSource();
            if (source.getPlayer() != null) {
                ServerPlayerEntity player = source.getPlayer();
                
                ModRecipes.givePlayerItemDirectly(player);
                
                return 1;
            } else {
                source.sendError(Text.literal("This command must be run by a player"));
                return 0;
            }
        } catch (Exception e) {
            LOGGER.error("Error executing giveitem command", e);
            context.getSource().sendError(Text.literal("An error occurred while giving the item"));
            return 0;
        }
    }
    
    private void forceUnlockRecipes(MinecraftServer server) {
        if (server == null) {
            LOGGER.warn("Cannot force-unlock recipes: server is null");
            return;
        }
        
        try {
            LOGGER.info("Forcing recipe unlocks for all players...");
            
            Identifier[] advancementIds = {
                Identifier.of(MOD_ID, "recipes/misc/xp_extractor"),
                Identifier.of("minecraft", "recipes/misc/xp_extractor")
            };
            
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                PlayerAdvancementTracker tracker = player.getAdvancementTracker();
                
                RecipeManager recipeManager = server.getRecipeManager();
                
                Identifier[] recipeIds = {
                    Identifier.of(MOD_ID, "xp_extractor"),
                    Identifier.of("minecraft", "xp_extractor")
                };
                
                for (Identifier recipeId : recipeIds) {
                    Optional<RecipeEntry<?>> recipe = recipeManager.get(recipeId);
                    if (recipe.isPresent()) {
                        LOGGER.info("Unlocking recipe " + recipeId + " for player " + player.getName().getString());
                        player.unlockRecipes(java.util.List.of(recipe.get()));
                    } else {
                        LOGGER.warn("Could not find recipe: " + recipeId);
                    }
                }
                
                for (Identifier id : advancementIds) {
                    var advancementEntry = server.getAdvancementLoader().get(id);
                    if (advancementEntry != null) {
                        var progress = tracker.getProgress(advancementEntry);
                        if (!progress.isDone()) {
                            LOGGER.info("Granting advancement " + id + " for player " + player.getName().getString());
                            
                            for (String criterion : advancementEntry.value().criteria().keySet()) {
                                tracker.grantCriterion(advancementEntry, criterion);
                            }
                        }
                    } else {
                        LOGGER.warn("Could not find advancement: " + id);
                    }
                }
            }
            
            LOGGER.info("Forced recipe unlocks complete");
        } catch (Exception e) {
            LOGGER.error("Error while forcing recipe unlocks", e);
        }
    }
    
    private void checkRecipesThouroughly(MinecraftServer server) {
        try {
            LOGGER.info("Available recipes in recipe manager:");
            boolean foundOurRecipes = false;
            boolean foundMainRecipe = false;
            boolean foundRepairRecipe = false;
            boolean foundMinecraftNamespace = false;
            boolean foundModNamespace = false;
            Map<String, Integer> recipeNamespaces = new HashMap<>();
            
            for (var entry : server.getRecipeManager().listAllOfType(RecipeType.CRAFTING)) {
                Identifier recipeId = entry.id();
                LOGGER.info("Recipe ID: " + recipeId);
                
                String namespace = recipeId.getNamespace();
                recipeNamespaces.put(namespace, recipeNamespaces.getOrDefault(namespace, 0) + 1);
                
                if (recipeId.getNamespace().equals(MOD_ID)) {
                    foundModNamespace = true;
                    
                    if (recipeId.getPath().equals("xp_extractor")) {
                        LOGGER.info("✓ Found main XP Extractor recipe in mod namespace: " + recipeId);
                        foundMainRecipe = true;
                        foundOurRecipes = true;
                    } 
                    else if (recipeId.getPath().equals("repair_xp_extractor")) {
                        LOGGER.info("✓ Found repair XP Extractor recipe: " + recipeId);
                        foundRepairRecipe = true;
                        foundOurRecipes = true;
                    }
                }
                else if (recipeId.getNamespace().equals("minecraft")) {
                    if (recipeId.getPath().equals("xp_extractor")) {
                        LOGGER.info("✓ Found XP Extractor recipe in minecraft namespace: " + recipeId);
                        foundMinecraftNamespace = true;
                        foundOurRecipes = true;
                    }
                }
            }
            
            LOGGER.info("Recipe namespace statistics:");
            for (Map.Entry<String, Integer> entry : recipeNamespaces.entrySet()) {
                LOGGER.info("- Namespace: " + entry.getKey() + ", Count: " + entry.getValue());
            }
            
            if (!foundOurRecipes) {
                LOGGER.warn("⚠️ No XP Extractor recipes found in recipe manager!");
                LOGGER.warn("Diagnosis:");
                LOGGER.warn("- Main recipe found: " + foundMainRecipe);
                LOGGER.warn("- Repair recipe found: " + foundRepairRecipe);
                LOGGER.warn("- Found in mod namespace: " + foundModNamespace);
                LOGGER.warn("- Found in minecraft namespace: " + foundMinecraftNamespace);
                
                checkJsonExists("xpextractor:recipes/xp_extractor");
                checkJsonExists("minecraft:recipes/xp_extractor");
                
                LOGGER.warn("Please check your game logs for any errors related to recipe loading.");
                LOGGER.warn("You may need to manually reload game resources with F3+T or the /reload command.");
            } else {
                LOGGER.info("✓ XP Extractor recipes found in recipe manager!");
                LOGGER.info("Recipe status:");
                LOGGER.info("- Main recipe found: " + foundMainRecipe);
                LOGGER.info("- Repair recipe found: " + foundRepairRecipe);
                LOGGER.info("- Found in mod namespace: " + foundModNamespace);
                LOGGER.info("- Found in minecraft namespace: " + foundMinecraftNamespace);
            }
        } catch (Exception e) {
            LOGGER.error("Error checking recipes on server start:", e);
        }
    }
    
    private void checkJsonExists(String idString) {
        try {
            String namespace, path;
            if (idString.contains(":")) {
                String[] parts = idString.split(":", 2);
                namespace = parts[0];
                path = parts[1];
            } else {
                namespace = "minecraft";
                path = idString;
            }
            
            Identifier id = Identifier.of(namespace, path);
            boolean itemExists = Registries.ITEM.containsId(Identifier.of(MOD_ID, "xp_extractor"));
            LOGGER.info("XP Extractor item registered: " + itemExists);
            
            String resourcePath = "data/" + id.getNamespace() + "/" + id.getPath().replace("recipes/", "recipe/") + ".json";
            if (getClass().getClassLoader().getResource(resourcePath) != null) {
                LOGGER.info("✓ Recipe JSON file exists at: " + resourcePath);
            } else {
                LOGGER.warn("❌ Recipe JSON file missing at: " + resourcePath);
                
                String altPath = resourcePath.replace("recipe/", "recipes/");
                if (getClass().getClassLoader().getResource(altPath) != null) {
                    LOGGER.warn("⚠ Found recipe at old path structure: " + altPath);
                    LOGGER.warn("Update paths to use 'recipe/' instead of 'recipes/' for Minecraft 1.21.1");
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error checking JSON existence: " + e.getMessage());
        }
    }
    
    private void forceResetConfig() {
        try {
            Path configPath = FabricLoader.getInstance().getConfigDir().resolve("xpextractor.json");
            if (Files.exists(configPath)) {
                LOGGER.info("Deleting old config file to apply new defaults");
                Files.delete(configPath);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to delete old config file", e);
        }
    }
    
    public static ModConfig getConfig() {
        try {
            return ModConfig.getInstance();
        } catch (Exception e) {
            LOGGER.error("Failed to get mod configuration!", e);
            return null;
        }
    }
}