package recommendation;

import enums.*;
import model.parking.*;
import model.user.PriorityUser;
import java.time.*;
import java.util.*;

public class RuleBasedRecommendationStrategy implements RecommendationStrategy {
    public static final Duration MAXIMUM_WAIT = Duration.ofMinutes(5);
    public static final int MINIMUM_DISTANCE_BENEFIT_METERS = 5;
    private static final Duration RECENT_ACTIVITY_WINDOW = Duration.ofMinutes(15);
    private static final Duration SMALL_ACTIVITY_ADJUSTMENT = Duration.ofSeconds(15);

    @Override
    public List<ParkingOption> generateOptions(PriorityUser user,
            AccessibilityPreference preference, List<ParkingSlot> slots,
            List<ParkingSession> history, LocalDateTime now) {
        List<ParkingSlot> available = slots.stream()
                .filter(ParkingSlot::isAvailable)
                .sorted(Comparator.comparingInt(slot -> distance(slot, preference)))
                .toList();
        List<ParkingOption> options = new ArrayList<>();

        if (!available.isEmpty()) {
            ParkingSlot best = available.get(0);
            options.add(new ParkingOption(ParkingOptionType.PARK_NOW, best,
                    distance(best, preference), Duration.ZERO,
                    "This is the closest suitable parking slot currently available."));

            ParkingSlot waiting = findWaitingCandidate(
                    slots, history, now, preference, distance(best, preference));
            if (waiting != null) {
                Duration wait = estimateWait(waiting.getActiveSession(), history, slots, now);
                String historyNote = completedDurations(history).isEmpty() ? "" :
                        " Historical parking duration was used to improve the estimate.";
                options.add(new ParkingOption(ParkingOptionType.WAIT_FOR_CLOSER_SLOT,
                        waiting, distance(waiting, preference), wait,
                        "This slot is meaningfully closer and its current vehicle is expected to leave soon."
                                + historyNote + " Waiting is not guaranteed."));
            }

            if (available.size() > 1 && options.size() < 3) {
                ParkingSlot alternative = available.get(1);
                int extraDistance = distance(alternative, preference) - distance(best, preference);
                options.add(new ParkingOption(ParkingOptionType.ALTERNATIVE,
                        alternative, distance(alternative, preference), Duration.ZERO,
                        "Available immediately as a backup option"
                                + (extraDistance > 0 ? " and only " + extraDistance + " meters farther away." : ".")));
            }
        }
        return options;
    }

    public Duration estimateWait(ParkingSession activeSession, List<ParkingSession> history,
                                 List<ParkingSlot> slots, LocalDateTime now) {
        Duration expectedTotal = activeSession.getEstimatedDuration();
        List<Duration> completed = completedDurations(history);
        if (!completed.isEmpty()) {
            long historicalAverage = Math.round(completed.stream()
                    .mapToLong(Duration::toSeconds).average().orElse(0));
            long adjustedSeconds = Math.round(
                    expectedTotal.toSeconds() * 0.70 + historicalAverage * 0.30);
            expectedTotal = Duration.ofSeconds(adjustedSeconds);
        }

        Duration remaining = Duration.between(
                now, activeSession.getCheckInTime().plus(expectedTotal));
        if (remaining.isNegative() || remaining.isZero()) return Duration.ZERO;

        long recentEntries = slots.stream().filter(slot -> slot.getActiveSession() != null)
                .map(ParkingSlot::getActiveSession)
                .filter(session -> Duration.between(session.getCheckInTime(), now)
                        .compareTo(RECENT_ACTIVITY_WINDOW) <= 0).count();
        long recentExits = history.stream().filter(session -> session.getCheckOutTime() != null)
                .filter(session -> Duration.between(session.getCheckOutTime(), now)
                        .compareTo(RECENT_ACTIVITY_WINDOW) <= 0).count();
        if (recentExits > recentEntries) remaining = remaining.minus(SMALL_ACTIVITY_ADJUSTMENT);
        if (recentEntries > recentExits) remaining = remaining.plus(SMALL_ACTIVITY_ADJUSTMENT);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    private ParkingSlot findWaitingCandidate(List<ParkingSlot> slots,
            List<ParkingSession> history, LocalDateTime now,
            AccessibilityPreference preference, int bestAvailableDistance) {
        return slots.stream()
                .filter(slot -> slot.getStatus() == SlotStatus.OCCUPIED)
                .filter(slot -> slot.getActiveSession() != null && slot.getActiveSession().isActive())
                .filter(slot -> bestAvailableDistance - distance(slot, preference)
                        >= MINIMUM_DISTANCE_BENEFIT_METERS)
                .filter(slot -> {
                    Duration wait = estimateWait(slot.getActiveSession(), history, slots, now);
                    return !wait.isZero() && wait.compareTo(MAXIMUM_WAIT) <= 0;
                })
                .min(Comparator.comparingInt(slot -> distance(slot, preference)))
                .orElse(null);
    }

    private List<Duration> completedDurations(List<ParkingSession> history) {
        return history.stream()
                .filter(session -> session.getStatus() == SessionStatus.COMPLETED)
                .map(ParkingSession::getActualDuration)
                .filter(Objects::nonNull)
                .toList();
    }

    private int distance(ParkingSlot slot, AccessibilityPreference preference) {
        return slot.getLocation().getDistanceTo(preference);
    }
}
