package dev.vocabcraft.srs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DeckFile {
	public static final int FORMAT = 1;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final long MAX_EXACT_LONG = 9_007_199_254_740_992L;

	private DeckFile() {
	}

	public static String write(DeckData data) {
		JsonObject root = new JsonObject();
		root.addProperty("format", FORMAT);
		if (data.targetLanguage() != null) {
			root.addProperty("targetLanguage", data.targetLanguage());
		}
		if (data.supportLanguage() != null) {
			root.addProperty("supportLanguage", data.supportLanguage());
		}
		JsonObject cards = new JsonObject();
		for (Map.Entry<String, CardState> entry : data.cards().entrySet()) {
			CardState card = entry.getValue();
			JsonObject json = new JsonObject();
			json.addProperty("ease", card.ease());
			json.addProperty("reps", card.reps());
			json.addProperty("intervalMs", card.intervalMs());
			json.addProperty("dueAt", card.dueAt());
			json.addProperty("reviews", card.reviews());
			json.addProperty("lapses", card.lapses());
			cards.add(entry.getKey(), json);
		}
		root.add("cards", cards);
		JsonArray queue = new JsonArray();
		for (String id : data.queue()) {
			queue.add(id);
		}
		root.add("queue", queue);
		if (data.newDay() != null) {
			root.addProperty("newDay", data.newDay());
		}
		root.addProperty("newCount", data.newCount());
		return GSON.toJson(root);
	}

	public static DeckData read(String text) {
		if (text == null || text.isBlank()) {
			throw new DeckFormatException("Progress file is empty.");
		}
		JsonElement parsed;
		try {
			parsed = JsonParser.parseString(text);
		} catch (RuntimeException exception) {
			throw new DeckFormatException("Progress file is not valid JSON.");
		}
		if (!parsed.isJsonObject()) {
			throw new DeckFormatException("Progress file is not a deck.");
		}
		JsonObject root = parsed.getAsJsonObject();
		formatOf(root);
		JsonElement cardsElement = root.get("cards");
		if (cardsElement == null || !cardsElement.isJsonObject()) {
			throw new DeckFormatException("Progress file has no cards.");
		}
		Map<String, CardState> cards = new LinkedHashMap<>();
		for (Map.Entry<String, JsonElement> entry : cardsElement.getAsJsonObject().entrySet()) {
			String id = entry.getKey();
			if (id.isBlank() || id.length() > 256 || hasWhitespace(id)) {
				throw new DeckFormatException("A card has an invalid item id.");
			}
			if (!entry.getValue().isJsonObject()) {
				throw new DeckFormatException(id + " is not a card.");
			}
			cards.put(id, card(id, entry.getValue().getAsJsonObject()));
		}
		return new DeckData(
			textOrNull(root.get("targetLanguage")),
			textOrNull(root.get("supportLanguage")),
			cards,
			queueOf(root, cards),
			textOrNull(root.get("newDay")),
			optionalCount(root, "newCount")
		);
	}

	public static void writeAtomically(Path path, String text) throws IOException {
		Files.createDirectories(path.getParent());
		Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
		Files.writeString(temporary, text);
		try {
			Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException exception) {
			Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static List<String> queueOf(JsonObject root, Map<String, CardState> cards) {
		JsonElement element = root.get("queue");
		if (element == null || element.isJsonNull()) {
			return List.of();
		}
		if (!element.isJsonArray()) {
			throw new DeckFormatException("Progress queue is invalid.");
		}
		List<String> queue = new ArrayList<>();
		for (JsonElement entry : element.getAsJsonArray()) {
			if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString()) {
				continue;
			}
			String id = entry.getAsString();
			if (!validId(id) || !cards.containsKey(id) || queue.contains(id)) {
				continue;
			}
			queue.add(id);
		}
		return queue;
	}

	private static int optionalCount(JsonObject root, String field) {
		JsonElement element = root.get(field);
		if (element == null || element.isJsonNull()) {
			return 0;
		}
		if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
			throw new DeckFormatException(field + " is invalid.");
		}
		double value = element.getAsDouble();
		if (value != Math.rint(value) || value < 0 || value > Integer.MAX_VALUE) {
			throw new DeckFormatException(field + " is invalid.");
		}
		return (int) value;
	}

	private static boolean validId(String id) {
		return !id.isBlank() && id.length() <= 256 && !hasWhitespace(id);
	}

	private static void formatOf(JsonObject root) {
		JsonElement element = root.get("format");
		if (element == null || element.isJsonNull()) {
			return;
		}
		if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
			throw new DeckFormatException("Progress format is not a number.");
		}
		int format = element.getAsInt();
		if (format != FORMAT) {
			throw new DeckFormatException("This progress file uses format " + format + ", which this version cannot read.");
		}
	}

	private static CardState card(String id, JsonObject json) {
		double ease = number(json, "ease", id);
		if (ease < 0) {
			throw new DeckFormatException(id + " has an invalid ease.");
		}
		return new CardState(
			ease,
			count(json, "reps", id),
			whole(json, "intervalMs", id),
			whole(json, "dueAt", id),
			count(json, "reviews", id),
			count(json, "lapses", id)
		);
	}

	private static double number(JsonObject json, String field, String id) {
		JsonElement element = json.get(field);
		if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
			throw new DeckFormatException(id + " is missing " + field + ".");
		}
		double value = element.getAsDouble();
		if (!Double.isFinite(value)) {
			throw new DeckFormatException(id + " has an invalid " + field + ".");
		}
		return value;
	}

	private static int count(JsonObject json, String field, String id) {
		long value = whole(json, field, id);
		if (value < 0 || value > Integer.MAX_VALUE) {
			throw new DeckFormatException(id + " has an invalid " + field + ".");
		}
		return (int) value;
	}

	private static long whole(JsonObject json, String field, String id) {
		double value = number(json, field, id);
		if (value != Math.rint(value) || value < -MAX_EXACT_LONG || value > MAX_EXACT_LONG) {
			throw new DeckFormatException(id + " has an invalid " + field + ".");
		}
		return (long) value;
	}

	private static String textOrNull(JsonElement element) {
		if (element == null || element.isJsonNull()) {
			return null;
		}
		if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
			throw new DeckFormatException("A language code is invalid.");
		}
		String text = element.getAsString();
		return text.isBlank() ? null : text;
	}

	private static boolean hasWhitespace(String id) {
		for (int i = 0; i < id.length(); i++) {
			if (Character.isWhitespace(id.charAt(i))) {
				return true;
			}
		}
		return false;
	}
}
