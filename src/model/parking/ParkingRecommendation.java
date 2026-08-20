package model.parking;
import enums.AccessibilityPreference;
import java.time.LocalDateTime;
import java.util.List;
public class ParkingRecommendation {
    private final LocalDateTime generatedAt=LocalDateTime.now();
    private final AccessibilityPreference preference;
    private final List<ParkingOption> options;
    public ParkingRecommendation(AccessibilityPreference preference,List<ParkingOption> options) {
        if(preference==null||options==null||options.isEmpty()||options.size()>3)
            throw new IllegalArgumentException("Recommendation requires 1 to 3 valid options.");
        this.preference=preference;this.options=List.copyOf(options);
    }
    public LocalDateTime getGeneratedAt(){return generatedAt;}
    public AccessibilityPreference getPreference(){return preference;}
    public List<ParkingOption> getOptions(){return options;}
}
