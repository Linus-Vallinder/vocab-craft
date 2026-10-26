package dev.vocabcraft.client.mixin;

import dev.vocabcraft.client.lang.GameLanguage;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.resources.language.LanguageManager;

@Mixin(LanguageManager.class)
public class LanguageManagerMixin {
	@Inject(method = "setSelected", at = @At("RETURN"))
	private void vocabcraft$followGameLanguage(String code, CallbackInfo callback) {
		GameLanguage.followGameLanguage(code);
	}
}
