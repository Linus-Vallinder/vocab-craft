package dev.vocabcraft.client.lang;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

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

	public static String entityName(EntityType<?> type, String code) {
		Identifier id = EntityType.getKey(type);
		String fallback = id == null ? type.getDescriptionId() : id.getPath();
		return language(code).getOrDefault(type.getDescriptionId(), fallback);
	}

	public static String cardName(String id, String code) {
		if (id.startsWith("entity:")) {
			Identifier key = Identifier.tryParse(id.substring("entity:".length()));
			if (key != null && BuiltInRegistries.ENTITY_TYPE.containsKey(key)) {
				return entityName(BuiltInRegistries.ENTITY_TYPE.getValue(key), code);
			}
			return id;
		}
		Identifier key = Identifier.tryParse(id);
		if (key == null) {
			return id;
		}
		Item item = BuiltInRegistries.ITEM.getValue(key);
		if (item == null || item == Items.AIR) {
			return id;
		}
		return itemName(item, code);
	}

	public static String newLine(String id, String targetCode, String supportCode) {
		String target = cardName(id, targetCode);
		String support = cardName(id, supportCode);
		if (target.equals(support)) {
			return "New: " + target;
		}
		return "New: " + target + " · " + support;
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
