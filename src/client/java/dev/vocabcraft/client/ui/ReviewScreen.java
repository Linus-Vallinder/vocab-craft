package dev.vocabcraft.client.ui;

import dev.vocabcraft.client.lang.ItemNames;
import dev.vocabcraft.client.srs.DeckStore;
import dev.vocabcraft.srs.ReviewMath;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ReviewScreen extends Screen {
	private final Item item;
	private final String itemId;
	private final ItemStack stack;
	private boolean flipped;

	public ReviewScreen(Item item, String itemId) {
		super(Component.literal("Vocab Craft"));
		this.item = item;
		this.itemId = itemId;
		this.stack = item.getDefaultInstance();
	}

	@Override
	protected void init() {
		int center = this.width / 2;
		int y = this.height / 2 + 52;
		if (!flipped) {
			addRenderableWidget(new Button.Builder(Component.literal("Flip"), button -> {
				flipped = true;
				rebuildWidgets();
			}).bounds(center - 100, y, 200, 20).build());
			return;
		}
		addRenderableWidget(new Button.Builder(Component.literal("I was right"), button -> grade(true))
			.bounds(center - 154, y, 150, 20)
			.build());
		addRenderableWidget(new Button.Builder(Component.literal("I was wrong"), button -> grade(false))
			.bounds(center + 4, y, 150, 20)
			.build());
	}

	private void grade(boolean correct) {
		DeckStore deck = DeckStore.get();
		deck.put(itemId, ReviewMath.grade(deck.card(itemId), correct, System.currentTimeMillis()));
		this.minecraft.setScreenAndShow(null);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		int left = this.width / 2 - 32;
		int top = this.height / 2 - 90;
		graphics.pose().pushMatrix();
		graphics.pose().translate(left, top);
		graphics.pose().scale(4.0F, 4.0F);
		graphics.item(this.stack, 0, 0);
		graphics.pose().popMatrix();

		DeckStore deck = DeckStore.get();
		int center = this.width / 2;
		if (!flipped) {
			String target = ItemNames.languageLabel(deck.targetLanguage());
			graphics.centeredText(this.font, Component.literal("What is this in " + target + "?"), center, this.height / 2 - 8, 0xFFFFFFFF);
			return;
		}
		graphics.centeredText(this.font, Component.literal(ItemNames.itemName(this.item, deck.targetLanguage())), center, this.height / 2 - 8, 0xFFFFFFFF);
		graphics.centeredText(this.font, Component.literal(ItemNames.itemName(this.item, deck.supportLanguage())), center, this.height / 2 + 10, 0xFFB0B0B0);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		this.extractTransparentBackground(graphics);
	}

	@Override
	public boolean isPauseScreen() {
		return true;
	}
}
