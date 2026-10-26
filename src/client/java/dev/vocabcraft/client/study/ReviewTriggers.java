package dev.vocabcraft.client.study;

import dev.vocabcraft.client.srs.DeckStore;
import dev.vocabcraft.client.ui.ReviewScreen;
import dev.vocabcraft.srs.ReviewMath;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class ReviewTriggers {
	private static final long REPEAT_GAP_MS = 2_000L;

	private static String lastOfferedId = "";
	private static long lastOfferedAt;
	private static PendingUse pendingUse;

	private ReviewTriggers() {
	}

	public static void onBroken(BlockState state) {
		offer(state.getBlock().asItem());
	}

	public static void beforeUse(LocalPlayer player, InteractionHand hand, BlockHitResult hit) {
		Minecraft client = Minecraft.getInstance();
		if (client.level == null || player != client.player) {
			pendingUse = null;
			return;
		}
		BlockPos clicked = hit.getBlockPos();
		BlockState beforeClicked = client.level.getBlockState(clicked);
		BlockPos placePos = beforeClicked.canBeReplaced() ? clicked : clicked.relative(hit.getDirection());
		pendingUse = new PendingUse(
			player.getItemInHand(hand).copy(),
			clicked,
			beforeClicked,
			placePos,
			client.level.getBlockState(placePos),
			client.gui.screen() != null
		);
	}

	public static void afterUse(InteractionResult result) {
		PendingUse pending = pendingUse;
		pendingUse = null;
		if (pending == null || pending.screenWasOpen() || result == null || !result.consumesAction()) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.level == null) {
			return;
		}
		ItemStack held = pending.held();
		if (held.getItem() instanceof BlockItem blockItem) {
			Block placed = blockItem.getBlock();
			BlockState afterPlace = client.level.getBlockState(pending.placePos());
			if (afterPlace.getBlock() == placed && pending.beforePlace().getBlock() != placed) {
				offer(placed.asItem(), true);
				return;
			}
		}
		BlockState afterClicked = client.level.getBlockState(pending.clicked());
		if (afterClicked.getBlock() == pending.beforeClicked().getBlock()) {
			// A furnace or crafting table opens its screen inside the use. That screen is this use.
			offer(pending.beforeClicked().getBlock().asItem(), true);
		}
	}

	public static void offer(Item item) {
		offer(item, false);
	}

	private static void offer(Item item, boolean fromThisUse) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || item == null || item == Items.AIR) {
			return;
		}
		if (!fromThisUse && client.gui.screen() != null) {
			return;
		}
		DeckStore deck = DeckStore.get();
		if (!deck.hasLanguages()) {
			return;
		}
		Identifier key = BuiltInRegistries.ITEM.getKey(item);
		if (key == null) {
			return;
		}
		String id = key.toString();
		long now = System.currentTimeMillis();
		if (id.equals(lastOfferedId) && now - lastOfferedAt < REPEAT_GAP_MS) {
			return;
		}
		if (!ReviewMath.isDue(deck.card(id), now)) {
			return;
		}
		lastOfferedId = id;
		lastOfferedAt = now;
		client.setScreenAndShow(new ReviewScreen(item, id));
	}

	private record PendingUse(
		ItemStack held,
		BlockPos clicked,
		BlockState beforeClicked,
		BlockPos placePos,
		BlockState beforePlace,
		boolean screenWasOpen
	) {
	}
}
