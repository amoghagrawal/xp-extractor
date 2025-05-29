package com.example.xpextractor.registry;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.example.xpextractor.XPExtractor;

import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class ModRecipes {
    public static final TagKey<Item> XP_BOTTLE_TAG = TagKey.of(RegistryKeys.ITEM, Identifier.of(XPExtractor.MOD_ID, "experience_containers"));
    
    public static void registerRecipes() {
        XPExtractor.LOGGER.info("Registering XP Extractor recipe system");
        
        createItemTagsFiles();
        
        createRecipeFiles();
        
        registerResourceReloadListener();
    }
    
    private static void registerResourceReloadListener() {
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(
            new SimpleSynchronousResourceReloadListener() {
                @Override
                public Identifier getFabricId() {
                    return Identifier.of(XPExtractor.MOD_ID, "recipes");
                }
                
                @Override
                public void reload(ResourceManager manager) {
                    XPExtractor.LOGGER.info("Reloading XP Extractor recipes");
                    
                    checkResourcesExist(manager, "data/xpextractor/recipe/xp_extractor.json");
                    checkResourcesExist(manager, "data/minecraft/recipe/xp_extractor.json");
                    
                    checkResourcesExist(manager, "data/xpextractor/tags/items/experience_containers.json");
                    checkResourcesExist(manager, "data/minecraft/tags/items/experience_containers.json");
                    
                    checkResourcesExist(manager, "data/xpextractor/advancements/recipes/misc/xp_extractor.json");
                    checkResourcesExist(manager, "data/minecraft/advancements/recipes/misc/xp_extractor.json");
                    
                    XPExtractor.LOGGER.info("XP Extractor recipe reload complete");
                }
                
                private void checkResourcesExist(ResourceManager manager, String path) {
                    Identifier id;
                    if (path.contains(":")) {
                        String[] parts = path.split(":", 2);
                        id = Identifier.of(parts[0], parts[1]);
                    } else {
                        id = Identifier.of("minecraft", path);
                    }
                    var resources = manager.getAllResources(id);
                    
                    if (resources.isEmpty()) {
                        XPExtractor.LOGGER.warn("Resource not found: " + path);
                    } else {
                        XPExtractor.LOGGER.info("Resource found: " + path + " (" + resources.size() + " entries)");
                        
                        try {
                            String content = resources.get(0).getReader().lines().collect(Collectors.joining("\n"));
                            if (content.length() < 500) {
                                XPExtractor.LOGGER.info("Content: " + content);
                            } else {
                                XPExtractor.LOGGER.info("Content too large to log fully (" + content.length() + " chars)");
                            }
                        } catch (Exception e) {
                            XPExtractor.LOGGER.error("Failed to read resource content", e);
                        }
                    }
                }
            }
        );
    }
    
    private static void createItemTagsFiles() {
        try {
            XPExtractor.LOGGER.info("Creating item tags for XP Extractor recipes");
            
            Path resourceDir = Path.of("src/main/resources").toAbsolutePath();
            Path modTagsDir = resourceDir.resolve("data/xpextractor/tags/items");
            Path minecraftTagsDir = resourceDir.resolve("data/minecraft/tags/items");
            
            Files.createDirectories(modTagsDir);
            Files.createDirectories(minecraftTagsDir);
            
            Path xpTagPath = modTagsDir.resolve("experience_containers.json");
            if (!Files.exists(xpTagPath)) {
                String tagJson = "{\n" +
                    "  \"replace\": false,\n" +
                    "  \"values\": [\n" +
                    "    \"minecraft:experience_bottle\"\n" +
                    "  ]\n" +
                    "}";
                
                Files.writeString(xpTagPath, tagJson);
                XPExtractor.LOGGER.info("Created experience_containers tag at: " + xpTagPath);
            }
            
            Path minecraftXpTagPath = minecraftTagsDir.resolve("experience_containers.json");
            if (!Files.exists(minecraftXpTagPath)) {
                String tagJson = "{\n" +
                    "  \"replace\": false,\n" +
                    "  \"values\": [\n" +
                    "    \"minecraft:experience_bottle\"\n" +
                    "  ]\n" +
                    "}";
                
                Files.writeString(minecraftXpTagPath, tagJson);
                XPExtractor.LOGGER.info("Created minecraft experience_containers tag at: " + minecraftXpTagPath);
            }
            
            Path navigationToolsPath = minecraftTagsDir.resolve("navigation_tools.json");
            if (!Files.exists(navigationToolsPath)) {
                String tagJson = "{\n" +
                    "  \"replace\": false,\n" +
                    "  \"values\": [\n" +
                    "    \"minecraft:compass\",\n" +
                    "    \"minecraft:recovery_compass\",\n" +
                    "    \"minecraft:map\",\n" +
                    "    \"minecraft:filled_map\"\n" +
                    "  ]\n" +
                    "}";
                
                Files.writeString(navigationToolsPath, tagJson);
                XPExtractor.LOGGER.info("Created navigation_tools tag at: " + navigationToolsPath);
            }
            
            XPExtractor.LOGGER.info("Item tags creation complete");
        } catch (Exception e) {
            XPExtractor.LOGGER.error("Failed to create item tags", e);
        }
    }
    
    private static void createRecipeFiles() {
        try {
            XPExtractor.LOGGER.info("Creating proper recipe files for 1.21.1...");
            
            Path resourceDir = Path.of("src/main/resources").toAbsolutePath();
            Path recipesDir = resourceDir.resolve("data/xpextractor/recipe");
            Files.createDirectories(recipesDir);
            
            Path mainRecipePath = recipesDir.resolve("xp_extractor.json");
            String mainRecipeJson = "{\n" +
                "  \"type\": \"minecraft:crafting_shaped\",\n" +
                "  \"pattern\": [\n" +
                "    \"EEE\",\n" +
                "    \"ELE\",\n" +
                "    \"EEE\"\n" +
                "  ],\n" +
                "  \"key\": {\n" +
                "    \"E\": {\n" +
                "      \"item\": \"minecraft:experience_bottle\"\n" +
                "    },\n" +
                "    \"L\": {\n" +
                "      \"item\": \"minecraft:lodestone\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"result\": {\n" +
                "    \"id\": \"xpextractor:xp_extractor\",\n" +
                "    \"count\": 1\n" +
                "  },\n" +
                "  \"category\": \"misc\",\n" +
                "  \"show_notification\": true,\n" +
                "  \"group\": \"xp_tools\"\n" +
                "}";
            
            Files.writeString(mainRecipePath, mainRecipeJson);
            XPExtractor.LOGGER.info("Created main recipe file at: " + mainRecipePath);
            
            Path minecraftRecipesDir = resourceDir.resolve("data/minecraft/recipe");
            Files.createDirectories(minecraftRecipesDir);
            
            Path minecraftMainRecipePath = minecraftRecipesDir.resolve("xp_extractor.json");
            Files.writeString(minecraftMainRecipePath, mainRecipeJson);
            XPExtractor.LOGGER.info("Created main recipe in minecraft namespace at: " + minecraftMainRecipePath);
            
            createAdvancementFiles(resourceDir);
            
            XPExtractor.LOGGER.info("Recipe files creation complete");
        } catch (Exception e) {
            XPExtractor.LOGGER.error("Failed to create recipe files", e);
        }
    }
    
    private static void createAdvancementFiles(Path resourceDir) {
        try {
            XPExtractor.LOGGER.info("Creating advancement files for recipe discovery");
            
            Path advancementsDir = resourceDir.resolve("data/xpextractor/advancements/recipes/misc");
            Files.createDirectories(advancementsDir);
            
            Path minecraftAdvancementsDir = resourceDir.resolve("data/minecraft/advancements/recipes/misc");
            Files.createDirectories(minecraftAdvancementsDir);
            
            Path advancementPath = advancementsDir.resolve("xp_extractor.json");
            String advancementJson = "{\n" +
                "  \"parent\": \"minecraft:recipes/root\",\n" +
                "  \"rewards\": {\n" +
                "    \"recipes\": [\n" +
                "      \"xpextractor:xp_extractor\",\n" +
                "      \"xpextractor:repair_xp_extractor\"\n" +
                "    ]\n" +
                "  },\n" +
                "  \"criteria\": {\n" +
                "    \"has_xp_bottle\": {\n" +
                "      \"trigger\": \"minecraft:inventory_changed\",\n" +
                "      \"conditions\": {\n" +
                "        \"items\": [\n" +
                "          {\n" +
                "            \"items\": [\"minecraft:experience_bottle\"]\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    },\n" +
                "    \"automatic\": {\n" +
                "      \"trigger\": \"minecraft:tick\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"requirements\": [\n" +
                "    [\n" +
                "      \"has_xp_bottle\",\n" +
                "      \"automatic\"\n" +
                "    ]\n" +
                "  ]\n" +
                "}";
            
            Files.writeString(advancementPath, advancementJson);
            XPExtractor.LOGGER.info("Created advancement file at: " + advancementPath);
            
            Path autoAdvancementPath = advancementsDir.resolve("xp_extractor_auto.json");
            String autoAdvancementJson = "{\n" +
                "  \"parent\": \"minecraft:recipes/root\",\n" +
                "  \"rewards\": {\n" +
                "    \"recipes\": [\n" +
                "      \"xpextractor:xp_extractor\",\n" +
                "      \"xpextractor:repair_xp_extractor\",\n" +
                "      \"minecraft:xp_extractor\"\n" +
                "    ]\n" +
                "  },\n" +
                "  \"criteria\": {\n" +
                "    \"tick\": {\n" +
                "      \"trigger\": \"minecraft:tick\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"requirements\": [\n" +
                "    [\"tick\"]\n" +
                "  ]\n" +
                "}";
            
            Files.writeString(autoAdvancementPath, autoAdvancementJson);
            XPExtractor.LOGGER.info("Created auto-trigger advancement at: " + autoAdvancementPath);
            
            Path minecraftAdvancementPath = minecraftAdvancementsDir.resolve("xp_extractor.json");
            String minecraftAdvancementJson = "{\n" +
                "  \"parent\": \"minecraft:recipes/root\",\n" +
                "  \"rewards\": {\n" +
                "    \"recipes\": [\n" +
                "      \"minecraft:xp_extractor\",\n" +
                "      \"minecraft:xp_extractor_alternative\",\n" +
                "      \"xpextractor:xp_extractor\",\n" +
                "      \"xpextractor:xp_extractor_alternative\",\n" +
                "      \"xpextractor:repair_xp_extractor\"\n" +
                "    ]\n" +
                "  },\n" +
                "  \"criteria\": {\n" +
                "    \"has_xp_bottle\": {\n" +
                "      \"trigger\": \"minecraft:inventory_changed\",\n" +
                "      \"conditions\": {\n" +
                "        \"items\": [\n" +
                "          {\n" +
                "            \"items\": [\"minecraft:experience_bottle\"]\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    },\n" +
                "    \"automatic\": {\n" +
                "      \"trigger\": \"minecraft:tick\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"requirements\": [\n" +
                "    [\n" +
                "      \"has_xp_bottle\",\n" +
                "      \"automatic\"\n" +
                "    ]\n" +
                "  ]\n" +
                "}";
            
            Files.writeString(minecraftAdvancementPath, minecraftAdvancementJson);
            XPExtractor.LOGGER.info("Created minecraft advancement at: " + minecraftAdvancementPath);
            
            Path minecraftAutoAdvancementPath = minecraftAdvancementsDir.resolve("xp_extractor_auto.json");
            Files.writeString(minecraftAutoAdvancementPath, autoAdvancementJson);
            XPExtractor.LOGGER.info("Created minecraft auto-trigger advancement at: " + minecraftAutoAdvancementPath);
            
            XPExtractor.LOGGER.info("Advancement files creation complete");
        } catch (Exception e) {
            XPExtractor.LOGGER.error("Failed to create advancement files", e);
        }
    }
    
    public static void patchRecipesIntoManager(RecipeManager recipeManager, MinecraftServer server) {
        try {
            if (recipeManager == null) {
                XPExtractor.LOGGER.error("Cannot patch recipes: RecipeManager is null");
                return;
            }
            
            XPExtractor.LOGGER.info("APPLYING DIRECT RECIPE PATCH to RecipeManager");
            
            boolean foundAnyRecipe = checkRecipesExist(recipeManager);
            
            if (!foundAnyRecipe && server != null) {
                XPExtractor.LOGGER.warn("NO RECIPES FOUND FOR XP EXTRACTOR! ACTIVATING EMERGENCY MODE");
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    givePlayerItemDirectly(player);
                }
            } else if (server != null) {
                forceUnlockRecipes(server);
            }
            
            XPExtractor.LOGGER.info("Recipe patch check complete. Recipes found: " + foundAnyRecipe);
        } catch (Exception e) {
            XPExtractor.LOGGER.error("Failed to patch recipes into RecipeManager", e);
        }
    }
    
    private static boolean checkRecipesExist(RecipeManager recipeManager) {
        boolean foundAnyRecipe = false;
        
        List<String> recipeIds = Arrays.asList(
            "xp_extractor"
        );
        
        for (String recipeId : recipeIds) {
            Identifier id = Identifier.of(XPExtractor.MOD_ID, recipeId);
            Optional<RecipeEntry<?>> recipe = recipeManager.get(id);
            
            if (recipe.isPresent()) {
                foundAnyRecipe = true;
                XPExtractor.LOGGER.info("Found recipe in mod namespace: " + id);
            } else {
                XPExtractor.LOGGER.warn("Recipe not found in mod namespace: " + id);
            }
        }
        
        for (String recipeId : recipeIds) {
            Identifier id = Identifier.of("minecraft", recipeId);
            Optional<RecipeEntry<?>> recipe = recipeManager.get(id);
            
            if (recipe.isPresent()) {
                foundAnyRecipe = true;
                XPExtractor.LOGGER.info("Found recipe in minecraft namespace: " + id);
            } else {
                XPExtractor.LOGGER.warn("Recipe not found in minecraft namespace: " + id);
            }
        }
        
        XPExtractor.LOGGER.info("Dumping all crafting recipes in RecipeManager:");
        recipeManager.listAllOfType(RecipeType.CRAFTING).forEach(entry -> {
            XPExtractor.LOGGER.info("Recipe ID: " + entry.id());
        });
        
        return foundAnyRecipe;
    }
    
    private static void forceUnlockRecipes(MinecraftServer server) {
        if (server == null) {
            XPExtractor.LOGGER.warn("Cannot force-unlock recipes: server is null");
            return;
        }
        
        XPExtractor.LOGGER.info("Forcing recipe unlocks for all players");
        
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            injectRecipesForPlayer(player);
        }
    }
    
    public static void injectRecipesForPlayer(ServerPlayerEntity player) {
        try {
            List<Identifier> recipeIds = new ArrayList<>();
            recipeIds.add(Identifier.of(XPExtractor.MOD_ID, "xp_extractor"));
            recipeIds.add(Identifier.of("minecraft", "xp_extractor"));
            
            RecipeManager recipeManager = player.getServer().getRecipeManager();
            
            List<RecipeEntry<?>> recipesToUnlock = new ArrayList<>();
            for (Identifier id : recipeIds) {
                Optional<RecipeEntry<?>> recipe = recipeManager.get(id);
                if (recipe.isPresent()) {
                    XPExtractor.LOGGER.info("Found recipe to unlock for " + player.getName().getString() + ": " + id);
                    recipesToUnlock.add(recipe.get());
                } else {
                    XPExtractor.LOGGER.warn("Could not find recipe to unlock: " + id);
                }
            }
            
            if (!recipesToUnlock.isEmpty()) {
                player.unlockRecipes(recipesToUnlock);
                XPExtractor.LOGGER.info("Unlocked " + recipesToUnlock.size() + " recipes for player: " + player.getName().getString());
            } else {
                XPExtractor.LOGGER.warn("No recipes found to unlock for player: " + player.getName().getString());
                
                player.getServer().getCommandManager().executeWithPrefix(player.getCommandSource(), "/reload");
                
                givePlayerItemDirectly(player);
            }
        } catch (Exception e) {
            XPExtractor.LOGGER.error("Failed to inject recipes for player: " + player.getName().getString(), e);
        }
    }
    
    public static void givePlayerItemDirectly(ServerPlayerEntity player) {
        try {
            if (player == null) {
                return;
            }
            
            XPExtractor.LOGGER.info("EMERGENCY MODE: Giving XP Extractor item directly to player " + player.getName().getString());
            
            ItemStack xpExtractor = new ItemStack(ModItems.XP_EXTRACTOR);
            
            boolean wasAdded = player.getInventory().insertStack(xpExtractor);
            
            if (!wasAdded) {
                player.dropItem(xpExtractor, false);
            }
            
            XPExtractor.LOGGER.info("Successfully gave XP Extractor item to player " + player.getName().getString());
        } catch (Exception e) {
            XPExtractor.LOGGER.error("Failed to give item directly to player", e);
        }
    }
    
    public static void giveAllPlayersItemDirectly(MinecraftServer server) {
        try {
            if (server == null) {
                XPExtractor.LOGGER.warn("Cannot give items: server is null");
                return;
            }
            
            XPExtractor.LOGGER.info("EMERGENCY MODE: Giving XP Extractor item directly to all players");
            
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                givePlayerItemDirectly(player);
            }
        } catch (Exception e) {
            XPExtractor.LOGGER.error("Failed to give items to all players", e);
        }
    }
}