# AGENT.md — Blood Bank Management System

Project: **Blood Bank Management System (Java Swing + SQLite)**
Location: `/home/arpo/Projects/Blood Bank Management System`

## Status summary

| Milestone | Name | Status |
|---|---|---|
| 1 | Project foundation | **DONE** |
| 2 | Authentication | **DONE** |
| 3 | Main UI | **DONE** |
| 4 | Donor management | **DONE** |
| 5 | Donation management | **DONE** |
| 6 | Inventory | **DONE** |
| 7 | Patient and request management | **DONE** |
| 8 | Dashboard, reports and final testing | **NOT DONE** |

---

## Project rules (must follow for every milestone)

- Java 21, Java Swing only, SQLite + JDBC, Maven, IntelliJ IDEA Community / VS Code.
- No NetBeans, GUI builders, Spring Boot, JavaFX, web technologies, or other languages.
- Hand-written Swing components only.
- Layered architecture: **UI → Service → DAO → Database**. SQL never in Swing listeners.
- Use `PreparedStatement` for all user parameters and try-with-resources for JDBC.
- Enable `PRAGMA foreign_keys = ON` on every connection.
- Use `LocalDate` for dates, `SwingWorker` for blocking database work.
- Reuse UI components; no duplicated layouts.
- Do not hardcode dashboard statistics.
- Work **one milestone at a time**. Explain, list files, give complete code, commands, expected output, and debug before moving on.
- When modifying an existing file, provide the complete updated file, not fragments.
- Do not start the next milestone until the current one is tested successfully.

## Technology / setup

- Dependencies: `org.xerial:sqlite-jdbc:3.50.3.0`, `org.mindrot:jbcrypt:0.4`, `org.slf4j:slf4j-nop:2.0.17`.
- Maven Shade Plugin packages a runnable JAR and preserves JDBC service-loader registrations.
- Maven wrapper (`./mvnw`) is included, so no system Maven install is required.
- Database: `data/bloodbank.db` (relative to the working directory).
- Schema: `src/main/resources/database/schema.sql` (seven tables, nine indexes, `IF NOT EXISTS`).
- Default admin credentials: `admin` / `admin123` (stored as BCrypt hash, created only if the admin table is empty).
- Build/run:
  ```bash
  cd "/home/arpo/Projects/Blood Bank Management System"
  ./mvnw -q clean package
  java -jar target/blood-bank-management-system-1.0-SNAPSHOT.jar
  java -jar target/blood-bank-management-system-1.0-SNAPSHOT.jar --test-auth
  java -jar target/blood-bank-management-system-1.0-SNAPSHOT.jar --test-donors
  java -jar target/blood-bank-management-system-1.0-SNAPSHOT.jar --test-donations
  java -jar target/blood-bank-management-system-1.0-SNAPSHOT.jar --test-inventory
  java -jar target/blood-bank-management-system-1.0-SNAPSHOT.jar --test-requests
  ```

---

## Milestone 1 — Project foundation — **DONE**

- [x] Set up Maven (`pom.xml`, Java 21 via `maven.compiler.release`).
- [x] Configure SQLite JDBC, jBCrypt, slf4j-nop dependencies.
- [x] Create the package structure (`database`, `model`, `dao`, `service`, `ui`, `util`, `ui.panels`, `ui.dialogs`).
- [x] Implement `DatabaseConnection` with `PRAGMA foreign_keys = ON` per connection.
- [x] Implement `DatabaseInitializer` running `schema.sql` from the classpath, idempotently.
- [x] Create all seven tables: `admin`, `donors`, `donations`, `blood_units`, `patients`, `blood_requests`, `blood_issues`.
- [x] Add indexes for blood group, unit status, expiry date, request status, donation history.
- [x] Test connectivity and CRUD (temporary console test in `Main.java`).
- Verified: seven tables found, nine indexes, `foreign_keys = 1`, CRUD smoke test passed.

## Milestone 2 — Authentication — **DONE**

