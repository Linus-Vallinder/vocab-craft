package dev.vocabcraft.client.ui;

import dev.vocabcraft.client.srs.DeckStore;
import dev.vocabcraft.client.study.BlockCatalog;
import dev.vocabcraft.srs.StudyStats;

import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class StatsPanel extends AbstractScrollArea {
	private static final int SHAKY = 0xFFE85D4C;
	private static final int LEARNING = 0xFFF0A202;
	private static final int FAMILIAR = 0xFFE6C229;
	private static final int KNOWN = 0xFF5BB98C;
	private static final int FLUENT = 0xFF4C8DFF;
	private static final int TRACK = 0xFF2C3036;
	private static final int LABEL = 0xFFE7E7E7;
	private static final int DIM = 0xFFB0B0B0;
	private static final String[] MASTERY = {"Shaky", "Learning", "Familiar", "Known", "Fluent"};
	private static final int[] MASTERY_COLORS = {SHAKY, LEARNING, FAMILIAR, KNOWN, FLUENT};
	private static final String[] SCHEDULE = {"Now", "10m", "1h", "Later"};
	private static final int[] SCHEDULE_COLORS = {0xFFE8A838, 0xFF7EB6FF, 0xFF5B8DEF, 0xFF6E8B90};
	private static final int[] EASE_COLORS = {0xFF3DDC97, 0xFF7ED67A, 0xFFC6D35A, 0xFFE6C229, 0xFFF0A202, 0xFFE37B3A, SHAKY};

	private int contentHeight = 1;

	StatsPanel(int x, int y, int width, int height) {
		super(x, y, width, height, Component.literal("Study stats"), AbstractScrollArea.defaultSettings(16));
	}

	@Override
	protected int contentHeight() {
		return contentHeight;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xC0101214);
		int right = getX() + getWidth() - scrollbarWidth();
		graphics.enableScissor(getX(), getY(), right, getY() + getHeight());
		DeckStore deck = DeckStore.get();
		Set<String> blocks = BlockCatalog.blockItemIds();
		StudyStats.Summary summary = StudyStats.of(deck.cards(), System.currentTimeMillis(), blocks);
		Font font = Minecraft.getInstance().font;
		int x = getX() + 8;
		int inner = right - x - 8;
		int y = getY() + 8 - (int) scrollAmount();
		int origin = y;
		y = coverage(graphics, font, summary, blocks.size(), x, y, inner);
		y = mastery(graphics, font, summary, x, y, inner);
		y = accuracy(graphics, font, summary, x, y, inner);
		y = schedule(graphics, font, summary, x, y, inner);
		y = ease(graphics, font, summary, x, y, inner);
		y = icons(graphics, font, "Trickiest", summary.trickiest(), "No misses yet.", x, y, mouseX, mouseY);
		y = icons(graphics, font, "Due now", summary.due(), "Nothing is due.", x, y, mouseX, mouseY);
		contentHeight = y - origin + 16;
		refreshScrollAmount();
		graphics.disableScissor();
		extractScrollbar(graphics, mouseX, mouseY);
	}

	private int coverage(GuiGraphicsExtractor graphics, Font font, StudyStats.Summary summary, int blockTotal, int x, int y, int inner) {
		int covered = StudyStats.percent(summary.blocksSeen(), blockTotal);
		String percent = covered + "%";
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(2.0F, 2.0F);
		graphics.text(font, percent, 0, 0, 0xFF7DFFC3);
		graphics.pose().popMatrix();
		graphics.text(font, StudyStats.rank(covered), x + font.width(percent) * 2 + 8, y + 5, LABEL);
		y += 22;
		graphics.text(font, "Block coverage", x, y, DIM);
		y += font.lineHeight + 3;
		bar(graphics, x, y, inner, 8, new int[] {summary.blocksSeen(), Math.max(0, blockTotal - summary.blocksSeen())}, new int[] {0xFF3DDC97, TRACK});
		y += 12;
		String caption = summary.blocksSeen() + " of " + blockTotal + " blocks";
		int other = summary.cards() - summary.blocksSeen();
		if (other > 0) {
			caption += " · " + other + " other cards";
		}
		graphics.text(font, caption, x, y, DIM);
		y += font.lineHeight + 2;
		if (summary.cards() == 0) {
			graphics.text(font, "Break, place, or use a block to start.", x, y, LABEL);
		} else {
			String next = summary.nextWaitMs() == 0 ? "due now" : "in " + StudyStats.duration(summary.nextWaitMs());
			graphics.text(font, "Next card " + next + " · longest memory " + StudyStats.duration(summary.longestIntervalMs()), x, y, LABEL);
		}
		return y + font.lineHeight + 10;
	}

	private int mastery(GuiGraphicsExtractor graphics, Font font, StudyStats.Summary summary, int x, int y, int inner) {
		int held = summary.known() + summary.fluent();
		graphics.text(font, "Mastery · " + StudyStats.percent(held, summary.cards()) + "% held a day or more", x, y, LABEL);
		y += font.lineHeight + 3;
		bar(graphics, x, y, inner, 10, summary.masteryCounts(), MASTERY_COLORS);
		y += 14;
		return legend(graphics, font, MASTERY, summary.masteryCounts(), MASTERY_COLORS, summary.cards(), x, y, inner) + 8;
	}

	private int accuracy(GuiGraphicsExtractor graphics, Font font, StudyStats.Summary summary, int x, int y, int inner) {
		int accuracy = StudyStats.percent(summary.correct(), summary.reviews());
		graphics.text(font, "Accuracy · " + (summary.reviews() == 0 ? "n/a" : accuracy + "%"), x, y, LABEL);
		y += font.lineHeight + 3;
		bar(graphics, x, y, inner, 8, new int[] {summary.correct(), summary.wrong()}, new int[] {KNOWN, SHAKY});
		y += 12;
		String detail = summary.reviews() == 0
			? "No reviews yet."
			: summary.correct() + " right · " + summary.wrong() + " wrong · " + summary.reviews() + " reviews";
		graphics.text(font, detail, x, y, DIM);
		return y + font.lineHeight + 10;
	}

	private int schedule(GuiGraphicsExtractor graphics, Font font, StudyStats.Summary summary, int x, int y, int inner) {
		graphics.text(font, "When cards come back", x, y, LABEL);
		y += font.lineHeight + 2;
		return columns(graphics, font, x, y, inner, summary.scheduleCounts(), SCHEDULE, SCHEDULE_COLORS) + 6;
	}

	private int ease(GuiGraphicsExtractor graphics, Font font, StudyStats.Summary summary, int x, int y, int inner) {
		String average = summary.cards() == 0 ? "n/a" : String.format(java.util.Locale.ROOT, "%.1f", summary.averageEase());
		graphics.text(font, "Ease · average " + average, x, y, LABEL);
		y += font.lineHeight + 2;
		int bottom = columns(graphics, font, x, y, inner, summary.easeBuckets(), new String[] {"2.5", "", "", "", "", "", "1.3"}, EASE_COLORS);
		graphics.text(font, "Easier cards stay left. Wrong answers push them right.", x, bottom, DIM);
		return bottom + font.lineHeight + 10;
	}

	private int icons(GuiGraphicsExtractor graphics, Font font, String title, java.util.List<String> ids, String empty, int x, int y, int mouseX, int mouseY) {
		graphics.text(font, title, x, y, LABEL);
		y += font.lineHeight + 3;
		if (ids.isEmpty()) {
			graphics.text(font, empty, x, y, DIM);
			return y + font.lineHeight + 10;
		}
		int shown = 0;
		for (String id : ids) {
			Item item = item(id);
			if (item == Items.AIR) {
				continue;
			}
			int ix = x + shown * 18;
			ItemStack stack = item.getDefaultInstance();
			if (mouseX >= ix && mouseX < ix + 16 && mouseY >= y && mouseY < y + 16 && isMouseOver(mouseX, mouseY)) {
				graphics.fill(ix - 1, y - 1, ix + 17, y + 17, 0x40FFFFFF);
				graphics.setTooltipForNextFrame(font, stack, mouseX, mouseY);
			}
			graphics.item(stack, ix, y);
			shown++;
		}
		return y + 18 + 8;
	}

	private static Item item(String id) {
		Identifier key = Identifier.tryParse(id);
		if (key == null) {
			return Items.AIR;
		}
		Item item = BuiltInRegistries.ITEM.getValue(key);
		return item == null ? Items.AIR : item;
	}

	private static void bar(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int[] counts, int[] colors) {
		graphics.fill(x, y, x + width, y + height, TRACK);
		int[] shares = StudyStats.share(width, counts);
		int cursor = x;
		for (int i = 0; i < shares.length; i++) {
			if (shares[i] <= 0) {
				continue;
			}
			graphics.fill(cursor, y, cursor + shares[i], y + height, colors[i]);
			cursor += shares[i];
		}
	}

	private int legend(GuiGraphicsExtractor graphics, Font font, String[] names, int[] counts, int[] colors, int whole, int x, int y, int inner) {
		int cursor = x;
		int row = y;
		for (int i = 0; i < names.length; i++) {
			String label = names[i] + " " + counts[i] + " · " + StudyStats.percent(counts[i], whole) + "%";
			int need = 12 + font.width(label) + 8;
			if (cursor > x && cursor + need > x + inner) {
				cursor = x;
				row += font.lineHeight + 3;
			}
			graphics.fill(cursor, row + 1, cursor + 8, row + 9, colors[i]);
			graphics.text(font, label, cursor + 12, row, DIM);
			cursor += need;
		}
		return row + font.lineHeight;
	}

	private int columns(GuiGraphicsExtractor graphics, Font font, int x, int y, int inner, int[] counts, String[] labels, int[] colors) {
		int max = 1;
		for (int count : counts) {
			max = Math.max(max, count);
		}
		int column = Math.max(1, inner / counts.length);
		int barMax = 36;
		int base = y + 12 + barMax;
		for (int i = 0; i < counts.length; i++) {
			int height = counts[i] == 0 ? 0 : Math.max(2, counts[i] * barMax / max);
			int left = x + i * column + Math.max(0, (column - 14) / 2);
			if (height > 0) {
				graphics.fill(left, base - height, left + 14, base, colors[i]);
			} else {
				graphics.fill(left, base - 1, left + 14, base, TRACK);
			}
			String count = Integer.toString(counts[i]);
			graphics.text(font, count, left + 7 - font.width(count) / 2, base - height - 10, LABEL);
			if (!labels[i].isEmpty()) {
				graphics.text(font, labels[i], left + 7 - font.width(labels[i]) / 2, base + 3, DIM);
			}
		}
		return base + 3 + font.lineHeight;
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}
