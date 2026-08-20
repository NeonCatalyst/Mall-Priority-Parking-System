package recommendation;
import enums.AccessibilityPreference;
import model.parking.ParkingOption;
import model.parking.ParkingSession;
import model.parking.ParkingSlot;
import model.user.PriorityUser;
import java.time.LocalDateTime;
import java.util.List;
public interface RecommendationStrategy {
    List<ParkingOption> generateOptions(PriorityUser user, AccessibilityPreference preference,
        List<ParkingSlot> slots, List<ParkingSession> history, LocalDateTime currentTime);
}