- [x] `Admin` model, `AdminDAO`, `AuthService`, `PasswordUtil`, `Validators`.
- [x] Default admin created only when the admin table is empty.
- [x] BCrypt hashing (cost 12); no plaintext passwords stored.
- [x] Login validation and session state (`currentAdmin`, `isLoggedIn`, `logout`).
- [x] Hand-written `LoginFrame` using `SwingWorker` for database access.
- [x] Console smoke test via `--test-auth` (correct login, wrong password, blank username, logout, BCrypt hash check).
- [x] Logout implemented (later moved into `MainFrame` in Milestone 3).
- Verified: `admin` / `admin123` works; hash starts with `$2a$12$`; console test passes on repeat runs.

## Milestone 3 — Main UI — **DONE**

- [x] `MainFrame` with dark sidebar and light content area.
- [x] `CardLayout` navigation across six cards.
- [x] Dashboard skeleton: five metric cards, inventory table, `JTabbedPane` for recent activity.
- [x] Placeholder panels for Donors, Donations, Inventory, Requests, Reports.
- [x] Reusable `UIComponents` factory (colors, fonts, buttons, cards, tables, placeholders).
- [x] `LoginFrame` updated to dispose itself and open `MainFrame`.
- [x] Logout confirmation in `MainFrame` returning to `LoginFrame`.
- [x] Dashboard values are placeholders (`—`) with a note; real statistics deferred to Milestone 8.
- Verified: clean build, GUI starts without exceptions, `--test-auth` still passes.

## Milestone 4 — Donor management — **DONE**

- [x] `Donor` model matching `donor_id, name, age, gender, blood_group, phone, address, created_at`.
- [x] `DonorDAO` with CRUD and search by name, ID, phone, or blood group.
- [x] `DonorService` with validation (age 18-100, blood group, phone 7-15 digits) and duplicate-phone handling.
- [x] `DonorDialog` for add/edit (manual Swing, `JDialog`, `SwingWorker` save).
- [x] Real `DonorPanel` with `JTable`, search/clear/refresh, add/edit/delete actions.
- [x] `DonorProfileDialog` with donor details and donation history (read-only `DonationDAO.findByDonorId`).
- [x] Delete only when referential integrity allows; friendly error when donations reference the donor.
- [x] Console smoke test `--test-donors` (add, age validation, duplicate, search, update, FK delete protection, delete).
- Blood groups: `A+, A-, B+, B-, AB+, AB-, O+, O-`; age >= 18.
- Note: `DonationDAO.findByDonorId` is now shared by the donor profile and the full donation module.

## Milestone 5 — Donation management — **DONE**

- [x] `Donation` model expanded with donor name/blood group for joined history queries.
- [x] `BloodComponent` enum with illustrative shelf lives (whole blood 35, red cells 42, plasma 365, platelets 5 days).
- [x] `BloodUnit` model and minimal `BloodUnitDAO` insert/duplicate guard (listing/filtering deferred to Milestone 6).
- [x] `DonationDAO` with joined findAll/findByDonorId/search/findById, insert, transactional accept and reject.
- [x] `DonationService` with screening status rules (`PENDING`, `ACCEPTED`, `REJECTED`), date/quantity validation and component expiry calculation.
- [x] `DonationDialog` for recording donations (donor, date, quantity, screening status, component, notes).
- [x] `DonationProcessDialog` for accepting or rejecting a PENDING donation.
- [x] Real `DonationPanel` with searchable donation history and Process action enabled only for PENDING rows.
- [x] Only `ACCEPTED` donations create `AVAILABLE` blood units; REJECTED/PENDING do not.
- [x] Accept and unit insertion happen in one SQL transaction; accepted/already-processed donations cannot be processed again.
- [x] Console smoke test `--test-donations` (pending, accept + expiry, duplicate process, accepted-on-record, rejected, future date, invalid quantity).
- Verified: clean build, console tests pass, test data is cleaned up, GUI launches without exceptions.
- Note: expiry periods are illustrative; real laboratory policy governs clinical use.

## Milestone 6 — Inventory — **DONE**

