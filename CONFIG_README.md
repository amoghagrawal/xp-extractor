# XP Extractor Configuration Guide

The XP Extractor mod allows you to customize various aspects of its functionality through a configuration file. This file (`xpextractor.json`) is automatically created in your Minecraft config folder the first time you run the mod.

## Configuration File Location

The configuration file is located at:
- Windows: `.minecraft/config/xpextractor.json`
- Mac: `~/Library/Application Support/minecraft/config/xpextractor.json`
- Linux: `~/.minecraft/config/xpextractor.json`

## Configuration Options

The following options can be customized:

### minXpLevel

**Default:** 5

The minimum number of XP levels a player must have to use the XP Extractor. Players with fewer levels than this will receive an error message when trying to use the item.

```json
"minXpLevel": 5
```

### xpBottlesPerExtraction

**Default:** 1

The number of XP bottles given to the player for each extraction. Increasing this value makes the XP Extractor more efficient, giving more bottles per level spent.

```json
"xpBottlesPerExtraction": 1
```

### cooldownTicks

**Default:** 60

The cooldown period (in ticks) after using the XP Extractor before it can be used again. There are 20 ticks per second, so the default (60) is a 3-second cooldown.

```json
"cooldownTicks": 60
```

### maxExtractionsPerUse

**Default:** 1

The maximum number of extractions that can be performed in a single use of the XP Extractor. When set higher than 1, the XP Extractor will automatically perform multiple extractions if the player has enough XP levels.

```json
"maxExtractionsPerUse": 1
```

### maxDurability

**Default:** 128

The maximum durability of the XP Extractor. Each extraction reduces durability by 1. When the durability is depleted, the item can no longer be used until repaired.

```json
"maxDurability": 128
```

### repairAmountPerBottle

**Default:** 16

How much durability is repaired when using an XP bottle to repair the XP Extractor in a crafting recipe. Higher values mean more efficient repairs.

```json
"repairAmountPerBottle": 16
```

## Example Configuration

Here's an example of a complete configuration file with custom settings:

```json
{
  "minXpLevel": 3,
  "xpBottlesPerExtraction": 2,
  "cooldownTicks": 40,
  "maxExtractionsPerUse": 3,
  "maxDurability": 256,
  "repairAmountPerBottle": 32
}
```

With this configuration:
- Players need at least 3 XP levels to use the extractor
- Each extraction gives 2 XP bottles
- There's a 2-second cooldown (40 ticks)
- The extractor can perform up to 3 extractions at once if the player has enough levels
- The item can be used 256 times before breaking
- Each XP bottle repairs 32 durability points when crafting to repair

## Changing the Configuration

You can edit the configuration file with any text editor. After making changes, save the file and restart Minecraft for the changes to take effect.

Make sure to maintain valid JSON syntax when editing the file. 