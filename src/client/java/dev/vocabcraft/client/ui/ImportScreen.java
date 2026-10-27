package dev.vocabcraft.client.ui;

import dev.vocabcraft.VocabCraft;
import dev.vocabcraft.client.lang.GameLanguage;
import dev.vocabcraft.client.srs.DeckStore;
import dev.vocabcraft.srs.DeckFile;
import dev.vocabcraft.srs.DeckFormatException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class ImportScreen extends Screen {
	private static final int ERROR = 0xFFFFAAAA;

	private final Screen parent;
	private EditBox pathBox;
	private String notice = "";

	public ImportScreen(Screen parent) {
		super(Component.literal("Import progress"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int center = this.width / 2;
		pathBox = new EditBox(this.font, center - 150, 56, 300, 20, Component.literal("Progress file"));
		pathBox.setMaxLength(1024);
		pathBox.setHint(Component.literal("Path to a progress file"));
		Path latest = DeckStore.latestExport();
		if (latest != null) {
			pathBox.setValue(latest.toString());
		}
		addRenderableWidget(pathBox);
		setFocused(pathBox);
		addRenderableWidget(Button.builder(Component.literal("Import file"), button -> importFile())
			.bounds(center - 150, 84, 300, 20)
			.build());
		addRenderableWidget(Button.builder(Component.literal("Import clipboard"), button -> importClipboard())
			.bounds(center - 150, 108, 300, 20)
			.build());
		addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
			.bounds(center - 100, this.height - 36, 200, 20)
			.build());
	}

	private void importFile() {
		String raw = pathBox.getValue().trim();
		if (raw.startsWith("\"") && raw.endsWith("\"") && raw.length() >= 2) {
			raw = raw.substring(1, raw.length() - 1).trim();
		}
		if (raw.isEmpty()) {
			notice = "Enter a file path.";
			return;
		}
		Path path = Path.of(raw);
		if (!path.isAbsolute()) {
			path = DeckStore.directory().resolve(path);
		}
		path = path.normalize();
		if (!Files.isRegularFile(path)) {
			notice = "That path is not a file.";
			return;
		}
		try {
			accept(Files.readString(path));
		} catch (IOException exception) {
			VocabCraft.LOGGER.warn("Could not read Vocab Craft progress from {}", path, exception);
			notice = "Could not read that file.";
		}
	}

	private void importClipboard() {
		String text = this.minecraft.keyboardHandler.getClipboard();
		if (text == null || text.isBlank()) {
			notice = "Clipboard is empty.";
			return;
		}
		accept(text);
	}

	private void accept(String text) {
		try {
			DeckStore.get().replace(DeckFile.read(text));
		} catch (DeckFormatException exception) {
			notice = exception.getMessage();
			return;
		}
		DeckStore deck = DeckStore.get();
		if (deck.targetLanguage() == null) {
			deck.setTargetLanguage(this.minecraft.getLanguageManager().getSelected());
		} else {
			GameLanguage.useAsGameLanguage(this.minecraft, deck.targetLanguage());
		}
		int cards = deck.cardCount();
		this.minecraft.setScreenAndShow(new LanguageScreen(this.parent, "Imported " + cards + " cards.", LanguageScreen.SUCCESS));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		int center = this.width / 2;
		graphics.centeredText(this.font, this.title, center, 20, 0xFFFFFFFF);
		graphics.centeredText(this.font, Component.literal("Replaces the current deck and both languages."), center, 40, 0xFFB0B0B0);
		if (!notice.isEmpty()) {
			graphics.centeredText(this.font, Component.literal(notice), center, 140, ERROR);
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		this.extractMenuBackground(graphics);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreenAndShow(new LanguageScreen(this.parent));
	}
}
