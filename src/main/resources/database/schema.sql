-- Blood Bank Management System - schema (Milestone 1)
-- Safe to run on every startup: every statement uses IF NOT EXISTS.

CREATE TABLE IF NOT EXISTS admin (
    admin_id      INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS donors (
    donor_id    INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT NOT NULL,
    age         INTEGER NOT NULL CHECK(age >= 18),
    gender      TEXT NOT NULL CHECK(gender IN ('Male', 'Female', 'Other')),
    blood_group TEXT NOT NULL CHECK(blood_group IN ('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-')),
    phone       TEXT NOT NULL,
    address     TEXT,
    created_at  TEXT DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS donations (
    donation_id      INTEGER PRIMARY KEY AUTOINCREMENT,
    donor_id         INTEGER NOT NULL,
    donation_date    TEXT NOT NULL,
    quantity_ml      INTEGER NOT NULL CHECK(quantity_ml > 0),
    screening_status TEXT NOT NULL CHECK(screening_status IN ('PENDING', 'ACCEPTED', 'REJECTED')),
    notes            TEXT,
    FOREIGN KEY (donor_id) REFERENCES donors(donor_id)
);

CREATE TABLE IF NOT EXISTS blood_units (
    unit_id         INTEGER PRIMARY KEY AUTOINCREMENT,
    donation_id     INTEGER NOT NULL,
    blood_group     TEXT NOT NULL CHECK(blood_group IN ('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-')),
    component_type  TEXT NOT NULL DEFAULT 'WHOLE_BLOOD',
    collection_date TEXT NOT NULL,
    expiry_date     TEXT NOT NULL,
    status          TEXT NOT NULL DEFAULT 'AVAILABLE'
                    CHECK(status IN ('AVAILABLE', 'ISSUED', 'EXPIRED', 'QUARANTINED')),
    FOREIGN KEY (donation_id) REFERENCES donations(donation_id),
    CHECK(expiry_date > collection_date)
);

CREATE TABLE IF NOT EXISTS patients (
    patient_id  INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT NOT NULL,
    age         INTEGER NOT NULL,
    gender      TEXT NOT NULL CHECK(gender IN ('Male', 'Female', 'Other')),
    blood_group TEXT NOT NULL CHECK(blood_group IN ('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-')),
    phone       TEXT NOT NULL,
    hospital    TEXT
);

CREATE TABLE IF NOT EXISTS blood_requests (
    request_id   INTEGER PRIMARY KEY AUTOINCREMENT,
    patient_id   INTEGER NOT NULL,
    blood_group  TEXT NOT NULL CHECK(blood_group IN ('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-')),
    quantity     INTEGER NOT NULL CHECK(quantity > 0),
    request_date TEXT NOT NULL,
    priority     TEXT NOT NULL DEFAULT 'NORMAL'
                 CHECK(priority IN ('NORMAL', 'URGENT', 'CRITICAL')),
    status       TEXT NOT NULL DEFAULT 'PENDING'
                 CHECK(status IN ('PENDING', 'APPROVED', 'PARTIALLY_FULFILLED',
                                  'FULFILLED', 'REJECTED', 'CANCELLED')),
    FOREIGN KEY (patient_id) REFERENCES patients(patient_id)
);

CREATE TABLE IF NOT EXISTS blood_issues (
    issue_id   INTEGER PRIMARY KEY AUTOINCREMENT,
    request_id INTEGER NOT NULL,
    unit_id    INTEGER NOT NULL UNIQUE,
    issue_date TEXT NOT NULL,
    FOREIGN KEY (request_id) REFERENCES blood_requests(request_id),
    FOREIGN KEY (unit_id) REFERENCES blood_units(unit_id)
);

-- Approval reserves units without changing their stored status.
-- A unit can be reserved for only one request at a time.
CREATE TABLE IF NOT EXISTS blood_reservations (
    reservation_id INTEGER PRIMARY KEY AUTOINCREMENT,
    request_id     INTEGER NOT NULL,
    unit_id        INTEGER NOT NULL UNIQUE,
    reserved_on    TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (request_id) REFERENCES blood_requests(request_id),
    FOREIGN KEY (unit_id) REFERENCES blood_units(unit_id)
);

-- Indexes for the filters and lookups the application will use later.
CREATE INDEX IF NOT EXISTS idx_donors_blood_group ON donors(blood_group);
CREATE INDEX IF NOT EXISTS idx_donations_donor_id ON donations(donor_id);
CREATE INDEX IF NOT EXISTS idx_donations_date ON donations(donation_date);
CREATE INDEX IF NOT EXISTS idx_blood_units_donation_id ON blood_units(donation_id);
CREATE INDEX IF NOT EXISTS idx_blood_units_group_status ON blood_units(blood_group, status);
CREATE INDEX IF NOT EXISTS idx_blood_units_expiry ON blood_units(expiry_date);
CREATE INDEX IF NOT EXISTS idx_requests_patient_id ON blood_requests(patient_id);
CREATE INDEX IF NOT EXISTS idx_requests_status ON blood_requests(status);
CREATE INDEX IF NOT EXISTS idx_issues_request_id ON blood_issues(request_id);
CREATE INDEX IF NOT EXISTS idx_reservations_request_id ON blood_reservations(request_id);
CREATE INDEX IF NOT EXISTS idx_reservations_unit_id ON blood_reservations(unit_id);
