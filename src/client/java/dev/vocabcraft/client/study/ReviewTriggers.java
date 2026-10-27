package dev.vocabcraft.client.study;

import dev.vocabcraft.client.VocabCraftClient;
import dev.vocabcraft.client.lang.ItemNames;
import dev.vocabcraft.client.srs.DeckStore;
import dev.vocabcraft.client.ui.ReviewScreen;
import dev.vocabcraft.srs.ReviewMath;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class ReviewTriggers {
	private static final long REPEAT_GAP_MS = 2_000L;

	private static final int HOLD_TICKS = 40;

	private static String lastOfferedId = "";
	private static long lastOfferedAt;
	private static PendingUse pendingUse;
	private static boolean holdBlockScreen;
	private static int holdTicks;
	private static Screen deferredScreen;
	private static Item pendingCraft;
	private static final ArrayDeque<Item> pendingFinds = new ArrayDeque<>();
	private static Set<String> knownInInventory;
	private static String pendingNotice;
	private static long lastNoticeAt;
	private static boolean reviewWasDown;

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
		tickHold();
		presentCraft();
		scanInventory();
		presentFind();
		flushNotice();
		pollReviewKey();
	}

	public static void openSession(Screen parent) {
		if (DeckStore.get().queueSize() == 0) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.player != null && client.gui.screen() instanceof AbstractContainerScreen<?>) {
			client.player.closeContainer();
		}
		advance(parent);
	}

	public static void advance(Screen parent) {
		Minecraft client = Minecraft.getInstance();
		DeckStore deck = DeckStore.get();
		while (deck.peekQueue() != null) {
			String id = deck.peekQueue();
			ReviewScreen screen = screenFor(id, parent);
			if (screen == null) {
				deck.dequeue(id);
				continue;
			}
			client.setScreenAndShow(screen);
			return;
		}
		client.setScreenAndShow(parent);
	}

	/** @return true when {@code incoming} must stay hidden until the card closes. Showing it would run {@code removed()} and close the menu. */
	public static boolean holdIncomingScreen(Screen incoming) {
		if (!holdBlockScreen || incoming == null || incoming instanceof ReviewScreen) {
			return false;
		}
		Minecraft client = Minecraft.getInstance();
		if (!(client.gui.screen() instanceof ReviewScreen)) {
			return false;
		}
		deferredScreen = incoming;
		holdBlockScreen = false;
		return true;
	}

	/** The screen to open when the card closes, or null to close normally. */
	public static Screen releaseIfClosingReview(Screen incoming) {
		if (incoming != null || deferredScreen == null) {
			return null;
		}
		Minecraft client = Minecraft.getInstance();
		if (!(client.gui.screen() instanceof ReviewScreen)) {
			return null;
		}
		Screen next = deferredScreen;
		deferredScreen = null;
		holdBlockScreen = false;
		return next;
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
			offer(pending.beforeClicked().getBlock().asItem(), true, true);
		}
	}

	public static void onMob(Player player, Entity entity, boolean holdFollowingScreen) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || player != client.player || !(entity instanceof Mob)) {
			return;
		}
		if (!holdFollowingScreen && client.gui.screen() != null) {
			return;
		}
		String id = idOf(entity.getType());
		if (id == null || !acceptsId(id)) {
			return;
		}
		if (StudySettings.get().queueMode()) {
			collect(id, null);
			return;
		}
		Item icon = SpawnEggItem.byId(entity.getType()).map(Holder::value).orElse(null);
		open(client, new ReviewScreen(entity.getType(), id, icon), id, holdFollowingScreen);
	}

	public static void offer(Item item) {
		offer(item, false);
	}

	private static void offer(Item item, boolean fromThisUse) {
		offer(item, fromThisUse, false);
	}

	private static void offer(Item item, boolean fromThisUse, boolean holdFollowingScreen) {
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
		show(item, holdFollowingScreen);
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
		String id = idOf(item);
		return id != null && acceptsId(id);
	}

	private static boolean acceptsId(String id) {
		DeckStore deck = DeckStore.get();
		if (!deck.hasLanguages()) {
			return false;
		}
		long now = System.currentTimeMillis();
		if (id.equals(lastOfferedId) && now - lastOfferedAt < REPEAT_GAP_MS) {
			return false;
		}
		return ReviewMath.isDue(deck.card(id), now);
	}

	private static void show(Item item) {
		show(item, false);
	}

	private static void show(Item item, boolean holdFollowingScreen) {
		Minecraft client = Minecraft.getInstance();
		String id = idOf(item);
		if (id == null) {
			return;
		}
		if (StudySettings.get().queueMode()) {
			collect(id, item);
			return;
		}
		remember(item);
		open(client, new ReviewScreen(item, id), id, holdFollowingScreen);
	}

	private static void collect(String id, Item item) {
		StudySettings settings = StudySettings.get();
		DeckStore deck = DeckStore.get();
		boolean introduced = deck.enqueue(id, System.currentTimeMillis(), settings.maxPending(), settings.dailyNew());
		lastOfferedId = id;
		lastOfferedAt = System.currentTimeMillis();
		if (item != null && deck.queued(id)) {
			remember(item);
		}
		if (introduced) {
			pendingNotice = ItemNames.newLine(id, deck.targetLanguage(), deck.supportLanguage());
		}
	}

	private static void open(Minecraft client, ReviewScreen review, String id, boolean holdFollowingScreen) {
		if (!holdFollowingScreen && client.gui.screen() instanceof AbstractContainerScreen<?> && client.player != null) {
			client.player.closeContainer();
		}
		lastOfferedId = id;
		lastOfferedAt = System.currentTimeMillis();
		if (holdFollowingScreen) {
			holdBlockScreen = true;
			holdTicks = HOLD_TICKS;
			deferredScreen = null;
		}
		client.setScreenAndShow(review);
	}

	private static void tickHold() {
		if (!holdBlockScreen) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (!(client.gui.screen() instanceof ReviewScreen) || --holdTicks <= 0) {
			holdBlockScreen = false;
		}
	}

	private static void flushNotice() {
		if (pendingNotice == null) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}
		StudySettings settings = StudySettings.get();
		if (settings.suppressed(client.player)) {
			return;
		}
		long now = System.currentTimeMillis();
		if (now - lastNoticeAt < settings.cooldownMillis()) {
			return;
		}
		Component line = Component.literal(pendingNotice);
		if (settings.actionBar()) {
			client.player.sendOverlayMessage(line);
		} else {
			client.player.sendSystemMessage(line);
		}
		pendingNotice = null;
		lastNoticeAt = now;
	}

	private static void pollReviewKey() {
		Minecraft client = Minecraft.getInstance();
		Screen screen = client.gui.screen();
		if (screen instanceof ReviewScreen) {
			reviewWasDown = true;
			return;
		}
		if (screen == null) {
			boolean pressed = false;
			while (VocabCraftClient.REVIEW.consumeClick()) {
				pressed = true;
			}
			if (pressed) {
				openSession(null);
			}
			reviewWasDown = VocabCraftClient.REVIEW.isDown();
			return;
		}
		if (screen.getFocused() instanceof EditBox) {
			return;
		}
		if (!(screen instanceof AbstractContainerScreen<?>) && !(screen instanceof PauseScreen)) {
			return;
		}
		boolean down = reviewKeyDown();
		if (down && !reviewWasDown) {
			openSession(screen instanceof PauseScreen ? screen : null);
		}
		reviewWasDown = down;
	}

	private static boolean reviewKeyDown() {
		var key = KeyMappingHelper.getBoundKeyOf(VocabCraftClient.REVIEW);
		if (key.getType() != InputConstants.Type.KEYBOARD) {
			return false;
		}
		return InputConstants.isKeyDown(key.getValue());
	}

	private static ReviewScreen screenFor(String id, Screen parent) {
		if (id.startsWith("entity:")) {
			Identifier key = Identifier.tryParse(id.substring("entity:".length()));
			if (key == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(key)) {
				return null;
			}
			EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(key);
			Item icon = SpawnEggItem.byId(type).map(Holder::value).orElse(null);
			return new ReviewScreen(type, id, icon, parent);
		}
		Item item = itemFromId(id);
		if (item == null || item == Items.AIR) {
			return null;
		}
		return new ReviewScreen(item, id, parent);
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

	private static String idOf(EntityType<?> type) {
		Identifier key = EntityType.getKey(type);
		return key == null ? null : "entity:" + key;
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
