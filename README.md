# Vocab Craft

**Vibe coded.** An AI assistant wrote this project in a chat. Read [VIBE.md](VIBE.md) before you rely on it.

Vocab Craft is a Minecraft Java mod that quizzes you on names while you play. Break a block, place one, or use one. Take the result of a craft, including the inventory grid and the recipe book, or from a furnace, smithing table, anvil, or stonecutter. Take a villager's trade. Pick an item up, or let a new one enter your inventory from a chest. Hit or use a mob. When that thing is new or due, it waits in a review queue. The first time you see it, a short notice names it. The card stays closed until you ask for it.

Press V, or Review on the title screen or the pause screen, to study the queue. Lying in a bed opens it when cards are waiting. It also opens once the queue reaches the Study threshold, 10 cards by default, when nothing else is on screen. The card shows the icon. Flip it to see the name in your target language, with your support language underneath. Mark the answer right or wrong, and the next queued card follows. Escape leaves the rest for later. The deck stays on this computer and uses a sped-up SM-2 schedule.

Hitting a block, tilling, and stripping do not queue a card. Moving an item you already carry does not either. The recipe book fills the grid; the card is queued when you take the result. Study, on the Vocab Craft page, sets the notice style, the cooldown, combat and movement suppression, the daily and pending caps, the bed review, and how many waiting cards open a review. Turning queue mode off restores the old behavior: the card opens immediately, and a screen such as a crafting table or a villager trade waits until the card closes.

## Play

1. Install Fabric Loader for Minecraft Java 26.3, Fabric API `0.165.0+26.3`, and this mod.
2. On the title screen, choose a support language. The target language is the game's language. Changing either one changes the other.
3. Vocab on the title screen, and Vocab Craft on the pause screen, open that page again: languages, study options, progress charts, export, import, and deck reset. Done returns to the screen that opened it. The title screen shows how many reviews are waiting, and Review starts them. A `Vocab N` count sits above the hotbar while you play.

Cards are stored in `config/vocab-craft/deck.json` inside the Minecraft instance. Export writes a copy under `config/vocab-craft/exports` and copies the JSON to the clipboard. Import replaces the deck and both languages from a file or the clipboard. A file this version cannot read is rejected, and the current deck stays.

## Build

Needs JDK 25. Versions are pinned in `gradle.properties`.

```sh
./gradlew test build
```

The jar is `build/libs/vocab-craft-0.1.0.jar`. `./gradlew runClient` launches a development client.

## Download

A push to `main` runs the tests, builds the jar, and publishes a beta on Modrinth. That page is where to install the latest build. The same jar is attached to the Actions run.

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
