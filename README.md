# MineGlot

Client-side AI translator for **Minecraft 1.8.9 Forge**.

MineGlot translates your own messages, selected player chat, and signs using
OpenAI or Claude. Your API key stays on your computer.

[![Minecraft](https://img.shields.io/badge/Minecraft-1.8.9-62b047?style=flat-square)](https://minecraft.net)
[![Forge](https://img.shields.io/badge/Forge-11.15.1.2318-orange?style=flat-square)](https://files.minecraftforge.net/)
[![Java](https://img.shields.io/badge/Java-8-red?style=flat-square)](https://adoptium.net/)
[![Side](https://img.shields.io/badge/Side-Client%20only-blue?style=flat-square)](#)
[![License](https://img.shields.io/badge/License-Apache%202.0-lightgrey?style=flat-square)](LICENSE)

## Preview

<p align="center">
  <img src="https://raw.githubusercontent.com/wiki/NtGitG/minecraft-ai-translator/images/menu-haut.png" alt="MineGlot main menu" width="720">
</p>

<p align="center">
  <img src="https://raw.githubusercontent.com/wiki/NtGitG/minecraft-ai-translator/images/hud.png" alt="MineGlot HUD" width="355">
  <img src="https://raw.githubusercontent.com/wiki/NtGitG/minecraft-ai-translator/images/configuration.png" alt="MineGlot configuration" width="355">
</p>

## Documentation

Full documentation on the **[GitHub wiki](https://github.com/NtGitG/minecraft-ai-translator/wiki)**:

- [Getting Started](https://github.com/NtGitG/minecraft-ai-translator/wiki/Getting-Started)
- [Commands](https://github.com/NtGitG/minecraft-ai-translator/wiki/Commands)
- [How It Works](https://github.com/NtGitG/minecraft-ai-translator/wiki/How-It-Works)
- [Configuration](https://github.com/NtGitG/minecraft-ai-translator/wiki/Configuration)
- [Troubleshooting](https://github.com/NtGitG/minecraft-ai-translator/wiki/Troubleshooting)
- [FAQ](https://github.com/NtGitG/minecraft-ai-translator/wiki/FAQ)

## Quick Start

Requirements:

- Minecraft `1.8.9`
- Forge `11.15.1.2318`
- Java `8`
- OpenAI or Claude API key

Install:

1. Put the MineGlot `.jar` in `.minecraft/mods`.
2. Launch Minecraft with Forge 1.8.9.
3. Open the MineGlot GUI and set your API key, language, and model.
4. Test with `/trs Bonjour`.

## Features

- Translate text with `/trs <text>` or `/translate <text>`.
- Translate an existing chat line with `/translation` and a click.
- Send translated private messages safely with `/trs msg <player> <text>`.
- Auto-translate chat from selected players.
- Translate signs on interaction.
- Cache translations in memory and RocksDB to reduce repeated API calls.
- Choose provider, model, target language, and default language in-game.

## Main Commands

| Command | Purpose |
| --- | --- |
| `/trs <text>` | Translate text to your target language |
| `/trs msg <player> <text>` | Translate and send as private message; unknown recipients fail without sending |
| `/translate <text>` | Same translation command |
| `/translation` | Click an already visible or incoming chat line to translate it |
| `/trs-clear` | Remove the last cached translation |
| `/transexport weekly` | Export usage stats |

See the full list in [Commands](https://github.com/NtGitG/minecraft-ai-translator/wiki/Commands).

## Build

```powershell
.\gradlew.bat test
.\gradlew.bat clean build
```

Jar output: `build/libs/`

## Acknowledgements

MineGlot was developed with AI assistance for code review, refactoring, and
documentation polish. Thanks to OpenAI and Anthropic Claude for helping speed up
parts of the development process.

## License

Apache 2.0. See [LICENSE](LICENSE).
