package dev.vocabcraft.srs;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record DeckData(String targetLanguage, String supportLanguage, Map<String, CardState> cards) {
	public DeckData {
		cards = Collections.unmodifiableMap(new LinkedHashMap<>(cards));
	}
}
