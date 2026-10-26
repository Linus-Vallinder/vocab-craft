package dev.vocabcraft.srs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class StudyStats {
	public static final long SOON_MS = 10 * 60_000L;
	public static final long HOUR_MS = 60 * 60_000L;
	public static final long DAY_MS = 24 * HOUR_MS;
	public static final long WEEK_MS = 7 * DAY_MS;
	public static final int EASE_BUCKETS = 7;
	public static final int ICON_LIMIT = 8;

	private StudyStats() {
	}

	public static Summary of(Map<String, CardState> cards, long now, Set<String> blockItemIds) {
		int shaky = 0;
		int learning = 0;
		int familiar = 0;
		int known = 0;
		int fluent = 0;
		int dueNow = 0;
		int dueSoon = 0;
		int dueHour = 0;
		int dueLater = 0;
		int reviews = 0;
		int wrong = 0;
		int blocksSeen = 0;
		double easeSum = 0;
		long longestInterval = 0;
		long nextWait = Long.MAX_VALUE;
		int[] easeBuckets = new int[EASE_BUCKETS];
		List<Map.Entry<String, CardState>> entries = new ArrayList<>(cards.entrySet());

		for (Map.Entry<String, CardState> entry : entries) {
			CardState card = entry.getValue();
			if (blockItemIds.contains(entry.getKey())) {
				blocksSeen++;
			}
			if (card.reps() <= 0) {
				shaky++;
			} else if (card.intervalMs() >= WEEK_MS) {
				fluent++;
			} else if (card.intervalMs() >= DAY_MS) {
				known++;
			} else if (card.reps() == 1) {
				learning++;
			} else {
				familiar++;
			}
			long wait = card.dueAt() - now;
			if (wait <= 0) {
				dueNow++;
				nextWait = 0;
			} else if (wait <= SOON_MS) {
				dueSoon++;
				nextWait = Math.min(nextWait, wait);
			} else if (wait <= HOUR_MS) {
				dueHour++;
				nextWait = Math.min(nextWait, wait);
			} else {
				dueLater++;
				nextWait = Math.min(nextWait, wait);
			}
			reviews += card.reviews();
			wrong += card.lapses();
			easeSum += card.ease();
			longestInterval = Math.max(longestInterval, card.intervalMs());
			easeBuckets[easeIndex(card.ease())]++;
		}

		entries.sort(Comparator
			.comparingInt((Map.Entry<String, CardState> entry) -> entry.getValue().lapses()).reversed()
			.thenComparingDouble(entry -> entry.getValue().ease())
			.thenComparing(Map.Entry::getKey));
		List<String> trickiest = new ArrayList<>();
		for (Map.Entry<String, CardState> entry : entries) {
			if (entry.getValue().lapses() <= 0) {
				continue;
			}
			trickiest.add(entry.getKey());
			if (trickiest.size() == ICON_LIMIT) {
				break;
			}
		}

		entries.sort(Comparator
			.comparingLong((Map.Entry<String, CardState> entry) -> entry.getValue().dueAt())
			.thenComparing(Map.Entry::getKey));
		List<String> due = new ArrayList<>();
		for (Map.Entry<String, CardState> entry : entries) {
			if (entry.getValue().dueAt() > now) {
				continue;
			}
			due.add(entry.getKey());
			if (due.size() == ICON_LIMIT) {
				break;
			}
		}

		int tracked = entries.size();
		if (tracked == 0) {
			nextWait = -1;
		}
		int correct = Math.max(0, reviews - wrong);
		return new Summary(
			tracked,
			blocksSeen,
			dueNow,
			dueSoon,
			dueHour,
			dueLater,
			shaky,
			learning,
			familiar,
			known,
			fluent,
			reviews,
			correct,
			wrong,
			tracked == 0 ? 0 : easeSum / tracked,
			longestInterval,
			nextWait,
			easeBuckets,
			List.copyOf(trickiest),
			List.copyOf(due)
		);
	}

	public static int percent(int part, int whole) {
		if (whole <= 0 || part <= 0) {
			return 0;
		}
		return part * 100 / whole;
	}

	public static String rank(int coveragePercent) {
		if (coveragePercent <= 0) {
			return "Fresh world";
		}
		if (coveragePercent < 5) {
			return "Just digging in";
		}
		if (coveragePercent < 15) {
			return "Pathfinder";
		}
		if (coveragePercent < 40) {
			return "Surveyor";
		}
		if (coveragePercent < 70) {
			return "Cartographer";
		}
		return "World fluent";
	}

	public static int easeIndex(double ease) {
		int steps = (int) Math.round((ReviewMath.START_EASE - ease) / 0.2);
		return Math.max(0, Math.min(EASE_BUCKETS - 1, steps));
	}

	public static int[] share(int total, int[] parts) {
		int[] widths = new int[parts.length];
		int sum = 0;
		for (int part : parts) {
			sum += Math.max(0, part);
		}
		if (total <= 0 || sum <= 0) {
			return widths;
		}
		int used = 0;
		int last = -1;
		for (int i = 0; i < parts.length; i++) {
			int part = Math.max(0, parts[i]);
			if (part > 0) {
				last = i;
			}
			widths[i] = part * total / sum;
			used += widths[i];
		}
		if (last >= 0) {
			widths[last] += total - used;
		}
		return widths;
	}

	public static String duration(long ms) {
		if (ms < 0) {
			return "none";
		}
		if (ms < 60_000L) {
			return Math.max(0, ms / 1000L) + "s";
		}
		if (ms < HOUR_MS) {
			return (ms / 60_000L) + "m";
		}
		if (ms < DAY_MS) {
			return (ms / HOUR_MS) + "h";
		}
		return (ms / DAY_MS) + "d";
	}

	public record Summary(
		int cards,
		int blocksSeen,
		int dueNow,
		int dueSoon,
		int dueHour,
		int dueLater,
		int shaky,
		int learning,
		int familiar,
		int known,
		int fluent,
		int reviews,
		int correct,
		int wrong,
		double averageEase,
		long longestIntervalMs,
		long nextWaitMs,
		int[] easeBuckets,
		List<String> trickiest,
		List<String> due
	) {
		public int[] masteryCounts() {
			return new int[] {shaky, learning, familiar, known, fluent};
		}

		public int[] scheduleCounts() {
			return new int[] {dueNow, dueSoon, dueHour, dueLater};
		}
	}
}
