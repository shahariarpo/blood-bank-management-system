# Blood Bank Management System

A desktop Blood Bank Management System built with **Java 21 Swing** and **SQLite**. It manages donors, donations, blood units, patients, requests, approvals, reservations, issuing, reports, and database backups through a hand-written Swing GUI.

> Compatibility rules in this project are illustrative only. Real transfusion requires laboratory crossmatching and clinical authorization.

## Tech stack

| Area | Technology |
|---|---|
| Language / runtime | Java 21 |
| GUI | Java Swing, hand-written components (no GUI builder) |
| Database | SQLite via `org.xerial:sqlite-jdbc:3.50.3.0` |
| Password hashing | jBCrypt `0.4` (cost 12; only the hash is stored) |
| Logging | `slf4j-nop:2.0.17` to silence SQLite JDBC warnings |
| Build | Maven wrapper 3.3.4 / Maven 3.9.9 |
| Build plugins | Maven Compiler Plugin `3.13.0`, Maven Shade Plugin `3.6.0` |
| Dates | `java.time.LocalDate` |
| Background work | `SwingWorker` |
| Recommended editor | VS Code with the Java Extension Pack and SQLite Viewer |

The application is deliberately **100% Java**: no Spring Boot, JavaFX, web technologies, NetBeans, or GUI builders.

## Architecture

```text
Swing UI  ->  Service  ->  DAO  ->  SQLite
```

- SQL lives in DAO classes, never in Swing listeners.
- All user parameters use `PreparedStatement`.
- JDBC resources use try-with-resources.
- `PRAGMA foreign_keys = ON` is enabled on every connection.
- Issuing and approval use SQL transactions with commit/rollback.
- Inventory is derived from individual `blood_units` rows; there is no stored stock counter.
- Issuing uses FEFO (first-expiring-first-out) and consumes approved reservations first.

## Project file structure

```text
Blood Bank Management System/
├── .github/
│   └── workflows/
│       └── release.yml                  # Builds AppImage + EXE on version tags
├── .mvn/
│   └── wrapper/
│       └── maven-wrapper.properties     # Maven 3.9.9 wrapper configuration
├── .vscode/
│   ├── extensions.json                  # Recommended VS Code extensions
│   └── tasks.json                       # Build, run, and console-test tasks
├── src/
│   └── main/
│       ├── java/
│       │   └── com/bloodbank/
│       │       ├── Main.java             # Entry point and console smoke tests
│       │       ├── database/
│       │       │   ├── DatabaseConnection.java
│       │       │   └── DatabaseInitializer.java
│       │       ├── model/
│       │       │   ├── Admin.java
│       │       │   ├── BloodComponent.java
│       │       │   ├── BloodIssue.java
│       │       │   ├── BloodRequest.java
│       │       │   ├── BloodReservation.java
│       │       │   ├── BloodUnit.java
│       │       │   ├── Donation.java
│       │       │   ├── Donor.java
│       │       │   └── Patient.java
│       │       ├── dao/
│       │       │   ├── AdminDAO.java
│       │       │   ├── BloodIssueDAO.java
│       │       │   ├── BloodRequestDAO.java
│       │       │   ├── BloodReservationDAO.java
│       │       │   ├── BloodUnitDAO.java
│       │       │   ├── DonationDAO.java
│       │       │   ├── DonorDAO.java
│       │       │   └── PatientDAO.java
│       │       ├── service/
│       │       │   ├── AuthService.java
│       │       │   ├── BackupService.java
│       │       │   ├── DonationService.java
│       │       │   ├── DonorService.java
│       │       │   ├── InventoryService.java
│       │       │   ├── IssueService.java
│       │       │   ├── PatientService.java
│       │       │   ├── ReportService.java
│       │       │   ├── RequestService.java
│       │       │   └── ReservationService.java
│       │       ├── ui/
│       │       │   ├── LoginFrame.java
│       │       │   ├── MainFrame.java
│       │       │   ├── UIComponents.java
│       │       │   ├── dialogs/
│       │       │   │   ├── DonationDialog.java
│       │       │   │   ├── DonationProcessDialog.java
│       │       │   │   ├── DonorDialog.java
│       │       │   │   ├── DonorProfileDialog.java
│       │       │   │   ├── IssueDialog.java
│       │       │   │   ├── PatientDialog.java
│       │       │   │   └── RequestDialog.java
│       │       │   └── panels/
│       │       │       ├── DashboardPanel.java
│       │       │       ├── DonationPanel.java
│       │       │       ├── DonorPanel.java
│       │       │       ├── InventoryPanel.java
│       │       │       ├── PatientPanel.java
│       │       │       ├── ReportsPanel.java
│       │       │       └── RequestPanel.java
│       │       └── util/
│       │           ├── BloodCompatibility.java
│       │           ├── PasswordUtil.java
│       │           └── Validators.java
│       └── resources/
│           └── database/
│               └── schema.sql              # Idempotent schema for eight tables
├── packaging/
│   ├── AppRun                              # AppImage entry point
│   ├── bloodbank.desktop                   # Linux desktop integration
│   ├── bloodbank.png                       # Application icon
│   └── launcher.sh                         # Starts the bundled Java runtime
├── .gitignore
├── mvnw                                    # Linux/macOS Maven wrapper
├── mvnw.cmd                                # Windows Maven wrapper
├── pom.xml
├── data/                                   # Generated on first run
│   └── bloodbank.db
└── target/                                 # Generated by Maven
    └── blood-bank-management-system-1.0-SNAPSHOT.jar
```

