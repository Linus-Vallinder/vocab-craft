package dev.vocabcraft.client.mixin;

import dev.vocabcraft.client.srs.DeckStore;
import dev.vocabcraft.client.ui.LanguagePickerScreen;
import dev.vocabcraft.client.ui.LanguageScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
	protected TitleScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("RETURN"))
	private void vocabcraft$init(CallbackInfo callback) {
		Minecraft client = Minecraft.getInstance();
		DeckStore deck = DeckStore.get();
		if (!deck.hasLanguages()) {
			LanguagePickerScreen.Role missing = deck.targetLanguage() == null
				? LanguagePickerScreen.Role.TARGET
				: LanguagePickerScreen.Role.SUPPORT;
			client.execute(() -> {
				if (client.gui.screen() instanceof TitleScreen) {
					client.setScreenAndShow(new LanguagePickerScreen(missing, true));
				}
			});
			return;
		}
		int x = this.width / 2 + 104;
		int y = this.height / 4 + 48;
		int buttonWidth = 98;
		if (x + buttonWidth > this.width - 4) {
			x = this.width / 2 - 100;
			y = this.height - 52;
			buttonWidth = 200;
		}
		int placedWidth = buttonWidth;
		this.addRenderableWidget(new Button.Builder(Component.literal("Vocab Craft"), button ->
			client.setScreenAndShow(new LanguageScreen())
		).bounds(x, y, placedWidth, 20).build());
	}
}
