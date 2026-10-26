package dev.vocabcraft.client.lang;

import dev.vocabcraft.client.srs.DeckStore;

import net.minecraft.client.Minecraft;

public final class GameLanguage {
	private static boolean pushing;

	private GameLanguage() {
	}

	public static void useAsGameLanguage(Minecraft client, String code) {
		if (code == null || code.isBlank()) {
			return;
		}
		if (code.equals(client.getLanguageManager().getSelected()) && code.equals(client.options.languageCode)) {
			return;
		}
		pushing = true;
		try {
			client.getLanguageManager().setSelected(code);
			client.options.languageCode = code;
			client.options.save();
		} finally {
			pushing = false;
		}
		client.reloadResourcePacks();
	}

	public static void followGameLanguage(String code) {
		if (pushing || code == null || code.isBlank()) {
			return;
		}
		DeckStore deck = DeckStore.get();
		if (code.equals(deck.targetLanguage())) {
			return;
		}
		deck.setTargetLanguage(code);
	}
}
