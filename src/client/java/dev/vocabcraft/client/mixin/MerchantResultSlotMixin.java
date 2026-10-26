package dev.vocabcraft.client.mixin;

import dev.vocabcraft.client.study.ReviewTriggers;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.item.ItemStack;

@Mixin(MerchantResultSlot.class)
public class MerchantResultSlotMixin {
	@Inject(method = "onTake", at = @At("RETURN"))
	private void vocabcraft$traded(Player player, ItemStack stack, CallbackInfo callback) {
		ReviewTriggers.onCrafted(stack);
	}
}
