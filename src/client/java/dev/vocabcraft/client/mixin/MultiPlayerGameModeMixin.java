package dev.vocabcraft.client.mixin;

import dev.vocabcraft.client.study.ReviewTriggers;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
	@Inject(method = "performUseItemOn", at = @At("HEAD"))
	private void vocabcraft$beforeUse(LocalPlayer player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) {
		ReviewTriggers.beforeUse(player, hand, hit);
	}

	@Inject(method = "performUseItemOn", at = @At("RETURN"))
	private void vocabcraft$afterUse(LocalPlayer player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) {
		ReviewTriggers.afterUse(callback.getReturnValue());
	}

	@Inject(method = "attack", at = @At("HEAD"))
	private void vocabcraft$attack(Player player, Entity target, CallbackInfo callback) {
		ReviewTriggers.onMob(player, target, false);
	}

	@Inject(method = "interact", at = @At("RETURN"))
	private void vocabcraft$interact(Player player, Entity target, EntityHitResult hit, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
		InteractionResult result = callback.getReturnValue();
		if (result != null && result.consumesAction()) {
			ReviewTriggers.onMob(player, target, true);
		}
	}
}
