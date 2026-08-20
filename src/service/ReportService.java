package service;
import enums.SlotStatus;
import model.parking.ParkingSession;
import model.parking.ParkingSlot;
import java.time.Duration;
import java.util.List;
public class ReportService {
    public long countSlotsByStatus(List<ParkingSlot> slots,SlotStatus status) {
        return slots.stream().filter(slot->slot.getStatus()==status).count();
    }
    public double calculateOccupancyPercentage(List<ParkingSlot> slots) {
        if(slots.isEmpty())return 0;return countSlotsByStatus(slots,SlotStatus.OCCUPIED)*100.0/slots.size();
    }
    public Duration calculateAverageParkingDuration(List<ParkingSession> history) {
        List<ParkingSession> completed=history.stream().filter(s->s.getActualDuration()!=null).toList();
        if(completed.isEmpty())return Duration.ZERO;
        return Duration.ofSeconds((long)completed.stream().mapToLong(s->s.getActualDuration().toSeconds()).average().orElse(0));
    }
}
