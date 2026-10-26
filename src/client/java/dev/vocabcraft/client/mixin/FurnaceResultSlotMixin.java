package dev.vocabcraft.client.mixin;

import dev.vocabcraft.client.study.ReviewTriggers;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.item.ItemStack;

@Mixin(FurnaceResultSlot.class)
public class FurnaceResultSlotMixin {
	@Inject(method = "onTake", at = @At("RETURN"))
	private void vocabcraft$smelted(Player player, ItemStack stack, CallbackInfo callback) {
		ReviewTriggers.onCrafted(stack);
	}
}
