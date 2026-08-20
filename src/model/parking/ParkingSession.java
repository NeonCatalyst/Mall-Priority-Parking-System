package model.parking;
import enums.AccessibilityPreference;
import enums.SessionStatus;
import model.Vehicle;
import model.user.PriorityUser;
import java.time.Duration;
import java.time.LocalDateTime;
public class ParkingSession {
    private final String sessionId;
    private final PriorityUser user;
    private final Vehicle vehicle;
    private final ParkingSlot parkingSlot;
    private final AccessibilityPreference preference;
    private final LocalDateTime checkInTime;
    private final Duration estimatedDuration;
    private LocalDateTime checkOutTime;
    private Duration actualDuration;
    private SessionStatus status = SessionStatus.ACTIVE;
    public ParkingSession(String id, PriorityUser user, Vehicle vehicle, ParkingSlot slot,
                          AccessibilityPreference preference, LocalDateTime checkIn, Duration estimated) {
        if (id == null || user == null || vehicle == null || slot == null || preference == null
                || checkIn == null || estimated == null || estimated.isZero() || estimated.isNegative())
            throw new IllegalArgumentException("Valid session details are required.");
        if (!vehicle.belongsTo(user)) throw new IllegalArgumentException("Vehicle does not belong to user.");
        this.sessionId=id; this.user=user; this.vehicle=vehicle; this.parkingSlot=slot;
        this.preference=preference; this.checkInTime=checkIn; this.estimatedDuration=estimated;
    }
    public String getSessionId(){return sessionId;} public PriorityUser getUser(){return user;}
    public Vehicle getVehicle(){return vehicle;} public ParkingSlot getParkingSlot(){return parkingSlot;}
    public AccessibilityPreference getAccessibilityPreference(){return preference;}
    public LocalDateTime getCheckInTime(){return checkInTime;} public Duration getEstimatedDuration(){return estimatedDuration;}
    public LocalDateTime getCheckOutTime(){return checkOutTime;} public Duration getActualDuration(){return actualDuration;}
    public SessionStatus getStatus(){return status;} public boolean isActive(){return status==SessionStatus.ACTIVE;}
    public LocalDateTime getEstimatedDepartureTime(){return checkInTime.plus(estimatedDuration);}
    public void complete(LocalDateTime time) {
        if (!isActive()) throw new IllegalStateException("Session is already completed.");
        if (time == null || time.isBefore(checkInTime)) throw new IllegalArgumentException("Invalid check-out time.");
        checkOutTime=time; actualDuration=Duration.between(checkInTime,time); status=SessionStatus.COMPLETED;
    }
}
