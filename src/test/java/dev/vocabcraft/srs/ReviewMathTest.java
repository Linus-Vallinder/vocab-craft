package dev.vocabcraft.srs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ReviewMathTest {
	@Test
	void missingCardIsDue() {
		assertTrue(ReviewMath.isDue(null, 0L));
	}

	@Test
	void threeCorrectAnswersUseSpedUpIntervals() {
		long now = 1_000_000L;
		CardState card = ReviewMath.grade(null, true, now);
		assertEquals(1, card.reps());
		assertEquals(ReviewMath.FIRST_RIGHT_MS, card.intervalMs());
		assertEquals(now + ReviewMath.FIRST_RIGHT_MS, card.dueAt());
		assertEquals(ReviewMath.START_EASE, card.ease(), 1e-9);

		card = ReviewMath.grade(card, true, card.dueAt());
		assertEquals(2, card.reps());
		assertEquals(ReviewMath.SECOND_RIGHT_MS, card.intervalMs());

		long thirdAt = card.dueAt();
		card = ReviewMath.grade(card, true, thirdAt);
		assertEquals(3, card.reps());
		assertEquals(Math.round(ReviewMath.SECOND_RIGHT_MS * ReviewMath.START_EASE), card.intervalMs());
		assertEquals(thirdAt + card.intervalMs(), card.dueAt());
		assertEquals(ReviewMath.START_EASE, card.ease(), 1e-9);
		assertEquals(0, card.lapses());
		assertEquals(3, card.reviews());
	}

	@Test
	void wrongAnswerResetsAndLowersEase() {
		CardState progressed = ReviewMath.grade(null, true, 0L);
		progressed = ReviewMath.grade(progressed, true, progressed.dueAt());
		long now = progressed.dueAt();
		CardState wrong = ReviewMath.grade(progressed, false, now);
		assertEquals(0, wrong.reps());
		assertEquals(ReviewMath.WRONG_MS, wrong.intervalMs());
		assertEquals(now + ReviewMath.WRONG_MS, wrong.dueAt());
		assertEquals(2.3, wrong.ease(), 1e-9);
		assertEquals(1, wrong.lapses());
		assertEquals(3, wrong.reviews());
	}

	@Test
	void easeDoesNotDropBelowMinimum() {
		CardState card = new CardState(ReviewMath.MIN_EASE, 4, 10_000L, 0L, 4, 1);
		CardState wrong = ReviewMath.grade(card, false, 50L);
		assertEquals(ReviewMath.MIN_EASE, wrong.ease(), 1e-9);
	}

	@Test
	void cardIsNotDueBeforeItsTime() {
		CardState card = ReviewMath.grade(null, true, 1_000L);
		assertFalse(ReviewMath.isDue(card, card.dueAt() - 1));
		assertTrue(ReviewMath.isDue(card, card.dueAt()));
	}
}
