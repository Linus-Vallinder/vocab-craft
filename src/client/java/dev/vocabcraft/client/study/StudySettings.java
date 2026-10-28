package dev.vocabcraft.client.study;

import dev.vocabcraft.VocabCraft;
import dev.vocabcraft.client.srs.DeckStore;
import dev.vocabcraft.srs.DeckFile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.world.entity.player.Player;

public final class StudySettings {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static StudySettings instance;

	private boolean queueMode = true;
	private boolean actionBar = true;
	private int cooldownSeconds = 30;
	private boolean suppressCombat = true;
	private boolean suppressFlying = true;
	private boolean suppressSwimming = true;
	private int dailyNew = 0;
	private int maxPending = 0;
	private boolean reviewInBed = true;
	private int reviewAt = 10;

	public static StudySettings get() {
		if (instance == null) {
			instance = load();
		}
		return instance;
	}

	public boolean queueMode() {
		return queueMode;
	}

	public boolean actionBar() {
		return actionBar;
	}

	public int cooldownSeconds() {
		return cooldownSeconds;
	}

	public long cooldownMillis() {
		return cooldownSeconds * 1_000L;
	}

	public boolean suppressCombat() {
		return suppressCombat;
	}

	public boolean suppressFlying() {
		return suppressFlying;
	}

	public boolean suppressSwimming() {
		return suppressSwimming;
	}

	public int dailyNew() {
		return dailyNew;
	}

	public int maxPending() {
		return maxPending;
	}

	public boolean reviewInBed() {
		return reviewInBed;
	}

	public int reviewAt() {
		return reviewAt;
	}

	public void toggleQueueMode() {
		queueMode = !queueMode;
		save();
	}

	public void toggleActionBar() {
		actionBar = !actionBar;
		save();
	}

	public void toggleSuppressCombat() {
		suppressCombat = !suppressCombat;
		save();
	}

	public void toggleSuppressFlying() {
		suppressFlying = !suppressFlying;
		save();
	}

	public void toggleSuppressSwimming() {
		suppressSwimming = !suppressSwimming;
		save();
	}

	public void setCooldownSeconds(int seconds) {
		cooldownSeconds = Math.max(0, seconds);
		save();
	}

	public void setDailyNew(int limit) {
		dailyNew = Math.max(0, limit);
		save();
	}

	public void setMaxPending(int limit) {
		maxPending = Math.max(0, limit);
		save();
	}

	public void toggleReviewInBed() {
		reviewInBed = !reviewInBed;
		save();
	}

	public void setReviewAt(int cards) {
		reviewAt = Math.max(0, cards);
		save();
	}

	public boolean suppressed(Player player) {
		if (player == null) {
			return false;
		}
		if (suppressCombat && fighting(player)) {
			return true;
		}
		if (suppressFlying && player.getAbilities().flying) {
			return true;
		}
		return suppressSwimming && player.isSwimming();
	}

	private static boolean fighting(Player player) {
		if (player.hurtTime > 0) {
			return true;
		}
		int now = player.tickCount;
		if (player.getLastHurtByMob() != null && recent(now, player.getLastHurtByMobTimestamp())) {
			return true;
		}
		return player.getLastHurtMob() != null && recent(now, player.getLastHurtMobTimestamp());
	}

	private static boolean recent(int now, int then) {
		int delta = now - then;
		return delta >= 0 && delta < 100;
	}

	private static StudySettings load() {
		StudySettings settings = new StudySettings();
		Path path = path();
		if (!Files.isRegularFile(path)) {
			return settings;
		}
		try {
			JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
			settings.queueMode = bool(root, "queueMode", true);
			settings.actionBar = bool(root, "actionBar", true);
			settings.cooldownSeconds = count(root, "cooldownSeconds", 30);
			settings.suppressCombat = bool(root, "suppressCombat", true);
			settings.suppressFlying = bool(root, "suppressFlying", true);
			settings.suppressSwimming = bool(root, "suppressSwimming", true);
			settings.dailyNew = count(root, "dailyNew", 0);
			settings.maxPending = count(root, "maxPending", 0);
			settings.reviewInBed = bool(root, "reviewInBed", true);
			settings.reviewAt = count(root, "reviewAt", 10);
		} catch (RuntimeException | IOException exception) {
			VocabCraft.LOGGER.warn("Could not read Vocab Craft settings, using defaults", exception);
			return new StudySettings();
		}
		return settings;
	}

	private void save() {
		JsonObject root = new JsonObject();
		root.addProperty("queueMode", queueMode);
		root.addProperty("actionBar", actionBar);
		root.addProperty("cooldownSeconds", cooldownSeconds);
		root.addProperty("suppressCombat", suppressCombat);
		root.addProperty("suppressFlying", suppressFlying);
		root.addProperty("suppressSwimming", suppressSwimming);
		root.addProperty("dailyNew", dailyNew);
		root.addProperty("maxPending", maxPending);
		root.addProperty("reviewInBed", reviewInBed);
		root.addProperty("reviewAt", reviewAt);
		try {
			DeckFile.writeAtomically(path(), GSON.toJson(root));
		} catch (IOException exception) {
			VocabCraft.LOGGER.error("Could not save Vocab Craft settings", exception);
		}
	}

	private static boolean bool(JsonObject root, String field, boolean fallback) {
		if (root.get(field) == null || !root.get(field).isJsonPrimitive()) {
			return fallback;
		}
		return root.get(field).getAsBoolean();
	}

	private static int count(JsonObject root, String field, int fallback) {
		if (root.get(field) == null || !root.get(field).isJsonPrimitive() || !root.get(field).getAsJsonPrimitive().isNumber()) {
			return fallback;
		}
		int value = root.get(field).getAsInt();
		return value < 0 ? fallback : value;
	}

	private static Path path() {
		return DeckStore.directory().resolve("settings.json");
	}
}
