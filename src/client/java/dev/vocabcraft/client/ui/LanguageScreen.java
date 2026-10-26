package dev.vocabcraft.client.ui;

import dev.vocabcraft.VocabCraft;
import dev.vocabcraft.client.lang.ItemNames;
import dev.vocabcraft.client.srs.DeckStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class LanguageScreen extends Screen {
	static final int SUCCESS = 0xFF7DFFC3;
	private static final int ERROR = 0xFFFFAAAA;

	private boolean confirmReset;
	private String notice = "";
	private int noticeColor = ERROR;

	public LanguageScreen() {
		this("", ERROR);
	}

	public LanguageScreen(String notice, int noticeColor) {
		super(Component.literal("Vocab Craft"));
		this.notice = notice;
		this.noticeColor = noticeColor;
	}

	@Override
	protected void init() {
		DeckStore deck = DeckStore.get();
		int center = this.width / 2;
		addRenderableWidget(Button.builder(Component.literal("Target language: " + label(deck.targetLanguage())), button ->
			this.minecraft.setScreenAndShow(new LanguagePickerScreen(LanguagePickerScreen.Role.TARGET, false))
		).bounds(center - 150, 48, 300, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Support language: " + label(deck.supportLanguage())), button ->
			this.minecraft.setScreenAndShow(new LanguagePickerScreen(LanguagePickerScreen.Role.SUPPORT, false))
		).bounds(center - 150, 76, 300, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Export"), button -> export())
			.bounds(center - 150, this.height - 88, 148, 20)
			.build());
		addRenderableWidget(Button.builder(Component.literal("Import"), button -> this.minecraft.setScreenAndShow(new ImportScreen()))
			.bounds(center + 2, this.height - 88, 148, 20)
			.build());
		addRenderableWidget(Button.builder(Component.literal(confirmReset ? "Confirm reset" : "Reset deck"), button -> reset())
			.bounds(center - 100, this.height - 64, 200, 20)
			.build());
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
			.bounds(center - 100, this.height - 36, 200, 20)
			.build());
		int top = notice.isEmpty() ? 108 : 122;
		int panelWidth = Math.min(360, this.width - 20);
		int panelHeight = Math.max(36, this.height - 96 - top);
		addRenderableWidget(new StatsPanel(center - panelWidth / 2, top, panelWidth, panelHeight));
	}

	private void export() {
		try {
			Path file = DeckStore.get().exportFile();
			try {
				this.minecraft.keyboardHandler.setClipboard(Files.readString(file));
				showNotice("Exported " + DeckStore.get().cardCount() + " cards. JSON copied.", SUCCESS);
			} catch (RuntimeException exception) {
				VocabCraft.LOGGER.warn("Could not copy Vocab Craft progress", exception);
				showNotice("Exported " + DeckStore.get().cardCount() + " cards.", SUCCESS);
			}
		} catch (IOException exception) {
			VocabCraft.LOGGER.warn("Could not export Vocab Craft progress", exception);
			showNotice("Could not export the deck.", ERROR);
		}
	}

	private void showNotice(String notice, int color) {
		this.notice = notice;
		this.noticeColor = color;
		rebuildWidgets();
	}

	private static String label(String code) {
		return code == null ? "not chosen" : ItemNames.languageLabel(code);
	}

	private void reset() {
		if (!confirmReset) {
			confirmReset = true;
			rebuildWidgets();
			return;
		}
		DeckStore.get().resetCards();
		confirmReset = false;
		showNotice("Deck cleared.", SUCCESS);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		int center = this.width / 2;
		graphics.centeredText(this.font, this.title, center, 20, 0xFFFFFFFF);
		if (!notice.isEmpty()) {
			graphics.centeredText(this.font, Component.literal(notice), center, 106, noticeColor);
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		this.extractMenuBackground(graphics);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreenAndShow(new TitleScreen());
	}
}
