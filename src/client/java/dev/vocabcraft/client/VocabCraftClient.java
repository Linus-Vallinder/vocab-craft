package dev.vocabcraft.client;

import dev.vocabcraft.client.srs.DeckStore;
import dev.vocabcraft.client.study.ReviewTriggers;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class VocabCraftClient implements ClientModInitializer {
	public static final KeyMapping REVIEW = KeyMappingHelper.registerKeyMapping(new KeyMapping(
		"key.vocabcraft.review",
		InputConstants.Type.KEYBOARD,
		InputConstants.KEY_V,
		KeyMapping.Category.MISC
	));

	@Override
	public void onInitializeClient() {
		ClientPlayerBlockBreakEvents.AFTER.register((level, player, pos, state) -> ReviewTriggers.onBroken(state));
		ClientTickEvents.END_CLIENT_TICK.register(client -> ReviewTriggers.endTick());
		HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, Identifier.fromNamespaceAndPath("vocab-craft", "queue"), (graphics, delta) -> {
			int waiting = DeckStore.get().queueSize();
			if (waiting == 0) {
				return;
			}
			Minecraft client = Minecraft.getInstance();
			String label = "Vocab " + waiting;
			int x = graphics.guiWidth() / 2 - client.font.width(label) / 2;
			int y = graphics.guiHeight() - 68;
			graphics.text(client.font, label, x, y, 0xFFFFD27A, true);
		});
	}
}
