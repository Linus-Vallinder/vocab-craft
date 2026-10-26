package dev.vocabcraft.client.study;

import dev.vocabcraft.client.srs.DeckStore;
import dev.vocabcraft.client.ui.ReviewScreen;
import dev.vocabcraft.srs.ReviewMath;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
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
	private static Item pendingCraft;
	private static final ArrayDeque<Item> pendingFinds = new ArrayDeque<>();
	private static Set<String> knownInInventory;

	private ReviewTriggers() {
	}

	public static void onBroken(BlockState state) {
		offer(state.getBlock().asItem());
	}

	public static void onCrafted(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return;
		}
		pendingCraft = stack.getItem();
	}

	public static void onPickedUp(int playerId, int itemEntityId) {
		Minecraft client = Minecraft.getInstance();
		if (client.level == null || client.player == null || client.player.getId() != playerId) {
			return;
		}
		Entity entity = client.level.getEntity(itemEntityId);
		if (entity instanceof ItemEntity itemEntity) {
			enqueueFind(itemEntity.getItem().getItem());
		}
	}

	public static void endTick() {
		presentCraft();
		scanInventory();
		presentFind();
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
		if (!accepts(item)) {
			return;
		}
		show(item);
	}

	private static void presentCraft() {
		Item item = pendingCraft;
		if (item == null) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		Screen screen = client.gui.screen();
		if (screen != null && !(screen instanceof AbstractContainerScreen<?>)) {
			return;
		}
		pendingCraft = null;
		offer(item, true);
	}

	private static void presentFind() {
		Minecraft client = Minecraft.getInstance();
		if (client.gui.screen() != null || pendingFinds.isEmpty()) {
			return;
		}
		if (!DeckStore.get().hasLanguages()) {
			return;
		}
		Item item = pendingFinds.poll();
		if (!accepts(item)) {
			remember(item);
			return;
		}
		show(item);
	}

	private static void scanInventory() {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			knownInInventory = null;
			pendingFinds.clear();
			return;
		}
		Set<String> now = idsInInventory(client.player);
		if (knownInInventory == null) {
			knownInInventory = now;
			return;
		}
		if (client.gui.screen() != null || !DeckStore.get().hasLanguages()) {
			return;
		}
		knownInInventory.retainAll(now);
		if (!lastOfferedId.isEmpty()) {
			knownInInventory.add(lastOfferedId);
		}
		for (String id : now) {
			if (knownInInventory.contains(id)) {
				continue;
			}
			Item item = itemFromId(id);
			if (item == null || !accepts(item)) {
				knownInInventory.add(id);
				continue;
			}
			enqueueFind(item);
			return;
		}
	}

	private static void enqueueFind(Item item) {
		if (item == null || item == Items.AIR || pendingFinds.contains(item) || pendingFinds.size() >= 8) {
			return;
		}
		pendingFinds.add(item);
	}

	private static boolean accepts(Item item) {
		if (item == null || item == Items.AIR) {
			return false;
		}
		DeckStore deck = DeckStore.get();
		if (!deck.hasLanguages()) {
			return false;
		}
		String id = idOf(item);
		if (id == null) {
			return false;
		}
		long now = System.currentTimeMillis();
		if (id.equals(lastOfferedId) && now - lastOfferedAt < REPEAT_GAP_MS) {
			return false;
		}
		return ReviewMath.isDue(deck.card(id), now);
	}

	private static void show(Item item) {
		Minecraft client = Minecraft.getInstance();
		String id = idOf(item);
		if (id == null) {
			return;
		}
		if (client.gui.screen() instanceof AbstractContainerScreen<?> && client.player != null) {
			client.player.closeContainer();
		}
		lastOfferedId = id;
		lastOfferedAt = System.currentTimeMillis();
		remember(item);
		client.setScreenAndShow(new ReviewScreen(item, id));
	}

	private static void remember(Item item) {
		String id = idOf(item);
		if (id != null && knownInInventory != null) {
			knownInInventory.add(id);
		}
	}

	private static Set<String> idsInInventory(LocalPlayer player) {
		Set<String> ids = new HashSet<>();
		var inventory = player.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.isEmpty()) {
				continue;
			}
			String id = idOf(stack.getItem());
			if (id != null) {
				ids.add(id);
			}
		}
		return ids;
	}

	private static Item itemFromId(String id) {
		Identifier key = Identifier.tryParse(id);
		if (key == null) {
			return null;
		}
		return BuiltInRegistries.ITEM.get(key).map(reference -> reference.value()).orElse(null);
	}

	private static String idOf(Item item) {
		Identifier key = BuiltInRegistries.ITEM.getKey(item);
		return key == null ? null : key.toString();
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
