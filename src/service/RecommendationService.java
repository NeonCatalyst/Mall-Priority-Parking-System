package service;
import enums.AccessibilityPreference;
import model.Vehicle;
import model.parking.ParkingOption;
import model.parking.ParkingRecommendation;
import model.parking.ParkingSession;
import model.parking.ParkingSlot;
import model.user.PriorityUser;
import recommendation.RecommendationStrategy;
import java.time.LocalDateTime;
import java.util.List;
public class RecommendationService {
    private final RecommendationStrategy strategy;
    public RecommendationService(RecommendationStrategy strategy) {
        if(strategy==null)throw new IllegalArgumentException("Strategy is required.");this.strategy=strategy;
    }
    public ParkingRecommendation getParkingChoices(PriorityUser user,Vehicle vehicle,
            AccessibilityPreference preference,List<ParkingSlot> slots,List<ParkingSession> history) {
        if(user==null||!user.isEligibleForPriorityParking())throw new IllegalStateException("Only verified users get recommendations.");
        if(vehicle==null||!vehicle.belongsTo(user))throw new IllegalArgumentException("Vehicle is not registered to user.");
        if(preference==null)throw new IllegalArgumentException("Accessibility preference is required.");
        List<ParkingOption> options=strategy.generateOptions(user,preference,slots,history,LocalDateTime.now());
        if(options.isEmpty())throw new IllegalStateException("No valid parking choices are currently available.");
        return new ParkingRecommendation(preference,options.subList(0,Math.min(3,options.size())));
    }
}
