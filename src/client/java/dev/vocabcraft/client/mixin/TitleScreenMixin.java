package dev.vocabcraft.client.mixin;

import dev.vocabcraft.client.lang.GameLanguage;
import dev.vocabcraft.client.srs.DeckStore;
import dev.vocabcraft.client.study.ReviewTriggers;
import dev.vocabcraft.client.ui.LanguagePickerScreen;
import dev.vocabcraft.client.ui.LanguageScreen;

import java.util.HashMap;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
	private int statY = -1;
	private boolean reviewButton;

	protected TitleScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("RETURN"))
	private void vocabcraft$init(CallbackInfo callback) {
		this.statY = -1;
		this.reviewButton = false;
		Minecraft client = Minecraft.getInstance();
		DeckStore deck = DeckStore.get();
		String game = client.getLanguageManager().getSelected();
		if (deck.targetLanguage() == null) {
			deck.setTargetLanguage(game);
		} else if (!deck.targetLanguage().equals(game)) {
			GameLanguage.useAsGameLanguage(client, deck.targetLanguage());
		}
		if (deck.supportLanguage() == null) {
			client.execute(() -> {
				if (client.gui.screen() instanceof TitleScreen) {
					client.setScreenAndShow(new LanguagePickerScreen(LanguagePickerScreen.Role.SUPPORT, true));
				}
			});
			return;
		}
		placeMenuButton(client);
	}

	private void placeMenuButton(Minecraft client) {
		Map<Integer, Integer> countByY = new HashMap<>();
		Map<Integer, Integer> rightByY = new HashMap<>();
		for (GuiEventListener child : this.children()) {
			if (!(child instanceof AbstractWidget widget) || widget.getWidth() != 20 || widget.getHeight() != 20) {
				continue;
			}
			int y = widget.getY();
			countByY.merge(y, 1, Integer::sum);
			rightByY.merge(y, widget.getX() + widget.getWidth(), Math::max);
		}
		int rowY = -1;
		int rowCount = 0;
		for (Map.Entry<Integer, Integer> entry : countByY.entrySet()) {
			if (entry.getValue() > rowCount) {
				rowCount = entry.getValue();
				rowY = entry.getKey();
			}
		}
		if (rowCount < 2) {
			return;
		}
		int lineY = rowY + 48;
		int waiting = DeckStore.get().queueSize();
		int lineHeight = waiting > 0 ? 20 : 10;
		if (lineY + lineHeight <= this.height - 12) {
			this.statY = lineY;
			if (waiting > 0) {
				String label = waiting == 1 ? "Review 1" : "Review " + waiting;
				int buttonWidth = this.font.width(label) + 20;
				this.addRenderableWidget(Button.builder(Component.literal(label), button -> ReviewTriggers.openSession(this))
					.bounds(this.width / 2 - buttonWidth / 2, lineY, buttonWidth, 20)
					.build());
				this.reviewButton = true;
			}
		}
		int x = rightByY.get(rowY) + 4;
		int buttonWidth = 60;
		if (x + buttonWidth > this.width - 4) {
			return;
		}
		this.addRenderableWidget(Button.builder(Component.literal("Vocab"), button ->
			client.setScreenAndShow(new LanguageScreen(this))
		).bounds(x, rowY, buttonWidth, 20).tooltip(Tooltip.create(Component.literal("Vocab Craft"))).build());
	}

	@Inject(method = "extractRenderState", at = @At("RETURN"))
	private void vocabcraft$dueCount(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callback) {
		if (this.statY < 0 || this.reviewButton) {
			return;
		}
		int waiting = DeckStore.get().queueSize();
		String label = waiting == 1 ? "1 review waiting" : waiting + " reviews waiting";
		int textWidth = this.font.width(label);
		int x = this.width / 2 - textWidth / 2;
		graphics.fill(x - 3, this.statY - 2, x + textWidth + 3, this.statY + 10, 0xA0101214);
		graphics.text(this.font, label, x, this.statY, waiting > 0 ? 0xFFFFD27A : 0xFFB0B0B0);
	}
}