`data/*.db`, `target/`, and VS Code Java output (`out/`) are ignored by Git.

## Prerequisites

- **JDK 21** installed and available as `java`.
- Internet access the first time the Maven wrapper downloads Maven and dependencies.
- Run commands from the project root so the relative `data/` directory is created in the right place.

Check your Java version:

```bash
java -version
```

Expected output should include `version "21..."` (or a newer compatible JDK configured for release 21).

## Build and run on Linux / macOS

From the project root:

```bash
cd "/path/to/Blood Bank Management System"

# If the wrapper is not executable yet:
chmod +x mvnw

# Build the runnable JAR
./mvnw -q clean package

# Start the Swing application
java -jar target/blood-bank-management-system-1.0-SNAPSHOT.jar
```

The database is created automatically at:

```text
/path/to/Blood Bank Management System/data/bloodbank.db
```

## Build and run on Windows

### PowerShell

```powershell
cd "C:\path\to\Blood Bank Management System"

# Build the runnable JAR
.\mvnw.cmd -q clean package

# Start the Swing application
java -jar target\blood-bank-management-system-1.0-SNAPSHOT.jar
```

### Command Prompt

```cmd
cd /d "C:\path\to\Blood Bank Management System"

mvnw.cmd -q clean package
java -jar target\blood-bank-management-system-1.0-SNAPSHOT.jar
```

The database is created automatically at:

```text
C:\path\to\Blood Bank Management System\data\bloodbank.db
```

## Default login

| Username | Password |
|---|---|
| `admin` | `admin123` |

Only a BCrypt hash is stored. The default admin is inserted only when the `admin` table is empty.

## Console smoke tests

The application also has built-in non-GUI smoke tests. They create temporary test rows and remove them afterward.

Linux / macOS:

```bash
java -jar target/blood-bank-management-system-1.0-SNAPSHOT.jar --test-auth
java -jar target/blood-bank-management-system-1.0-SNAPSHOT.jar --test-donors
java -jar target/blood-bank-management-system-1.0-SNAPSHOT.jar --test-donations
java -jar target/blood-bank-management-system-1.0-SNAPSHOT.jar --test-inventory
java -jar target/blood-bank-management-system-1.0-SNAPSHOT.jar --test-requests
java -jar target/blood-bank-management-system-1.0-SNAPSHOT.jar --test-reports
java -jar target/blood-bank-management-system-1.0-SNAPSHOT.jar --test-reservations
```

Windows PowerShell:

