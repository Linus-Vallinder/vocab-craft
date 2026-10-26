package dev.vocabcraft.client.ui;

import dev.vocabcraft.client.lang.GameLanguage;
import dev.vocabcraft.client.srs.DeckStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class LanguagePickerScreen extends Screen {
	public enum Role {
		TARGET("Target language"),
		SUPPORT("Support language");

		final String title;

		Role(String title) {
			this.title = title;
		}
	}

	private final Role role;
	private final boolean required;
	private LanguageList list;
	private String notice = "";

	public LanguagePickerScreen(Role role, boolean required) {
		super(Component.literal(role.title));
		this.role = role;
		this.required = required;
	}

	@Override
	protected void init() {
		HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
		LinearLayout header = LinearLayout.vertical().spacing(4);
		header.defaultCellSetting().alignHorizontallyCenter();
		header.addChild(new StringWidget(this.title, this.font));
		EditBox search = new EditBox(this.font, 0, 0, 200, 15, Component.empty());
		search.setHint(Component.translatable("gui.language.search").withStyle(EditBox.SEARCH_HINT_STYLE));
		search.setResponder(query -> {
			if (this.list != null) {
				this.list.filter(query);
			}
		});
		header.addChild(search);
		layout.addToHeader(header);
		layout.setHeaderHeight(36);

		this.list = new LanguageList(this.minecraft, this.width, this.height);
		layout.addToContents(this.list);

		layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, button -> confirm()).width(200).build());
		layout.visitWidgets(this::addRenderableWidget);
		layout.arrangeElements();
		this.list.updateSize(this.width, layout);
		this.list.fill(currentCode());
		setInitialFocus(search);
	}

	private String currentCode() {
		DeckStore deck = DeckStore.get();
		String chosen = this.role == Role.TARGET ? deck.targetLanguage() : deck.supportLanguage();
		return chosen == null ? this.minecraft.getLanguageManager().getSelected() : chosen;
	}

	private void confirm() {
		LanguageList.Entry selected = this.list.getSelected();
		if (selected == null) {
			this.notice = "Choose a language.";
			return;
		}
		DeckStore deck = DeckStore.get();
		String other = this.role == Role.TARGET ? deck.supportLanguage() : deck.targetLanguage();
		if (selected.code.equals(other)) {
			this.notice = "Target and support languages have to differ.";
			return;
		}
		if (this.role == Role.TARGET) {
			deck.setTargetLanguage(selected.code);
			GameLanguage.useAsGameLanguage(this.minecraft, selected.code);
		} else {
			deck.setSupportLanguage(selected.code);
		}
		if (this.required && this.role == Role.TARGET) {
			this.minecraft.setScreenAndShow(new LanguagePickerScreen(Role.SUPPORT, true));
			return;
		}
		if (this.required) {
			this.minecraft.setScreenAndShow(new TitleScreen());
			return;
		}
		this.minecraft.setScreenAndShow(new LanguageScreen());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		if (!this.notice.isEmpty()) {
			graphics.centeredText(this.font, Component.literal(this.notice), this.width / 2, this.height - 62, 0xFFFFAAAA);
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		this.extractMenuBackground(graphics);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return !this.required;
	}

	@Override
	public void onClose() {
		if (!this.required) {
			this.minecraft.setScreenAndShow(new LanguageScreen());
		}
	}

	private final class LanguageList extends ObjectSelectionList<LanguageList.Entry> {
		private LanguageList(Minecraft minecraft, int width, int height) {
			super(minecraft, width, height - 33 - 53, 33, 18);
		}

		private void fill(String selectedCode) {
			filter("");
			for (Entry entry : children()) {
				if (entry.code.equals(selectedCode)) {
					setSelected(entry);
					centerScrollOn(entry);
					break;
				}
			}
		}

		private void filter(String query) {
			String selected = getSelected() == null ? null : getSelected().code;
			String needle = query.toLowerCase(Locale.ROOT);
			List<Entry> matches = new ArrayList<>();
			for (Map.Entry<String, LanguageInfo> language : minecraft.getLanguageManager().getLanguages().entrySet()) {
				LanguageInfo info = language.getValue();
				String name = info.name() == null ? "" : info.name();
				String region = info.region() == null ? "" : info.region();
				if (needle.isEmpty()
					|| name.toLowerCase(Locale.ROOT).contains(needle)
					|| region.toLowerCase(Locale.ROOT).contains(needle)
					|| language.getKey().toLowerCase(Locale.ROOT).contains(needle)) {
					matches.add(new Entry(language.getKey(), info));
				}
			}
			replaceEntries(matches);
			refreshScrollAmount();
			if (selected == null) {
				return;
			}
			for (Entry entry : children()) {
				if (entry.code.equals(selected)) {
					setSelected(entry);
					break;
				}
			}
		}

		@Override
		public int getRowWidth() {
			return super.getRowWidth() + 50;
		}

		private final class Entry extends ObjectSelectionList.Entry<Entry> {
			private final String code;
			private final LanguageInfo info;

			private Entry(String code, LanguageInfo info) {
				this.code = code;
				this.info = info;
			}

			@Override
			public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
				int textY = getContentY() + (getContentHeight() - 9) / 2;
				graphics.text(font, this.info.toComponent(), getContentX(), textY, 0xFFFFFFFF);
				String region = this.info.region() == null ? "" : this.info.region();
				graphics.text(font, region, getContentRight() - font.width(region), textY, 0xFF808080);
			}

			@Override
			public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
				LanguageList.this.setSelected(this);
				if (doubleClick) {
					confirm();
				}
				return true;
			}

			@Override
			public Component getNarration() {
				return this.info.toComponent();
			}
		}
	}
}
