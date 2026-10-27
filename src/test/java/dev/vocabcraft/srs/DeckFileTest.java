package dev.vocabcraft.srs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DeckFileTest {
	@Test
	void roundTripKeepsLanguagesCardsAndOrder(@TempDir Path dir) throws Exception {
		Map<String, CardState> cards = new LinkedHashMap<>();
		cards.put("minecraft:dirt", new CardState(2.5, 1, 300_000L, 10L, 1, 0));
		cards.put("minecraft:stone", new CardState(2.3, 0, 30_000L, 40L, 2, 1));
		DeckData data = new DeckData("es_cl", "es_ec", cards);
		Path file = dir.resolve("deck.json");
		DeckFile.writeAtomically(file, DeckFile.write(data));

		DeckData read = DeckFile.read(Files.readString(file));
		assertEquals("es_cl", read.targetLanguage());
		assertEquals("es_ec", read.supportLanguage());
		assertEquals(cards.keySet().stream().toList(), read.cards().keySet().stream().toList());
		assertEquals(cards.get("minecraft:stone"), read.cards().get("minecraft:stone"));
		assertEquals(List.of(), read.queue());
		assertEquals(0, read.newCount());
		assertTrue(Files.readString(file).contains("\"format\": 1"));
	}

	@Test
	void roundTripKeepsTheQueueAndDropsUnknownIds() {
		Map<String, CardState> cards = new LinkedHashMap<>();
		cards.put("minecraft:dirt", new CardState(2.5, 0, 0L, 1L, 0, 0));
		cards.put("entity:minecraft:zombie", new CardState(2.5, 1, 300_000L, 5L, 1, 0));
		DeckData data = new DeckData("en_us", "ko_kr", cards, List.of("entity:minecraft:zombie", "minecraft:dirt"), "2026-10-27", 2);
		DeckData read = DeckFile.read(DeckFile.write(data));
		assertEquals(List.of("entity:minecraft:zombie", "minecraft:dirt"), read.queue());
		assertEquals("2026-10-27", read.newDay());
		assertEquals(2, read.newCount());

		String stale = """
			{"cards":{"minecraft:dirt":{"ease":2.5,"reps":0,"intervalMs":0,"dueAt":1,"reviews":0,"lapses":0}},"queue":["missing:id","minecraft:dirt"," ","minecraft:dirt"]}
			""";
		assertEquals(List.of("minecraft:dirt"), DeckFile.read(stale).queue());
	}

	@Test
	void legacyDeckWithoutFormatStillLoads() {
		String json = """
			{"targetLanguage":"ko_kr","cards":{"minecraft:oak_log":{"ease":2.5,"reps":2,"intervalMs":3600000,"dueAt":9,"reviews":2,"lapses":0}}}
			""";
		DeckData read = DeckFile.read(json);
		assertEquals("ko_kr", read.targetLanguage());
		assertEquals(2, read.cards().get("minecraft:oak_log").reps());
		assertEquals(List.of(), read.queue());
	}

	@Test
	void rejectsUnknownFormatAndBrokenCards() {
		assertEquals("Progress file is not valid JSON.", assertThrows(DeckFormatException.class, () -> DeckFile.read("{")).getMessage());
		assertTrue(assertThrows(DeckFormatException.class, () -> DeckFile.read("{\"format\":2,\"cards\":{}}")).getMessage().contains("format 2"));
		assertEquals(
			"minecraft:dirt is missing ease.",
			assertThrows(DeckFormatException.class, () -> DeckFile.read("{\"cards\":{\"minecraft:dirt\":{\"reps\":1}}}")).getMessage()
		);
		assertThrows(DeckFormatException.class, () -> DeckFile.read("{\"cards\":{\"minecraft:dirt\":{\"ease\":2.5,\"reps\":-1,\"intervalMs\":1,\"dueAt\":1,\"reviews\":0,\"lapses\":0}}}"));
	}
}
