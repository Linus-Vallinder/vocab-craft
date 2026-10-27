package dev.vocabcraft.client.ui;

import dev.vocabcraft.VocabCraft;
import dev.vocabcraft.client.lang.ItemNames;
import dev.vocabcraft.client.srs.DeckStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class LanguageScreen extends Screen {
	static final int SUCCESS = 0xFF7DFFC3;
	private static final int ERROR = 0xFFFFAAAA;

	private final Screen parent;
	private boolean confirmReset;
	private String notice = "";
	private int noticeColor = ERROR;

	public LanguageScreen(Screen parent) {
		this(parent, "", ERROR);
	}

	public LanguageScreen(Screen parent, String notice, int noticeColor) {
		super(Component.literal("Vocab Craft"));
		this.parent = parent;
		this.notice = notice;
		this.noticeColor = noticeColor;
	}

	@Override
	protected void init() {
		DeckStore deck = DeckStore.get();
		int inner = Math.min(300, Math.max(120, this.width - 40));
		int headerHeight = this.notice.isEmpty() ? 72 : 92;
		int footerHeight = 104;
		HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, headerHeight, footerHeight);

		LinearLayout header = LinearLayout.vertical().spacing(4);
		header.defaultCellSetting().alignHorizontallyCenter();
		header.addChild(new StringWidget(this.title, this.font));
		if (!this.notice.isEmpty()) {
			header.addChild(new StringWidget(Component.literal(this.notice).withColor(this.noticeColor & 0xFFFFFF), this.font));
		}
		header.addChild(Button.builder(Component.literal("Target language: " + label(deck.targetLanguage())), button ->
			this.minecraft.setScreenAndShow(new LanguagePickerScreen(LanguagePickerScreen.Role.TARGET, false, this.parent))
		).width(inner).build());
		header.addChild(Button.builder(Component.literal("Support language: " + label(deck.supportLanguage())), button ->
			this.minecraft.setScreenAndShow(new LanguagePickerScreen(LanguagePickerScreen.Role.SUPPORT, false, this.parent))
		).width(inner).build());
		layout.addToHeader(header);

		int panelWidth = Math.min(360, this.width - 20);
		int panelHeight = Math.max(36, this.height - headerHeight - footerHeight);
		StatsPanel stats = new StatsPanel((this.width - panelWidth) / 2, headerHeight, panelWidth, panelHeight);
		layout.addToContents(stats);

		int half = (inner - 4) / 2;
		LinearLayout footer = LinearLayout.vertical().spacing(4);
		footer.defaultCellSetting().alignHorizontallyCenter();
		footer.addChild(Button.builder(Component.literal("Study"), button ->
			this.minecraft.setScreenAndShow(new StudyOptionsScreen(this))
		).width(inner).build());
		LinearLayout fileRow = LinearLayout.horizontal().spacing(4);
		fileRow.addChild(Button.builder(Component.literal("Export"), button -> export()).width(half).build());
		fileRow.addChild(Button.builder(Component.literal("Import"), button -> this.minecraft.setScreenAndShow(new ImportScreen(this.parent))).width(half).build());
		footer.addChild(fileRow);
		footer.addChild(Button.builder(Component.literal(confirmReset ? "Confirm reset" : "Reset deck"), button -> reset()).width(inner).build());
		footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).width(inner).build());
		layout.addToFooter(footer);

		layout.visitWidgets(this::addRenderableWidget);
		layout.arrangeElements();
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
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		this.extractMenuBackground(graphics);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreenAndShow(this.parent == null ? new TitleScreen() : this.parent);
	}
}
