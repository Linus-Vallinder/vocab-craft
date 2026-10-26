# Vocab Craft

**Vibe coded.** An AI assistant wrote this project in a chat. Read [VIBE.md](VIBE.md) before you rely on it.

Vocab Craft is a Minecraft Java mod that quizzes you on block names while you play. Break a block, place one, or use one, such as a furnace, a door, or a crafting table. If that block is new or due, a card opens. The front shows the icon. Flip it to see the name in your target language, with your support language underneath. Mark the answer right or wrong. The deck stays on this computer and uses a sped-up SM-2 schedule.

Hitting a block, picking an item up, tilling, and stripping do not open a card.

## Play

1. Install Fabric Loader for Minecraft Java 26.3, Fabric API `0.165.0+26.3`, and this mod.
2. On the title screen, choose a target language and a support language. The game's own language does not change.
3. The Vocab Craft button on the title screen opens that page again: languages, progress charts, export, import, and deck reset.

Cards are stored in `config/vocab-craft/deck.json` inside the Minecraft instance. Export writes a copy under `config/vocab-craft/exports` and copies the JSON to the clipboard. Import replaces the deck and both languages from a file or the clipboard. A file this version cannot read is rejected, and the current deck stays.

## Build

Needs JDK 25. Versions are pinned in `gradle.properties`.

```sh
./gradlew test build
```

The jar is `build/libs/vocab-craft-0.1.0.jar`. `./gradlew runClient` launches a development client.

## Layout

| Path | What it is |
| --- | --- |
| `src/main` | Shared mod code and the review schedule |
| `src/client` | Screens, deck storage, and interaction hooks |
| `src/test` | Schedule and deck-file tests |
| `bedrock/behavior_pack` | Bedrock shell. It only logs that it loaded |
| `shared/content.json` | Placeholder for lesson files. Nothing reads it yet |

Bedrock cannot load a Fabric jar. Geyser does not run this behavior pack. Java worlds use the mod. A Bedrock world can enable the pack, and the pack does not review blocks.

## License

All rights reserved.
