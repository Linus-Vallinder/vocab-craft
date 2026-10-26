package dev.vocabcraft.client.mixin;

import dev.vocabcraft.client.study.ReviewTriggers;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
	@Inject(method = "handleTakeItemEntity", at = @At("HEAD"))
	private void vocabcraft$pickup(ClientboundTakeItemEntityPacket packet, CallbackInfo callback) {
		ReviewTriggers.onPickedUp(packet.getPlayerId(), packet.getItemId());
	}
}
