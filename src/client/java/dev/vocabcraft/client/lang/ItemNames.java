package dev.vocabcraft.client.lang;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public final class ItemNames {
	private static final Map<String, ClientLanguage> LANGUAGES = new HashMap<>();

	private ItemNames() {
	}

	public static String languageLabel(String code) {
		LanguageInfo info = Minecraft.getInstance().getLanguageManager().getLanguage(code);
		if (info == null || info.name() == null || info.name().isBlank()) {
			return code;
		}
		return info.name();
	}

	public static String itemName(Item item, String code) {
		Identifier id = BuiltInRegistries.ITEM.getKey(item);
		String fallback = id == null ? item.getDescriptionId() : id.toString();
		return language(code).getOrDefault(item.getDescriptionId(), fallback);
	}

	private static ClientLanguage language(String code) {
		return LANGUAGES.computeIfAbsent(code, ItemNames::load);
	}

	private static ClientLanguage load(String code) {
		// Later languages overwrite earlier ones, so English is the base and the chosen language wins.
		List<String> codes = "en_us".equals(code) ? List.of(code) : List.of("en_us", code);
		return ClientLanguage.loadFrom(Minecraft.getInstance().getResourceManager(), codes, false);
	}
}
