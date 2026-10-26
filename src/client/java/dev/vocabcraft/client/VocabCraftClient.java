package dev.vocabcraft.client;

import dev.vocabcraft.client.study.ReviewTriggers;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;

public final class VocabCraftClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayerBlockBreakEvents.AFTER.register((level, player, pos, state) -> ReviewTriggers.onBroken(state));
	}
}
