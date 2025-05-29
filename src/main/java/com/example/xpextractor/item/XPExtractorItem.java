package com.example.xpextractor.item;

import com.example.xpextractor.XPExtractor;
import com.example.xpextractor.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.ParticlesMode;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CompassItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.joml.Vector3f;
import org.slf4j.Logger;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public class XPExtractorItem extends CompassItem {
    private static final Vector3f XP_PARTICLE_COLOR = new Vector3f(0.3f, 0.8f, 0.3f);
    private static final Vector3f XP_PARTICLE_COLOR_BRIGHT = new Vector3f(0.4f, 1.0f, 0.4f);
    private static final Vector3f XP_PARTICLE_COLOR_DARK = new Vector3f(0.2f, 0.6f, 0.2f);
    private static final Vector3f MAGIC_PARTICLE_COLOR = new Vector3f(0.5f, 0.2f, 0.9f);
    
    private static final Logger LOGGER = XPExtractor.LOGGER;
    
    private static final int PARTICLE_DISTANCE_SQUARED = 32 * 32;
    private static final int AMBIENT_PARTICLE_CHANCE = 16;
    private static final float DEFAULT_PARTICLE_SIZE = 1.0f;
    
    public XPExtractorItem(Settings settings) {
        super(settings.maxDamage(getConfigSafely().getMaxDurability()));
    }

    private static ModConfig getConfigSafely() {
        ModConfig config = XPExtractor.getConfig();
        if (config == null) {
            LOGGER.warn("Failed to get mod configuration, using default instance");
            return new ModConfig();
        }
        return config;
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public Text getName(ItemStack stack) {
        return super.getName(stack);
    }
    
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        if (world == null || player == null) {
            return TypedActionResult.pass(ItemStack.EMPTY);
        }
        
        ItemStack stack = player.getStackInHand(hand);
        if (stack.isEmpty()) {
            return TypedActionResult.pass(stack);
        }
        
        try {
            ModConfig config = getConfigSafely();
            
            if (stack.getDamage() >= stack.getMaxDamage() - 1) {
                if (!world.isClient) {
                    player.sendMessage(Text.translatable("message.xpextractor.too_damaged"), true);
                    
                    playSoundSafely(world, player, SoundEvents.ENTITY_ITEM_BREAK, 0.8f, 1.0f);
                }
                return TypedActionResult.fail(stack);
            }
            
            int bottlesPerExtraction = config.getXpBottlesPerExtraction();
            int maxExtractions = config.getMaxExtractionsPerUse();
            int cooldownTicks = config.getCooldownTicks();
            
            int xpPerBottle = 7;
            int usageFee = 2;
            int totalXpPerBottle = xpPerBottle + usageFee;
            
            int playerXp = getTotalPlayerXp(player);
            
            int possibleExtractions = Math.min(maxExtractions, playerXp / totalXpPerBottle);
            
            if (possibleExtractions > 0) {
                try {
                    ItemStack xpBottles = new ItemStack(Items.EXPERIENCE_BOTTLE, bottlesPerExtraction * possibleExtractions);
                    
                    boolean wasAdded = player.getInventory().insertStack(xpBottles);
                    
                    if (!wasAdded) {
                        ItemEntity itemEntity = new ItemEntity(
                            world, 
                            player.getX(), player.getY() + 0.5, player.getZ(),
                            xpBottles
                        );
                        itemEntity.setPickupDelay(10);
                        world.spawnEntity(itemEntity);
                    }
                    
                    int totalXpCost = possibleExtractions * totalXpPerBottle;
                    player.addExperience(-totalXpCost);
                    
                    if (hand == Hand.MAIN_HAND) {
                        stack.damage(possibleExtractions, player, EquipmentSlot.MAINHAND);
                    } else {
                        stack.damage(possibleExtractions, player, EquipmentSlot.OFFHAND);
                    }
                    
                    if (world instanceof ServerWorld serverWorld) {
                        spawnExtractionParticles(serverWorld, player, possibleExtractions);
                    }
                    
                    playSoundSafely(
                        world, 
                        player, 
                        SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
                        0.7f,
                        0.8f + world.getRandom().nextFloat() * 0.2f
                    );
                    
                    playSoundSafely(
                        world,
                        player,
                        SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                        0.6f,
                        0.5f + (possibleExtractions * 0.05f)
                    );
                    
                    playSoundSafely(
                        world,
                        player,
                        SoundEvents.ITEM_BOTTLE_FILL,
                        0.5f,
                        1.1f + world.getRandom().nextFloat() * 0.2f
                    );
                    
                    playSoundSafely(
                        world,
                        player,
                        SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL,
                        0.3f,
                        1.2f
                    );
                    
                    int totalBottles = bottlesPerExtraction * possibleExtractions;
                    int totalXpUsed = possibleExtractions * totalXpPerBottle;
                    
                    if (possibleExtractions == 1 && bottlesPerExtraction == 1) {
                        player.sendMessage(Text.translatable("message.xpextractor.extraction_success").formatted(Formatting.GREEN), true);
                    } else {
                        player.sendMessage(Text.translatable("message.xpextractor.extraction_multiple", totalXpUsed, totalBottles).formatted(Formatting.GREEN), true);
                    }
                } catch (Exception e) {
                    LOGGER.error("Error during XP extraction", e);
                    player.sendMessage(Text.literal("An error occurred during XP extraction").formatted(Formatting.RED), false);
                }
            } else {
                if (!world.isClient) {
                    player.sendMessage(Text.translatable("message.xpextractor.insufficient_xp", totalXpPerBottle).formatted(Formatting.RED), true);
                    
                    playSoundSafely(world, player, SoundEvents.BLOCK_FIRE_EXTINGUISH, 0.4f, 1.0f);
                    playSoundSafely(world, player, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 0.2f, 0.5f);
                    
                    if (world instanceof ServerWorld serverWorld) {
                        spawnFailureParticles(serverWorld, player);
                    }
                }
                return TypedActionResult.fail(stack);
            }
            
            player.getItemCooldownManager().set(this, cooldownTicks);
            
            return TypedActionResult.success(stack);
        } catch (Exception e) {
            LOGGER.error("Error using XP Extractor", e);
            if (!world.isClient) {
                player.sendMessage(Text.literal("An error occurred using the XP Extractor").formatted(Formatting.RED), false);
            }
            return TypedActionResult.fail(stack);
        }
    }
    
    private void playSoundSafely(World world, PlayerEntity player, net.minecraft.sound.SoundEvent sound, float volume, float pitch) {
        try {
            world.playSound(
                null, 
                player.getX(), player.getY(), player.getZ(),
                sound,
                SoundCategory.PLAYERS,
                volume,
                pitch
            );
        } catch (Exception e) {
            LOGGER.warn("Failed to play sound: {}", sound.getId(), e);
        }
    }
    
    private void spawnClientSideParticles(World world, PlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        ParticlesMode particlesMode = client.options.getParticles().getValue();
        if (particlesMode == ParticlesMode.MINIMAL) {
            return;
        }
        
        Random random = world.getRandom();
        
        int particleMultiplier = (particlesMode == ParticlesMode.ALL) ? 5 : 2;
        int particleCount = Math.min(15, 5 + (particleMultiplier * 5));
        for (int i = 0; i < particleCount; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 0.5;
            double offsetY = (random.nextDouble() - 0.5) * 0.5;
            double offsetZ = (random.nextDouble() - 0.5) * 0.5;
            
            world.addParticle(
                ParticleTypes.ENCHANT,
                player.getX() + offsetX,
                player.getY() + 1.0 + offsetY,
                player.getZ() + offsetZ,
                0, 0, 0
            );
        }
        
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4;
            double radius = 0.6;
            double offsetX = Math.sin(angle) * radius;
            double offsetZ = Math.cos(angle) * radius;
            
            world.addParticle(
                ParticleTypes.PORTAL,
                player.getX() + offsetX,
                player.getY() + 0.1,
                player.getZ() + offsetZ,
                0, 0.1, 0
            );
        }
        
        particleCount = Math.min(12, 4 + (particleMultiplier * 4));
        for (int i = 0; i < particleCount; i++) {
            double angle = i * MathHelper.TAU / particleCount;
            double radius = 1.0;
            double offsetX = Math.sin(angle) * radius;
            double offsetZ = Math.cos(angle) * radius;
            
            world.addParticle(
                ParticleTypes.WITCH,
                player.getX() + offsetX,
                player.getY() + 0.8,
                player.getZ() + offsetZ,
                -offsetX * 0.1, -0.02, -offsetZ * 0.1
            );
        }
    }
    
    private void spawnExtractionParticles(ServerWorld world, PlayerEntity player, int extractionCount) {
        try {
            boolean playersNearby = false;
            for (PlayerEntity otherPlayer : world.getPlayers()) {
                if (otherPlayer != player && otherPlayer.squaredDistanceTo(player) <= PARTICLE_DISTANCE_SQUARED) {
                    playersNearby = true;
                    break;
                }
            }
            
            if (!playersNearby && player instanceof ServerPlayerEntity) {
                playersNearby = true;
            }
            
            if (!playersNearby) {
                return;
            }
            
            ModConfig config = getConfigSafely();
            int particlesPerExtraction = Math.max(8, config.getMaxParticlesPerExtraction());
            int totalParticles = Math.min(150, extractionCount * particlesPerExtraction);
            
            Random random = world.getRandom();
            
            double radius = 2.0;
            for (int i = 0; i < totalParticles / 2; i++) {
                double progress = (double) i / (totalParticles / 2);
                double angle = progress * Math.PI * 12;
                
                double spiralRadius = radius * (1 - progress * 0.8); 
                
                double x1 = player.getX() + Math.sin(angle) * spiralRadius;
                double z1 = player.getZ() + Math.cos(angle) * spiralRadius;
                double y1 = player.getY() + 2.5 - progress * 2.0;
                
                Vector3f color = i % 3 == 0 ? XP_PARTICLE_COLOR_BRIGHT : 
                                 i % 3 == 1 ? XP_PARTICLE_COLOR : XP_PARTICLE_COLOR_DARK;
                
                float size = DEFAULT_PARTICLE_SIZE * (0.7f + (float)progress * 0.6f);
                                 
                world.spawnParticles(
                    new DustParticleEffect(color, size),
                    x1, y1, z1,
                    1,
                    0.02, 0.02, 0.02,
                    0.01
                );
                
                if (i % 2 == 0) {
                    double x2 = player.getX() + Math.sin(angle + Math.PI) * spiralRadius;
                    double z2 = player.getZ() + Math.cos(angle + Math.PI) * spiralRadius;
                    double y2 = player.getY() + 0.1 + progress * 1.0;
                    
                    world.spawnParticles(
                        new DustParticleEffect(color, size * 0.8f),
                        x2, y2, z2,
                        1,
                        0.02, 0.02, 0.02,
                        0.01
                    );
                }
            }
            
            Vec3d handPos = player.getEyePos().subtract(0, 0.2, 0).add(player.getRotationVector().multiply(0.5));
            for (int i = 0; i < Math.min(30, 10 + extractionCount * 4); i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double distance = 1.0 + random.nextDouble() * 1.0;
                double height = 0.5 + random.nextDouble() * 1.5;
                
                double x = player.getX() + Math.sin(angle) * distance;
                double z = player.getZ() + Math.cos(angle) * distance;
                double y = player.getY() + height;
                
                double vx = (handPos.x - x) * 0.1;
                double vy = (handPos.y - y) * 0.1;
                double vz = (handPos.z - z) * 0.1;
                
                if (i % 4 == 0) {
                    world.spawnParticles(
                        new DustParticleEffect(MAGIC_PARTICLE_COLOR, 0.7f),
                        x, y, z,
                        1,
                        vx * 0.2, vy * 0.2, vz * 0.2,
                        0.05
                    );
                } else {
                    world.spawnParticles(
                        ParticleTypes.ENCHANT,
                        x, y, z,
                        1,
                        vx, vy, vz,
                        0.05
                    );
                }
            }
            
            for (int i = 0; i < Math.min(15, 5 + extractionCount * 2); i++) {
                double offsetX = random.nextGaussian() * 0.3;
                double offsetY = random.nextGaussian() * 0.3;
                double offsetZ = random.nextGaussian() * 0.3;
                
                world.spawnParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    player.getX() + offsetX,
                    player.getY() + 0.8 + offsetY,
                    player.getZ() + offsetZ,
                    1, 0, 0, 0, 0
                );
            }
            
            for (int i = 0; i < 12; i++) {
                world.spawnParticles(
                    ParticleTypes.WITCH,
                    handPos.x, handPos.y, handPos.z,
                    3, 
                    0.07, 0.07, 0.07, 
                    0.01
                );
            }
            
            world.spawnParticles(
                ParticleTypes.END_ROD,
                handPos.x, handPos.y, handPos.z,
                extractionCount * 3,
                0.2, 0.2, 0.2,
                0.05
            );
            
        } catch (Exception e) {
            LOGGER.warn("Error spawning particles", e);
        }
    }
    
    private void spawnFailureParticles(ServerWorld world, PlayerEntity player) {
        try {
            Vec3d handPos = player.getEyePos().subtract(0, 0.2, 0).add(player.getRotationVector().multiply(0.5));
            
            world.spawnParticles(
                ParticleTypes.SMOKE,
                handPos.x, handPos.y, handPos.z,
                8,
                0.05, 0.05, 0.05,
                0.02
            );
            
            world.spawnParticles(
                ParticleTypes.CRIT,
                handPos.x, handPos.y, handPos.z,
                5,
                0.1, 0.1, 0.1,
                0.05
            );
            
            world.spawnParticles(
                new DustParticleEffect(new Vector3f(0.8f, 0.1f, 0.1f), 1.0f),
                handPos.x, handPos.y, handPos.z,
                6,
                0.1, 0.1, 0.1,
                0.01
            );
        } catch (Exception e) {
            LOGGER.warn("Error spawning failure particles", e);
        }
    }
    
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        try {
            super.inventoryTick(stack, world, entity, slot, selected);
            
            if (!world.isClient || !(entity instanceof PlayerEntity) || !selected) {
                return;
            }
            
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) return;
            
            ParticlesMode particlesMode = client.options.getParticles().getValue();
            if (particlesMode == ParticlesMode.MINIMAL) {
                return;
            }
            
            PlayerEntity player = (PlayerEntity) entity;
            Random random = world.getRandom();
            
            if (random.nextInt(AMBIENT_PARTICLE_CHANCE) == 0) {
                world.addParticle(
                    ParticleTypes.REVERSE_PORTAL,
                    entity.getX() + (random.nextDouble() - 0.5) * 0.5,
                    entity.getY() + 1.0,
                    entity.getZ() + (random.nextDouble() - 0.5) * 0.5,
                    0, 0.1, 0
                );
                
                if (random.nextBoolean()) {
                    world.addParticle(
                        ParticleTypes.ENCHANT,
                        entity.getX() + (random.nextDouble() - 0.5) * 0.3,
                        entity.getY() + 0.8 + (random.nextDouble() - 0.5) * 0.2,
                        entity.getZ() + (random.nextDouble() - 0.5) * 0.3,
                        0, 0, 0
                    );
                }
                
                Vec3d handPos = player.getEyePos().subtract(0, 0.2, 0).add(player.getRotationVector().multiply(0.5));
                
                if (random.nextInt(3) == 0) {
                    world.addParticle(
                        ParticleTypes.END_ROD,
                        handPos.x + (random.nextDouble() - 0.5) * 0.1,
                        handPos.y + (random.nextDouble() - 0.5) * 0.1,
                        handPos.z + (random.nextDouble() - 0.5) * 0.1,
                        0, 0, 0
                    );
                }
                
                if (random.nextInt(8) == 0) {
                    world.playSound(
                        player,
                        player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                        SoundCategory.PLAYERS,
                        0.1f,
                        0.8f + random.nextFloat() * 0.4f
                    );
                }
            }
        } catch (Exception e) {
            if (world.getTime() % 200 == 0) {
                LOGGER.debug("Error in inventory tick", e);
            }
        }
    }
    
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    private int getTotalPlayerXp(PlayerEntity player) {
        int level = player.experienceLevel;
        float progress = player.experienceProgress;
        int xp = 0;
        
        for (int i = 0; i < level; i++) {
            xp += getXpNeededForLevel(i);
        }
        
        xp += Math.round(progress * getXpNeededForLevel(level));
        
        return xp;
    }
    
    private int getXpNeededForLevel(int level) {
        if (level >= 30) {
            return 112 + (level - 30) * 9;
        } else if (level >= 15) {
            return 37 + (level - 15) * 5;
        } else {
            return 7 + level * 2;
        }
    }
}