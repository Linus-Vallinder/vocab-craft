package dev.vocabcraft.client.srs;

import dev.vocabcraft.VocabCraft;
import dev.vocabcraft.srs.CardState;
import dev.vocabcraft.srs.DeckData;
import dev.vocabcraft.srs.DeckFile;
import dev.vocabcraft.srs.QueueAdmit;
import dev.vocabcraft.srs.ReviewMath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import net.fabricmc.loader.api.FabricLoader;

public final class DeckStore {
	private static DeckStore instance;

	private String targetLanguage;
	private String supportLanguage;
	private final Map<String, CardState> cards = new LinkedHashMap<>();
	private final List<String> queue = new ArrayList<>();
	private String newDay;
	private int newCount;

	public static DeckStore get() {
		if (instance == null) {
			instance = load();
		}
		return instance;
	}

	public boolean hasLanguages() {
		return targetLanguage != null && supportLanguage != null;
	}

	public String targetLanguage() {
		return targetLanguage;
	}

	public String supportLanguage() {
		return supportLanguage;
	}

	public void setTargetLanguage(String targetLanguage) {
		this.targetLanguage = targetLanguage;
		save();
	}

	public void setSupportLanguage(String supportLanguage) {
		this.supportLanguage = supportLanguage;
		save();
	}

	public CardState card(String itemId) {
		return cards.get(itemId);
	}

	public void put(String itemId, CardState card) {
		cards.put(itemId, card);
		save();
	}

	public List<String> queue() {
		return Collections.unmodifiableList(queue);
	}

	public int queueSize() {
		return queue.size();
	}

	public boolean queued(String itemId) {
		return queue.contains(itemId);
	}

	public String peekQueue() {
		return queue.isEmpty() ? null : queue.get(0);
	}

	public void dequeue(String itemId) {
		if (queue.remove(itemId)) {
			save();
		}
	}

	/** @return true when this call introduced the card. */
	public boolean enqueue(String itemId, long now, int maxPending, int dailyLimit) {
		rollDay();
		boolean known = cards.containsKey(itemId);
		QueueAdmit.Result result = QueueAdmit.decide(known, queue.contains(itemId), queue.size(), maxPending, newCount, dailyLimit);
		if (!result.enqueue()) {
			return false;
		}
		if (result.introduce()) {
			cards.put(itemId, ReviewMath.fresh(now));
			newCount++;
		}
		queue.add(itemId);
		save();
		return result.introduce();
	}

	public void resetCards() {
		cards.clear();
		queue.clear();
		newCount = 0;
		save();
	}

	public void replace(DeckData data) {
		apply(data);
		save();
	}

	public DeckData snapshot() {
		return new DeckData(targetLanguage, supportLanguage, new LinkedHashMap<>(cards), new ArrayList<>(queue), newDay, newCount);
	}

	public Path exportFile() throws IOException {
		Path dir = directory().resolve("exports");
		Files.createDirectories(dir);
		String stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss").format(LocalDateTime.now());
		Path file = dir.resolve("vocab-craft-" + stamp + ".json");
		int suffix = 2;
		while (Files.exists(file)) {
			file = dir.resolve("vocab-craft-" + stamp + "-" + suffix + ".json");
			suffix++;
		}
		String json = DeckFile.write(snapshot());
		DeckFile.writeAtomically(file, json);
		return file;
	}

	public static Path directory() {
		return FabricLoader.getInstance().getConfigDir().resolve("vocab-craft");
	}

	public static Path latestExport() {
		Path dir = directory().resolve("exports");
		if (!Files.isDirectory(dir)) {
			return null;
		}
		try (Stream<Path> files = Files.list(dir)) {
			return files
				.filter(path -> path.getFileName().toString().endsWith(".json"))
				.max(Comparator.comparingLong(DeckStore::modifiedAt))
				.orElse(null);
		} catch (IOException exception) {
			return null;
		}
	}

	public Map<String, CardState> cards() {
		return Collections.unmodifiableMap(cards);
	}

	public int cardCount() {
		return cards.size();
	}

	public int dueCount(long now) {
		int due = 0;
		for (CardState card : cards.values()) {
			if (ReviewMath.isDue(card, now)) {
				due++;
			}
		}
		return due;
	}

	public int reviewCount() {
		int reviews = 0;
		for (CardState card : cards.values()) {
			reviews += card.reviews();
		}
		return reviews;
	}

	public int lapseCount() {
		int lapses = 0;
		for (CardState card : cards.values()) {
			lapses += card.lapses();
		}
		return lapses;
	}

	private static DeckStore load() {
		DeckStore deck = new DeckStore();
		Path path = path();
		if (!Files.isRegularFile(path)) {
			return deck;
		}
		try {
			deck.apply(DeckFile.read(Files.readString(path)));
		} catch (IOException | RuntimeException exception) {
			VocabCraft.LOGGER.error("Could not read Vocab Craft deck, starting a new one", exception);
			return new DeckStore();
		}
		return deck;
	}

	private void apply(DeckData data) {
		targetLanguage = data.targetLanguage();
		supportLanguage = data.supportLanguage();
		cards.clear();
		cards.putAll(data.cards());
		queue.clear();
		queue.addAll(data.queue());
		newDay = data.newDay();
		newCount = data.newCount();
	}

	private void rollDay() {
		String today = LocalDate.now().toString();
		if (!today.equals(newDay)) {
			newDay = today;
			newCount = 0;
		}
	}

	private void save() {
		try {
			DeckFile.writeAtomically(path(), DeckFile.write(snapshot()));
		} catch (IOException exception) {
			VocabCraft.LOGGER.error("Could not save Vocab Craft deck", exception);
		}
	}

	private static long modifiedAt(Path path) {
		try {
			return Files.getLastModifiedTime(path).toMillis();
		} catch (IOException exception) {
			return 0L;
		}
	}

	private static Path path() {
		return directory().resolve("deck.json");
	}
}
