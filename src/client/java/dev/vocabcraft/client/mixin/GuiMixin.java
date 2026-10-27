package dev.vocabcraft.client.mixin;

import dev.vocabcraft.client.study.ReviewTriggers;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;

@Mixin(Gui.class)
public class GuiMixin {
	@Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
	private void vocabcraft$holdBlockScreen(Screen screen, CallbackInfo callback) {
		if (ReviewTriggers.holdIncomingScreen(screen)) {
			callback.cancel();
			return;
		}
		Screen deferred = ReviewTriggers.releaseIfClosingReview(screen);
		if (deferred != null) {
			callback.cancel();
			((Gui) (Object) this).setScreen(deferred);
		}
	}
}
