package dev.vocabcraft.srs;

public final class ReviewMath {
	public static final double START_EASE = 2.5;
	public static final double MIN_EASE = 1.3;
	public static final long WRONG_MS = 30_000L;
	public static final long FIRST_RIGHT_MS = 5 * 60_000L;
	public static final long SECOND_RIGHT_MS = 60 * 60_000L;

	private ReviewMath() {
	}

	public static boolean isDue(CardState card, long now) {
		return card == null || now >= card.dueAt();
	}

	public static CardState grade(CardState card, boolean correct, long now) {
		double ease = card == null ? START_EASE : card.ease();
		int reps = card == null ? 0 : card.reps();
		long intervalMs = card == null ? 0L : card.intervalMs();
		int reviews = card == null ? 0 : card.reviews();
		int lapses = card == null ? 0 : card.lapses();
		reviews++;
		if (!correct) {
			return new CardState(lowerEase(ease), 0, WRONG_MS, now + WRONG_MS, reviews, lapses + 1);
		}
		long nextInterval;
		if (reps <= 0) {
			nextInterval = FIRST_RIGHT_MS;
		} else if (reps == 1) {
			nextInterval = SECOND_RIGHT_MS;
		} else {
			nextInterval = Math.round(intervalMs * ease);
		}
		return new CardState(ease, reps + 1, nextInterval, now + nextInterval, reviews, lapses);
	}

	private static double lowerEase(double ease) {
		double next = Math.rint((ease - 0.2) * 10.0) / 10.0;
		return Math.max(MIN_EASE, next);
	}
}
