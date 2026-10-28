package dev.vocabcraft.srs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ReviewPromptTest {
	@Test
	void bedOpensOnceWhenCardsAreWaiting() {
		assertEquals(new ReviewPrompt.Step(false, false, false, false), step(true, 0, 10, false, false, true, true, false, false));
		assertEquals(new ReviewPrompt.Step(true, false, false, true), step(true, 3, 10, false, false, true, true, false, false));
		assertEquals(new ReviewPrompt.Step(true, false, true, true), step(true, 10, 10, false, false, true, true, false, false));
		assertEquals(new ReviewPrompt.Step(false, false, true, true), step(true, 10, 10, true, false, true, true, true, false));
	}

	@Test
	void bedOffLeavesThePileUntilAwake() {
		assertEquals(new ReviewPrompt.Step(false, false, false, false), step(false, 10, 10, false, false, true, true, false, false));
		assertEquals(new ReviewPrompt.Step(false, true, true, false), step(false, 10, 10, false, true, false, false, false, false));
	}

	@Test
	void readyPileOpensOnceWhenFree() {
		assertEquals(new ReviewPrompt.Step(false, false, false, false), step(true, 9, 10, false, true, false, false, false, false));
		assertEquals(new ReviewPrompt.Step(false, false, false, false), step(true, 10, 10, false, false, false, false, false, false));
		assertEquals(new ReviewPrompt.Step(false, true, true, false), step(true, 10, 10, false, true, false, false, false, false));
		assertEquals(new ReviewPrompt.Step(false, false, true, false), step(true, 10, 10, true, true, false, false, false, false));
		assertEquals(new ReviewPrompt.Step(false, false, false, false), step(true, 9, 10, true, true, false, false, false, false));
		assertEquals(new ReviewPrompt.Step(false, false, false, false), step(true, 10, 0, false, true, false, false, false, false));
	}

	@Test
	void anOpenReviewClaimsThePileAndSleepDoesNotOpenWhileAwakeScreenIsPending() {
		assertEquals(new ReviewPrompt.Step(false, false, true, false), step(true, 10, 10, false, false, false, false, false, true));
		assertEquals(new ReviewPrompt.Step(false, false, false, false), step(true, 10, 10, false, true, true, false, false, false));
		assertEquals(new ReviewPrompt.Step(false, false, true, true), step(true, 12, 10, false, false, true, false, false, true));
		assertEquals(new ReviewPrompt.Step(false, false, true, false), step(true, 12, 10, true, true, false, false, true, false));
	}

	private static ReviewPrompt.Step step(
		boolean reviewInBed,
		int queueSize,
		int threshold,
		boolean latched,
		boolean free,
		boolean sleeping,
		boolean bedScreen,
		boolean offeredSleep,
		boolean reviewing
	) {
		return ReviewPrompt.step(reviewInBed, queueSize, threshold, latched, free, sleeping, bedScreen, offeredSleep, reviewing);
	}
}
