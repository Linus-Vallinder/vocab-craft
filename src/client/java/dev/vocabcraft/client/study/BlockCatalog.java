package dev.vocabcraft.client.study;

import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

public final class BlockCatalog {
	private static Set<String> blockItemIds;

	private BlockCatalog() {
	}

	public static Set<String> blockItemIds() {
		if (blockItemIds == null) {
			Set<String> ids = new LinkedHashSet<>();
			for (Block block : BuiltInRegistries.BLOCK) {
				Item item = block.asItem();
				if (item == Items.AIR) {
					continue;
				}
				Identifier key = BuiltInRegistries.ITEM.getKey(item);
				if (key != null) {
					ids.add(key.toString());
				}
			}
			blockItemIds = Set.copyOf(ids);
		}
		return blockItemIds;
	}
}
