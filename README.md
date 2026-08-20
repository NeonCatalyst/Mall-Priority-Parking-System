# Mall Priority Parking Management System

A college-level Advanced Object-Oriented Programming project for managing priority
parking for persons with disabilities (PWDs) and senior citizens. The system supports
verified accounts, registered vehicles, parking operations, staff monitoring,
management reports, and explainable AI-assisted parking choices.

The project uses a console-based interface and in-memory Java data structures because
GUI and RDBMS integration are optional under the course requirements.

## Target users

- Persons with disabilities
- Senior citizens
- Parking personnel
- Mall management

## Main features

- Account registration, staff verification, and rejection
- Four-digit hashed PIN authentication
- Account ID and simulated QR-token login
- Password-protected staff login with role-based menus
- Multiple vehicles per verified user
- Available, occupied, and reserved slot management
- Accessibility preferences
- Explainable PARK_NOW, WAIT_FOR_CLOSER_SLOT, and ALTERNATIVE choices
- Check-in, active parking, checkout, and personal history
- Text parking map, personnel monitoring, and management reports
- Friendly menus and input validation

No raw PIN is stored or displayed. QR tokens are opaque identifiers and contain no
personal information.

## Requirements

- JDK 17 or newer
- No third-party libraries

## Compile and run in VS Code

Open the project folder in VS Code, open its integrated PowerShell terminal, and run:

    javac -d out (Get-ChildItem -Recurse src -Filter *.java).FullName
    java -cp out Main

The application continues until 0 - Exit is selected.

Run the regression tests with:

    java -cp out testing.RegressionTest

## Demo accounts

The in-memory demo state is recreated each time the program starts.

| Account | Account ID | PIN | Vehicle |
|---|---|---|---|
| Maria Santos, verified PWD | MP-10001 | 1234 | ABC 1234, XYZ 5678 |
| Roberto Cruz, verified senior citizen | MP-10002 | 5678 | SEN 2026 |
| Lina Gomez, pending PWD | Generated after verification | 2468 | PEN 3000 |

QR tokens are randomly generated at startup. Parking personnel can verify the pending
account and then see its Account ID and QR token.

### Demo staff accounts

| Staff role | Staff ID | Password |
|---|---|---|
| Parking Personnel | STAFF-001 | park123 |
| Mall Management | ADMIN-001 | admin123 |

Select Staff Sign In from the Main Menu. Successful authentication routes the staff
member only to the menu allowed by their StaffRole. Parking personnel cannot open
management-only operations, and priority-user credentials cannot authenticate through
the staff service.

Demo parking data includes five priority slots, one occupied slot, one reserved slot,
and one completed historical session.

## Project workflow

First visit:

    Create Account
    -> Register First Vehicle
    -> Status PENDING
    -> Parking Personnel Review
    -> VERIFIED or REJECTED
    -> Generate Account ID and QR Token

Returning visit:

    Sign In
    -> Select Vehicle
    -> Select Accessibility Preference
    -> Generate AI-Assisted Choices
    -> User Selects
    -> Revalidate and Check In
    -> View Active Parking
    -> Check Out
    -> Save History

## AI-assisted recommendation

The recommendation system is an explainable rule-based decision-support feature. It
does not automatically assign a slot.

- PARK_NOW is the closest suitable available slot.
- ALTERNATIVE is another genuine available choice.
- WAIT_FOR_CLOSER_SLOT appears only when an active occupied slot is meaningfully closer
  and its estimated remaining time is no more than five minutes.

When completed history exists, the strategy uses:

    adjusted duration = 70% entered estimate + 30% historical average

Recent activity makes only a small adjustment. Internal scores are never displayed.
Waiting does not reserve a slot, and ParkingService performs the final validation.

## OOP concepts

- Encapsulation: Private fields and controlled state-changing methods.
- Staff security: Staff passwords are hashed and verified without exposing the raw
  password or stored hash.
- Inheritance: PWDUser and SeniorCitizenUser extend PriorityUser, which extends User.
- Polymorphism: RecommendationService depends on the RecommendationStrategy interface.
- Association: ParkingSession connects a user, vehicle, and parking slot.
- Aggregation: A priority user maintains registered vehicles.
- Composition: Recommendations contain options and slots contain location information.

## Package overview

    src/
    |-- Main.java
    |-- app/
    |   |-- DemoData.java
    |   '-- MallParkingApplication.java
    |-- enums/
    |-- model/
    |   |-- Vehicle.java
    |   |-- parking/
    |   '-- user/
    |-- recommendation/
    |-- service/
    |   |-- StaffService.java
    |-- testing/
    |   '-- RegressionTest.java
    '-- ui/
        |-- AccountConsole.java
        |-- ConsoleUI.java
        '-- ParkingConsole.java

All state is intentionally temporary and disappears when the program exits. No GUI,
database, networking, external API, or machine-learning library is used.
