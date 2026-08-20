package model.parking;
import enums.SlotStatus;
public class ParkingSlot {
    private final String slotNumber;
    private final ParkingLocation location;
    private SlotStatus status = SlotStatus.AVAILABLE;
    private ParkingSession activeSession;
    public ParkingSlot(String number, ParkingLocation location) {
        if (number == null || number.isBlank() || location == null) throw new IllegalArgumentException("Slot details are required.");
        this.slotNumber=number.trim().toUpperCase(); this.location=location;
    }
    public String getSlotNumber(){return slotNumber;} public ParkingLocation getLocation(){return location;}
    public SlotStatus getStatus(){return status;} public ParkingSession getActiveSession(){return activeSession;}
    public boolean isAvailable(){return status==SlotStatus.AVAILABLE;}
    public void occupy(ParkingSession session) {
        if (!isAvailable()) throw new IllegalStateException("Slot is not available.");
        if (session == null || session.getParkingSlot()!=this) throw new IllegalArgumentException("Session must use this slot.");
        activeSession=session; status=SlotStatus.OCCUPIED;
    }
    public void reserve(){if(!isAvailable())throw new IllegalStateException("Only available slots can be reserved.");status=SlotStatus.RESERVED;}
    public void release(){activeSession=null;status=SlotStatus.AVAILABLE;}
}
