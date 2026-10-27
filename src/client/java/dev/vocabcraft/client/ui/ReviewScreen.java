package dev.vocabcraft.client.ui;

import dev.vocabcraft.client.lang.ItemNames;
import dev.vocabcraft.client.srs.DeckStore;
import dev.vocabcraft.client.study.ReviewTriggers;
import dev.vocabcraft.srs.ReviewMath;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ReviewScreen extends Screen {
	private final Item item;
	private final EntityType<?> mob;
	private final String itemId;
	private final ItemStack stack;
	private final Screen parent;
	private boolean flipped;

	public ReviewScreen(Item item, String itemId) {
		this(item, null, itemId, null);
	}

	public ReviewScreen(Item item, String itemId, Screen parent) {
		this(item, null, itemId, parent);
	}

	public ReviewScreen(EntityType<?> mob, String itemId, Item icon) {
		this(icon, mob, itemId, null);
	}

	public ReviewScreen(EntityType<?> mob, String itemId, Item icon, Screen parent) {
		this(icon, mob, itemId, parent);
	}

	private ReviewScreen(Item icon, EntityType<?> mob, String itemId, Screen parent) {
		super(Component.literal("Vocab Craft"));
		this.item = icon;
		this.mob = mob;
		this.itemId = itemId;
		this.parent = parent;
		this.stack = componentsBound(icon) ? icon.getDefaultInstance() : ItemStack.EMPTY;
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
		deck.dequeue(itemId);
		ReviewTriggers.advance(this.parent);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreenAndShow(this.parent);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		if (!this.stack.isEmpty()) {
			int left = this.width / 2 - 32;
			int top = this.height / 2 - 90;
			graphics.pose().pushMatrix();
			graphics.pose().translate(left, top);
			graphics.pose().scale(4.0F, 4.0F);
			graphics.item(this.stack, 0, 0);
			graphics.pose().popMatrix();
		}

		DeckStore deck = DeckStore.get();
		int center = this.width / 2;
		if (!flipped) {
			String target = ItemNames.languageLabel(deck.targetLanguage());
			graphics.centeredText(this.font, Component.literal("What is this in " + target + "?"), center, this.height / 2 - 8, 0xFFFFFFFF);
			return;
		}
		graphics.centeredText(this.font, Component.literal(name(deck.targetLanguage())), center, this.height / 2 - 8, 0xFFFFFFFF);
		graphics.centeredText(this.font, Component.literal(name(deck.supportLanguage())), center, this.height / 2 + 10, 0xFFB0B0B0);
	}

	private static boolean componentsBound(Item icon) {
		if (icon == null || icon == Items.AIR) {
			return false;
		}
		Identifier id = BuiltInRegistries.ITEM.getKey(icon);
		return id != null && BuiltInRegistries.ITEM.get(id).map(holder -> holder.areComponentsBound()).orElse(false);
	}

	private String name(String code) {
		if (this.mob != null) {
			return ItemNames.entityName(this.mob, code);
		}
		return ItemNames.itemName(this.item, code);
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
