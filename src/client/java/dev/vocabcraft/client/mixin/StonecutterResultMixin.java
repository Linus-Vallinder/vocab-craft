package dev.vocabcraft.client.mixin;

import dev.vocabcraft.client.study.ReviewTriggers;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@Mixin(targets = "net.minecraft.world.inventory.StonecutterMenu$2")
public class StonecutterResultMixin {
	@Inject(method = "onTake", at = @At("RETURN"))
	private void vocabcraft$cut(Player player, ItemStack stack, CallbackInfo callback) {
		ReviewTriggers.onCrafted(stack);
	}
}
