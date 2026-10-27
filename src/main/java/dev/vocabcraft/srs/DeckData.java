package dev.vocabcraft.srs;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record DeckData(
	String targetLanguage,
	String supportLanguage,
	Map<String, CardState> cards,
	List<String> queue,
	String newDay,
	int newCount
) {
	public DeckData(String targetLanguage, String supportLanguage, Map<String, CardState> cards) {
		this(targetLanguage, supportLanguage, cards, List.of(), null, 0);
	}

	public DeckData {
		cards = Collections.unmodifiableMap(new LinkedHashMap<>(cards));
		queue = List.copyOf(queue);
	}
}
