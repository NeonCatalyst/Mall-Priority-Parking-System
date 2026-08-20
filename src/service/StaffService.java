package service;

import enums.StaffRole;
import model.user.Staff;
import java.util.ArrayList;
import java.util.List;

public class StaffService {
    private final List<Staff> staffAccounts = new ArrayList<>();

    public Staff registerStaff(String staffId, String name, StaffRole role, String password) {
        if (staffAccounts.stream().anyMatch(staff ->
                staff.getUserId().equalsIgnoreCase(staffId)))
            throw new IllegalArgumentException("Staff ID is already registered.");
        Staff staff = new Staff(staffId, name, role, password);
        staffAccounts.add(staff);
        return staff;
    }

    public Staff authenticate(String staffId, String password) {
        if (staffId == null || password == null)
            throw invalidCredentials();
        return staffAccounts.stream()
                .filter(staff -> staff.getUserId().equalsIgnoreCase(staffId.trim()))
                .filter(staff -> staff.verifyPassword(password))
                .findFirst()
                .orElseThrow(StaffService::invalidCredentials);
    }

    public List<Staff> getStaffAccounts() {
        return List.copyOf(staffAccounts);
    }

    private static IllegalArgumentException invalidCredentials() {
        return new IllegalArgumentException("Invalid Staff ID or password.");
    }
}
