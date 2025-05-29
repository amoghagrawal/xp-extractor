# XP Extractor Mod

A Minecraft mod that allows players to extract and store their experience points into bottles. This mod is designed for Minecraft 1.21.1 using Fabric.

## Features

- Extract XP into Experience Bottles
- Beautiful particle effects and sounds

## Requirements

- Minecraft 1.21.1
- Fabric Loader 0.15.11 or higher
- Fabric API 0.115.1 or higher
- Java 17 or higher

## Installation

### For Players
1. Install Fabric Loader for Minecraft 1.21.1
2. Install Fabric API
3. Place the XP Extractor mod JAR file in your mods folder

### For Server Hosts
1. Install Fabric Loader on your server
2. Install Fabric API on your server
3. Place the XP Extractor mod JAR file in your server's mods folder

**Important**: This mod must be installed on both the server and all clients to work properly. Players without the mod cannot join a server with the mod installed, and vice versa.

## Recipe

```
[E][E][E]
[E][L][E]
[E][E][E]
```
- E = Experience Bottle
- L = Lodestone
- Result: 1 XP Extractor

## Usage

1. Hold the XP Extractor in your hand
2. Right-click to extract XP
3. Each extraction:
   - Costs 9 XP per bottle (7 XP base + 2 XP usage fee)
   - Creates 1 Experience Bottle (configurable)
   - Damages the XP Extractor by 1
   - Has a 3-second cooldown (configurable)

## Configuration

The mod can be configured through the `xpextractor.json` file in your config folder:

- `xpBottlesPerExtraction`: Number of bottles created per use (default: 1)
- `cooldownTicks`: Cooldown between uses in ticks (default: 60)
- `maxExtractionsPerUse`: Maximum number of extractions per use (default: 1)
- `maxDurability`: Maximum durability of the XP Extractor (default: 32)

## License

This mod is licensed under the MIT License. 