package com.example.xpextractor.config;

import com.example.xpextractor.XPExtractor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("xpextractor.json");
    
    private static volatile ModConfig INSTANCE;
    private static final AtomicBoolean IS_LOADING = new AtomicBoolean(false);
    
    private int xpBottlesPerExtraction = 1;
    private int cooldownTicks = 60;
    private int maxExtractionsPerUse = 1;
    private int maxDurability = 32;
    private int repairAmountPerBottle = 16;
    
    private transient int maxParticlesPerExtraction;
    
    public static ModConfig getInstance() {
        ModConfig result = INSTANCE;
        if (result == null) {
            synchronized (ModConfig.class) {
                result = INSTANCE;
                if (result == null) {
                    loadConfig();
                    result = INSTANCE;
                }
            }
        }
        return result;
    }
    
    public static void loadConfig() {
        if (!IS_LOADING.compareAndSet(false, true)) {
            XPExtractor.LOGGER.debug("Config loading already in progress, skipping duplicate load");
            return;
        }
        
        try {
            if (Files.exists(CONFIG_PATH)) {
                try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                    ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
                    
                    if (loaded == null) {
                        throw new JsonParseException("Config file parsed as null");
                    }
                    
                    loaded.validateAndInitialize();
                    
                    INSTANCE = loaded;
                    XPExtractor.LOGGER.info("Loaded XP Extractor configuration from: " + CONFIG_PATH);
                } catch (JsonParseException e) {
                    XPExtractor.LOGGER.error("Failed to parse config file: " + e.getMessage());
                    XPExtractor.LOGGER.error("Using default configuration instead");
                    createDefaultConfig();
                }
            } else {
                createDefaultConfig();
            }
        } catch (IOException e) {
            XPExtractor.LOGGER.error("Failed to load config: " + e.getMessage(), e);
            XPExtractor.LOGGER.error("Using default configuration instead");
            
            INSTANCE = new ModConfig();
            INSTANCE.validateAndInitialize();
        } finally {
            IS_LOADING.set(false);
        }
    }
    
    private static void createDefaultConfig() throws IOException {
        INSTANCE = new ModConfig();
        INSTANCE.validateAndInitialize();
        saveConfig();
        XPExtractor.LOGGER.info("Created default XP Extractor configuration at: " + CONFIG_PATH);
    }
    
    public static void saveConfig() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(INSTANCE, writer);
                XPExtractor.LOGGER.info("Saved XP Extractor configuration to: " + CONFIG_PATH);
            }
        } catch (IOException e) {
            XPExtractor.LOGGER.error("Failed to save config: " + e.getMessage(), e);
            XPExtractor.LOGGER.error("Config path: " + CONFIG_PATH);
        }
    }
    
    private void validateAndInitialize() {
        setXpBottlesPerExtraction(xpBottlesPerExtraction);
        setCooldownTicks(cooldownTicks);
        setMaxExtractionsPerUse(maxExtractionsPerUse);
        setMaxDurability(maxDurability);
        setRepairAmountPerBottle(repairAmountPerBottle);
        
        updateDerivedValues();
    }
    
    private void updateDerivedValues() {
        this.maxParticlesPerExtraction = Math.min(20, 5 + (5 * maxExtractionsPerUse));
    }
    
    public int getXpBottlesPerExtraction() {
        return xpBottlesPerExtraction;
    }
    
    public void setXpBottlesPerExtraction(int xpBottlesPerExtraction) {
        this.xpBottlesPerExtraction = Math.max(1, xpBottlesPerExtraction);
    }
    
    public int getCooldownTicks() {
        return cooldownTicks;
    }
    
    public void setCooldownTicks(int cooldownTicks) {
        this.cooldownTicks = Math.max(0, cooldownTicks);
    }
    
    public int getMaxExtractionsPerUse() {
        return maxExtractionsPerUse;
    }
    
    public void setMaxExtractionsPerUse(int maxExtractionsPerUse) {
        this.maxExtractionsPerUse = Math.max(1, maxExtractionsPerUse);
        updateDerivedValues();
    }
    
    public int getMaxDurability() {
        return maxDurability;
    }
    
    public void setMaxDurability(int maxDurability) {
        this.maxDurability = Math.max(1, maxDurability);
    }
    
    public int getRepairAmountPerBottle() {
        return repairAmountPerBottle;
    }
    
    public void setRepairAmountPerBottle(int repairAmountPerBottle) {
        this.repairAmountPerBottle = Math.max(1, repairAmountPerBottle);
    }
    
    public int getMaxParticlesPerExtraction() {
        return maxParticlesPerExtraction;
    }
}