import app.DemoData;
import app.MallParkingApplication;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        DemoData demoData = new DemoData();
        demoData.seed();
        new MallParkingApplication(demoData, new Scanner(System.in)).run();
    }
}
