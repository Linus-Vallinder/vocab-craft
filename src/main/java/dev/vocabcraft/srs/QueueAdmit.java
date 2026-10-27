package dev.vocabcraft.srs;

/** Whether a due id joins the queue, and whether this sighting is the first one. */
public final class QueueAdmit {
	public record Result(boolean introduce, boolean enqueue) {
	}

	private QueueAdmit() {
	}

	public static Result decide(boolean known, boolean queued, int queueSize, int maxPending, int introducedToday, int dailyLimit) {
		if (queued) {
			return new Result(false, false);
		}
		if (!known && dailyLimit > 0 && introducedToday >= dailyLimit) {
			return new Result(false, false);
		}
		if (maxPending > 0 && queueSize >= maxPending) {
			return new Result(false, false);
		}
		return new Result(!known, true);
	}
}