```powershell
java -jar target\blood-bank-management-system-1.0-SNAPSHOT.jar --test-auth
java -jar target\blood-bank-management-system-1.0-SNAPSHOT.jar --test-donors
java -jar target\blood-bank-management-system-1.0-SNAPSHOT.jar --test-donations
java -jar target\blood-bank-management-system-1.0-SNAPSHOT.jar --test-inventory
java -jar target\blood-bank-management-system-1.0-SNAPSHOT.jar --test-requests
java -jar target\blood-bank-management-system-1.0-SNAPSHOT.jar --test-reports
java -jar target\blood-bank-management-system-1.0-SNAPSHOT.jar --test-reservations
```

| Argument | What it checks |
|---|---|
| `--test-auth` | Login, logout, BCrypt hash, invalid credentials |
| `--test-donors` | Donor add/search/update/delete and validation |
| `--test-donations` | Screening rules and unit creation on acceptance |
| `--test-inventory` | FEFO, stock counts, expiry, quarantine/release |
| `--test-requests` | Requests, issuing, partial issue, dashboard count update |
| `--test-reports` | Dashboard/report figures and backup verification |
| `--test-reservations` | Reserve, double-reserve block, issue, reject, expiry, cancel |

## VS Code tasks

`.vscode/tasks.json` includes tasks for:

- Build and run application
- Authentication console test
- Donor console test
- Donation console test
- Inventory console test
- Request and issuing console test
- Report and backup console test
- Reservation console test
- Run existing JAR without rebuilding

Open the project folder in VS Code and use **Terminal → Run Task…**.

**Ctrl+Shift+B** runs the default task, **BBMS: Build and run application**. The tasks are cross-platform: Linux/macOS use `./mvnw`, while Windows uses `mvnw.cmd` through `cmd.exe`. A fresh clone therefore builds and starts the GUI with one shortcut, provided JDK 21 is installed and the first Maven wrapper download can reach the internet.

## Release binaries

Pushing a version tag automatically builds two binaries and publishes them as a GitHub Release:

| Platform | Artifact |
|---|---|
| Linux | `BloodBankManagementSystem-<version>.AppImage` |
| Windows | `BloodBankManagementSystem-<version>.exe` |

Both artifacts bundle a Java 21 runtime, so end users do not need to install Java separately.

```bash
git tag v1.0.0
git push origin v1.0.0
```

The workflow is `.github/workflows/release.yml`. It builds the shaded JAR, uses `jpackage` for the Windows installer, wraps the Linux application image with `linuxdeploy`, then attaches both files to the release. You can also run the workflow manually from the Actions tab to verify the build without publishing a release.

Linux users can run the AppImage directly:

```bash
chmod +x BloodBankManagementSystem-1.0.0.AppImage
./BloodBankManagementSystem-1.0.0.AppImage
```

Windows users run the `.exe` installer; it creates a Start Menu entry and allows choosing the install directory.

## Database and backups

- Schema: `src/main/resources/database/schema.sql`
- All DDL uses `IF NOT EXISTS`, so startup is idempotent.
- Eight application tables: `admin`, `donors`, `donations`, `blood_units`, `patients`, `blood_requests`, `blood_issues`, `blood_reservations`.
- Backups are created from **Reports → Backup database** using SQLite `VACUUM INTO`.
- Backup files are timestamped, for example `bloodbank-20261126-160852.db`.

## Troubleshooting

| Problem | Fix |
|---|---|
| `java -version` is not 21 | Install JDK 21 and ensure `java` points to it. |
| `mvnw: Permission denied` on Linux/macOS | Run `chmod +x mvnw`. |
| Windows cannot find `mvnw.cmd` | Use `.\mvnw.cmd` in PowerShell, or `mvnw.cmd` in CMD. |
| `Database ready` path is unexpected | Run the JAR from the project root; the database path is relative to the working directory. |
| GUI does not appear on a headless Linux server | A desktop environment is required; use the console smoke tests there. |
| Foreign-key errors when deleting records | Delete dependent rows first; the UI shows a friendly error for protected records. |
