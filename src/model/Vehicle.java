package model;
import enums.VehicleType;
import model.user.PriorityUser;
public class Vehicle {
    private final String plateNumber;
    private final VehicleType type;
    private final PriorityUser owner;
    public Vehicle(String plate, VehicleType type, PriorityUser owner) {
        if (plate == null || plate.isBlank() || type == null || owner == null)
            throw new IllegalArgumentException("Valid vehicle details are required.");
        this.plateNumber = plate.trim().toUpperCase();
        this.type = type;
        this.owner = owner;
    }
    public String getPlateNumber() { return plateNumber; }
    public VehicleType getVehicleType() { return type; }
    public PriorityUser getOwner() { return owner; }
    public boolean belongsTo(PriorityUser user) { return owner.equals(user); }
    public String getDisplayName() { return plateNumber + " - " + type; }
}
