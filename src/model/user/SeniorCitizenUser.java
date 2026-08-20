package model.user;
import enums.UserType;
public class SeniorCitizenUser extends PriorityUser {
    public SeniorCitizenUser(String id,String name,String eligibilityId,String pin){super(id,name,eligibilityId,pin);}
 @Override public UserType getUserType(){return UserType.SENIOR_CITIZEN;}
}
