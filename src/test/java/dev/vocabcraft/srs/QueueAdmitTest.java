package dev.vocabcraft.srs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class QueueAdmitTest {
	@Test
	void firstSightingJoinsTheQueueAndLaterOnesOnlyQueue() {
		assertEquals(new QueueAdmit.Result(true, true), QueueAdmit.decide(false, false, 0, 0, 0, 0));
		assertEquals(new QueueAdmit.Result(false, true), QueueAdmit.decide(true, false, 1, 0, 1, 0));
		assertEquals(new QueueAdmit.Result(false, false), QueueAdmit.decide(true, true, 1, 0, 1, 0));
	}

	@Test
	void capsBlockNewCardsAndAFullQueue() {
		assertEquals(new QueueAdmit.Result(false, false), QueueAdmit.decide(false, false, 0, 0, 5, 5));
		assertEquals(new QueueAdmit.Result(false, true), QueueAdmit.decide(true, false, 2, 0, 5, 5));
		assertEquals(new QueueAdmit.Result(false, false), QueueAdmit.decide(true, false, 3, 3, 0, 0));
		assertEquals(new QueueAdmit.Result(true, true), QueueAdmit.decide(false, false, 9, 0, 9, 0));
	}
}
