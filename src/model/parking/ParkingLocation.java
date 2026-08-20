package model.parking;
import enums.AccessibilityPreference;
public final class ParkingLocation {
    private final String building;
    private final String floor;
    private final String section;
    private final int entranceDistance;
    private final int elevatorDistance;
    private final int rampDistance;
    public ParkingLocation(String building, String floor, String section, int entrance, int elevator, int ramp) {
        if (building == null || floor == null || section == null || entrance < 0 || elevator < 0 || ramp < 0)
            throw new IllegalArgumentException("Valid location details are required.");
        this.building=building; this.floor=floor; this.section=section;
        this.entranceDistance=entrance; this.elevatorDistance=elevator; this.rampDistance=ramp;
    }
    public String getBuilding() { return building; }
    public String getFloor() { return floor; }
    public String getSection() { return section; }
    public int getDistanceTo(AccessibilityPreference preference) {
        return switch (preference) {
            case ACCESSIBLE_ENTRANCE -> entranceDistance;
            case ELEVATOR -> elevatorDistance;
            case RAMP -> rampDistance;
            case NO_PREFERENCE -> Math.min(entranceDistance, Math.min(elevatorDistance, rampDistance));
        };
    }
    public String getDescription() { return building + ", " + floor + ", Section " + section; }
}