- [x] `BloodUnit` expanded with joined-query donor name and mutable status.
- [x] `BloodUnitDAO` expanded with joined listing, keyword/group/status filters, find-by-ID,
      FEFO selection, expiry flagging, manual status updates, group counts and status counts.
- [x] `InventoryService` with refresh-statuses, filtering, stock counts, expiry monitoring,
      quarantine/release rules, and FEFO selection for future issuing.
- [x] Real `InventoryPanel` replaces the placeholder: unit table, search, group/status filters,
      available-by-group summary, expiry monitor, and quarantine/release actions.
- [x] Statuses: `AVAILABLE`, `ISSUED`, `EXPIRED`, `QUARANTINED`; only AVAILABLE/QUARANTINED
      can be changed manually; ISSUED/EXPIRED are system-controlled.
- [x] FEFO (first-expiring-first-out) selects only unexpired AVAILABLE units, ordered by
      expiry date then unit ID.
- [x] Inventory is calculated from individual unit rows; there is no stored stock counter.
- [x] Console smoke test `--test-inventory` (FEFO order, derived counts, expiry detection,
      quarantine/release, ID search, insufficient stock, invalid status change).
- [x] Test uses an empty blood group selected at runtime so it does not depend on user data.
- Verified: clean build, all four console tests pass, test data is removed, GUI launches without exceptions.
- Note: FEFO here only selects units; Milestone 7 will perform transactional issuing.

## Milestone 7 — Patient and request management — **DONE**

- [x] `Patient` model, `PatientDAO`, `PatientService`, `PatientDialog` and real `PatientPanel`.
- [x] Added a **Patients** card to `MainFrame` sidebar.
- [x] `BloodRequest` model, joined `BloodRequestDAO`, `RequestService`, `RequestDialog`.
- [x] `BloodIssue` model and joined `BloodIssueDAO` for issuing history.
- [x] Real `RequestPanel` with requests table, status filter, search, approve/reject/issue actions and issuing history.
- [x] Priorities: `NORMAL`, `URGENT`, `CRITICAL`; request statuses tracked through PENDING/APPROVED/PARTIALLY_FULFILLED/FULFILLED/REJECTED/CANCELLED.
- [x] `BloodCompatibility` utility with illustrative ABO/Rh donor-group rules; explicitly not clinical authorization.
- [x] `IssueDialog` previews eligible FEFO units, asks for issue quantity and requires explicit partial-issue consent.
- [x] `IssueService.issue()` verifies request status, excludes expired/quarantined units, selects compatible units by FEFO, updates units to ISSUED, records `blood_issues`, and updates request status in **one SQL transaction** with rollback on failure.
- [x] Duplicate issuing is prevented by status gating and the existing UNIQUE constraint on `blood_issues.unit_id`.
- [x] Console smoke test `--test-requests` (compatibility rules, FK protection, status guard, full FEFO issue, duplicate rejection, partial-issue guard, partial issue, depleted stock rejection).
- [x] UI note: ABO/Rh compatibility is **not** clinical authorization; real transfusion needs laboratory crossmatching and clinical approval.

## Milestone 8 — Dashboard, reports and final testing — **NOT DONE**

- [ ] Replace dashboard placeholders with real SQLite-backed statistics:
      total donors, available units, pending requests, fulfilled requests, group-wise inventory, recent donations, recent requests, expired units.
- [ ] `ReportsPanel` with donor totals, donations, inventory by group, expired units, pending/fulfilled requests, donation history, issuing history.
- [ ] Input validation and exception handling polish.
- [ ] Database backup feature.
- [ ] Full test pass: login correct/incorrect, donor validation/duplicates, donation acceptance/rejection, blood unit creation, inventory calculations, expired detection, request create/approve/reject, issuing unavailable blood, duplicate issuing prevention, fulfillment status, persistence after restart, foreign keys, transaction rollback.
- [ ] Final runnable JAR verification.
- Note: runnable JAR packaging and BCrypt were already set up in Milestones 1–2; the final verification is still pending.
