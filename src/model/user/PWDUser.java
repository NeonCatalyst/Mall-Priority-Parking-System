package model.user;
import enums.UserType;
public class PWDUser extends PriorityUser {
    public PWDUser(String userId, String name, String idNumber, String pin) { super(userId, name, idNumber, pin); }
    @Override public UserType getUserType() { return UserType.PWD; }
}
