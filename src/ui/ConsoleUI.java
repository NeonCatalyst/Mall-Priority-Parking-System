package ui;

import java.util.Scanner;

public class ConsoleUI {
    public static final String LINE = "============================================================";
    private final Scanner scanner;

    public ConsoleUI(Scanner scanner) {
        this.scanner = scanner;
    }

    public void printHeader(String title) {
        System.out.println();
        System.out.println(LINE);
        System.out.println(title);
        System.out.println("=".repeat(Math.min(title.length(), LINE.length())));
        System.out.println();
    }

    public void printDivider() {
        System.out.println("------------------------------------------------------------");
    }

    public void success(String message) { System.out.println("[SUCCESS] " + message); }
    public void error(String message) { System.out.println("[ERROR] " + message); }
    public void warning(String message) { System.out.println("[WARNING] " + message); }
    public void info(String message) { System.out.println("[INFO] " + message); }

    public String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    public int readChoice(String prompt, int minimum, int maximum) {
        while (true) {
            String input = readLine(prompt);
            try {
                int choice = Integer.parseInt(input);
                if (choice >= minimum && choice <= maximum) return choice;
            } catch (NumberFormatException ignored) {
                // A friendly message is printed below for every invalid value.
            }
            error("Invalid choice. Please select " + minimum + "-" + maximum + ".");
        }
    }

    public long readPositiveNumber(String prompt) {
        while (true) {
            String input = readLine(prompt);
            try {
                long value = Long.parseLong(input);
                if (value > 0) return value;
            } catch (NumberFormatException ignored) {
            }
            error("Please enter a positive whole number.");
        }
    }

    public void pressEnterToContinue() {
        readLine("Press ENTER to continue.");
    }
}
