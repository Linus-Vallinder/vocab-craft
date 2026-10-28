package dev.vocabcraft.srs;

/** When a waiting queue should open on its own. */
public final class ReviewPrompt {
	public record Step(boolean openBed, boolean openReady, boolean latched, boolean offeredSleep) {
	}

	private ReviewPrompt() {
	}

	public static Step step(
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
		if (!sleeping) {
			offeredSleep = false;
		}
		if (reviewing && threshold > 0 && queueSize >= threshold) {
			latched = true;
		}
		if (sleeping && reviewing) {
			offeredSleep = true;
		}
		if (sleeping && bedScreen && reviewInBed && queueSize > 0 && !offeredSleep) {
			boolean claim = threshold > 0 && queueSize >= threshold;
			return new Step(true, false, latched || claim, true);
		}
		if (threshold <= 0 || queueSize < threshold) {
			return new Step(false, false, false, offeredSleep);
		}
		if (latched || !free || sleeping) {
			return new Step(false, false, latched, offeredSleep);
		}
		return new Step(false, true, true, offeredSleep);
	}
}
