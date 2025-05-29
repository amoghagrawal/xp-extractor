package com.example.xpextractor.fabric;

import com.example.xpextractor.XPExtractor;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Predicate;

public class JsonResourceHook {
    
    private static boolean hasCheckedRecipes = false;
    
    public static void register() {
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new RecipeReloadListener());
        XPExtractor.LOGGER.info("Registered enhanced recipe reload listener");
    }
    
    private static class RecipeReloadListener implements SimpleSynchronousResourceReloadListener {
        @Override
        public Identifier getFabricId() {
            return Identifier.of(XPExtractor.MOD_ID, "recipe_listener");
        }
        
        @Override
        public void reload(ResourceManager manager) {
            XPExtractor.LOGGER.info("Server resources reloading - checking for XP Extractor recipes...");
            
            List<String> recipeVariants = new ArrayList<>();
            recipeVariants.add("xp_extractor.json");
            
            List<String> namespaces = new ArrayList<>();
            namespaces.add(XPExtractor.MOD_ID);
            namespaces.add("minecraft");
            
            for (String namespace : namespaces) {
                for (String recipeFile : recipeVariants) {
                    checkSpecificRecipe(manager, namespace + ":recipes/" + recipeFile);
                }
            }
            
            checkForResources(manager, "recipes", id -> 
                (id.getPath().contains("xp_extractor") || id.getNamespace().equals(XPExtractor.MOD_ID)) && 
                id.getPath().endsWith(".json")
            );
            
            checkForResources(manager, "advancements", id -> 
                id.getPath().contains("recipes") && 
                (id.getPath().contains("xp_extractor") || id.getNamespace().equals(XPExtractor.MOD_ID)) && 
                id.getPath().endsWith(".json")
            );
            
            hasCheckedRecipes = true;
        }
        
        private void checkForResources(ResourceManager manager, String prefix, Predicate<Identifier> filter) {
            var resources = manager.findResources(prefix, filter);
            
            if (resources.isEmpty()) {
                XPExtractor.LOGGER.warn("No " + prefix + " resources found matching the filter!");
            } else {
                XPExtractor.LOGGER.info("Found XP Extractor " + prefix + " resources: " + resources.size());
                for (var entry : resources.entrySet()) {
                    XPExtractor.LOGGER.info("Resource: " + entry.getKey());
                    
                    try (InputStream stream = manager.getResource(entry.getKey()).get().getInputStream()) {
                        int size = stream.available();
                        XPExtractor.LOGGER.info("  - Size: " + size + " bytes");
                        
                        if (entry.getKey().getPath().contains("xp_extractor") && size < 2000) {
                            try (BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(manager.getResource(entry.getKey()).get().getInputStream(), StandardCharsets.UTF_8))) {
                                String content = reader.lines().collect(java.util.stream.Collectors.joining("\n"));
                                XPExtractor.LOGGER.debug("  - Full content: " + content);
                                
                                boolean hasResult = content.contains("\"result\"");
                                boolean hasItem = content.contains("\"item\"");
                                boolean hasXpExtractor = content.contains("xp_extractor");
                                boolean hasExperienceBottle = content.contains("experience_bottle");
                                
                                XPExtractor.LOGGER.info("  - Recipe validation: " +
                                    "Has result: " + hasResult + ", " +
                                    "Has item: " + hasItem + ", " +
                                    "References xp_extractor: " + hasXpExtractor + ", " +
                                    "References experience_bottle: " + hasExperienceBottle);
                            }
                        }
                    } catch (Exception e) {
                        XPExtractor.LOGGER.error("  - Failed to read resource content: " + e.getMessage());
                    }
                }
            }
        }
        
        private void checkSpecificRecipe(ResourceManager manager, String path) {
            try {
                String namespace, recipePath;
                if (path.contains(":")) {
                    String[] parts = path.split(":", 2);
                    namespace = parts[0];
                    recipePath = parts[1];
                } else {
                    namespace = "minecraft";
                    recipePath = path;
                }
                
                Identifier id = Identifier.of(namespace, recipePath);
                var optional = manager.getResource(id);
                
                if (optional.isPresent()) {
                    XPExtractor.LOGGER.info("✓ Found specific recipe: " + path);
                    try (InputStream stream = optional.get().getInputStream()) {
                        int size = stream.available();
                        XPExtractor.LOGGER.info("  - Size: " + size + " bytes");
                        
                        if (size < 2000) {
                            try (BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(optional.get().getInputStream(), StandardCharsets.UTF_8))) {
                                String content = reader.lines().collect(java.util.stream.Collectors.joining("\n"));
                                XPExtractor.LOGGER.debug("  - Full content: " + content);
                                
                                boolean hasResult = content.contains("\"result\"");
                                boolean hasItem = content.contains("\"item\"");
                                boolean hasXpExtractor = content.contains("xp_extractor");
                                boolean hasExperienceBottle = content.contains("experience_bottle");
                                
                                XPExtractor.LOGGER.info("  - Recipe validation: " +
                                    "Has result: " + hasResult + ", " +
                                    "Has item: " + hasItem + ", " +
                                    "References xp_extractor: " + hasXpExtractor + ", " +
                                    "References experience_bottle: " + hasExperienceBottle);
                            }
                        }
                    } catch (Exception e) {
                        XPExtractor.LOGGER.error("  - Failed to read recipe: " + e.getMessage());
                    }
                } else {
                    XPExtractor.LOGGER.warn("Did not find specific recipe: " + path);
                }
            } catch (Exception e) {
                XPExtractor.LOGGER.error("Error checking specific recipe: " + path, e);
            }
        }
    }
}