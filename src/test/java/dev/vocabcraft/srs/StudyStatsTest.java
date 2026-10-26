package dev.vocabcraft.srs;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class StudyStatsTest {
	private static final long NOW = 1_000_000L;

	@Test
	void emptyDeckHasNoCoverageAndNoNextCard() {
		StudyStats.Summary summary = StudyStats.of(Map.of(), NOW, Set.of("minecraft:dirt"));
		assertEquals(0, summary.cards());
		assertEquals(0, summary.blocksSeen());
		assertEquals(0, StudyStats.percent(summary.blocksSeen(), 100));
		assertEquals("Fresh world", StudyStats.rank(0));
		assertEquals(-1L, summary.nextWaitMs());
		assertEquals(0, summary.masteryCounts()[0] + summary.scheduleCounts()[0]);
	}

	@Test
	void bucketsSplitMasteryScheduleAndBlockCoverage() {
		Map<String, CardState> cards = new LinkedHashMap<>();
		cards.put("minecraft:dirt", new CardState(2.5, 1, ReviewMath.FIRST_RIGHT_MS, NOW - 1, 1, 0));
		cards.put("minecraft:oak_log", new CardState(2.5, 2, ReviewMath.SECOND_RIGHT_MS, NOW + StudyStats.HOUR_MS + 1, 2, 0));
		cards.put("minecraft:stone", new CardState(2.5, 4, StudyStats.DAY_MS, NOW + StudyStats.DAY_MS, 4, 0));
		cards.put("minecraft:obsidian", new CardState(2.3, 5, StudyStats.WEEK_MS, NOW - 5, 6, 1));
		cards.put("minecraft:sand", new CardState(2.3, 0, ReviewMath.WRONG_MS, NOW + 30_000L, 2, 2));
		cards.put("minecraft:stick", new CardState(2.5, 1, ReviewMath.FIRST_RIGHT_MS, NOW + 30 * 60_000L, 1, 0));

		StudyStats.Summary summary = StudyStats.of(cards, NOW, Set.of(
			"minecraft:dirt", "minecraft:oak_log", "minecraft:stone", "minecraft:obsidian", "minecraft:sand"
		));

		assertEquals(6, summary.cards());
		assertEquals(5, summary.blocksSeen());
		assertEquals(2, summary.learning());
		assertEquals(1, summary.familiar());
		assertEquals(1, summary.known());
		assertEquals(1, summary.fluent());
		assertEquals(1, summary.shaky());
		assertEquals(2, summary.dueNow());
		assertEquals(1, summary.dueSoon());
		assertEquals(1, summary.dueHour());
		assertEquals(2, summary.dueLater());
		assertEquals(0L, summary.nextWaitMs());
		assertEquals(StudyStats.WEEK_MS, summary.longestIntervalMs());
		assertEquals(4, summary.easeBuckets()[0]);
		assertEquals(2, summary.easeBuckets()[1]);
		assertEquals(13, summary.correct());
		assertEquals(3, summary.wrong());
		assertEquals("minecraft:sand", summary.trickiest().get(0));
		assertEquals("minecraft:obsidian", summary.due().get(0));
	}

	@Test
	void easeIndexAndBarSharesStayInRange() {
		assertEquals(0, StudyStats.easeIndex(ReviewMath.START_EASE));
		assertEquals(1, StudyStats.easeIndex(2.3));
		assertEquals(6, StudyStats.easeIndex(ReviewMath.MIN_EASE));
		assertArrayEquals(new int[] {5, 5, 0}, StudyStats.share(10, new int[] {1, 1, 0}));
		assertArrayEquals(new int[] {3, 3, 4}, StudyStats.share(10, new int[] {1, 1, 1}));
		assertArrayEquals(new int[] {0, 0}, StudyStats.share(10, new int[] {0, 0}));
		assertEquals("World fluent", StudyStats.rank(70));
		assertEquals("12m", StudyStats.duration(12 * 60_000L));
	}

}
