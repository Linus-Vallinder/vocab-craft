package dev.vocabcraft.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;

import dev.vocabcraft.client.srs.DeckStore;
import dev.vocabcraft.client.study.ReviewTriggers;
import dev.vocabcraft.client.ui.LanguageScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {
	protected PauseScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "createPauseMenu", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/CommonComponents;disconnectButtonLabel(Z)Lnet/minecraft/network/chat/Component;"))
	private void vocabcraft$pauseButton(CallbackInfo callback, @Local GridLayout.RowHelper rows) {
		int waiting = DeckStore.get().queueSize();
		if (waiting > 0) {
			String label = waiting == 1 ? "Review 1" : "Review " + waiting;
			rows.addChild(Button.builder(Component.literal(label), button -> ReviewTriggers.openSession(this)).width(204).build(), 2);
		}
		rows.addChild(Button.builder(Component.literal("Vocab Craft"), button ->
			this.minecraft.setScreenAndShow(new LanguageScreen(this))
		).width(204).build(), 2);
	}
}
