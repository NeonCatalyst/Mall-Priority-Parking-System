package model.parking;
import enums.ParkingOptionType;
import enums.SlotStatus;
import java.time.Duration;
public class ParkingOption {
    private final ParkingOptionType type;
    private final ParkingSlot slot;
    private final SlotStatus displayedStatus;
    private final int distanceMeters;
    private final Duration estimatedWait;
    private final String explanation;
    public ParkingOption(ParkingOptionType type, ParkingSlot slot, int distance, Duration wait, String explanation) {
        if(type==null||slot==null||distance<0||wait==null||wait.isNegative()||explanation==null||explanation.isBlank())
            throw new IllegalArgumentException("Valid option details are required.");
        this.type=type;this.slot=slot;this.displayedStatus=slot.getStatus();
        this.distanceMeters=distance;this.estimatedWait=wait;this.explanation=explanation;
    }
    public ParkingOptionType getOptionType(){return type;} public ParkingSlot getParkingSlot(){return slot;}
    public SlotStatus getDisplayedStatus(){return displayedStatus;} public int getDistanceMeters(){return distanceMeters;}
    public Duration getEstimatedWait(){return estimatedWait;} public String getExplanation(){return explanation;}
    public String toDisplayString() {
        long roundedMinutes=(estimatedWait.toSeconds()+59)/60;
        String wait=estimatedWait.isZero()?"None":"About "+roundedMinutes+" minutes";
        return type+" | Slot "+slot.getSlotNumber()+" | "+displayedStatus+" | "+distanceMeters+" m | Wait: "+wait
                +System.lineSeparator()+"  "+explanation;
    }
}
