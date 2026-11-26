package com.bloodbank.service;

import com.bloodbank.dao.AdminDAO;
import com.bloodbank.model.Admin;
import com.bloodbank.util.PasswordUtil;
import com.bloodbank.util.Validators;

import java.sql.SQLException;
import java.util.Optional;

/** Business logic for login, default admin setup and session state. */
public final class AuthService {

    public static final String DEFAULT_USERNAME = "admin";
    public static final String DEFAULT_PASSWORD = "admin123";

    private static Admin currentAdmin;

    private AuthService() {
        // Service class - no instances.
    }

    /**
     * Creates the default administrator only when the admin table is empty.
     * Returns true when a new account was created.
     */
    public static boolean ensureDefaultAdmin() throws SQLException {
        if (AdminDAO.count() > 0) {
            return false;
        }
        Admin admin = new Admin(0, DEFAULT_USERNAME, PasswordUtil.hash(DEFAULT_PASSWORD));
        admin.setAdminId(AdminDAO.insert(admin));
        return true;
    }

    /**
     * Checks credentials and starts a session on success.
     * Blank/invalid usernames are programming/UI errors, so they throw;
     * wrong credentials simply return an empty Optional.
     */
    public static Optional<Admin> login(String username, String password) throws SQLException {
        if (!Validators.isValidUsername(username)) {
            throw new IllegalArgumentException(
                    "Username must be 3-30 letters, digits or underscores.");
        }
        if (Validators.isBlank(password)) {
            throw new IllegalArgumentException("Password is required.");
        }

        Optional<Admin> found = AdminDAO.findByUsername(username.trim());
        if (found.isPresent() && PasswordUtil.matches(password, found.get().getPasswordHash())) {
            currentAdmin = found.get();
            return found;
        }
        return Optional.empty();
    }

    public static boolean isLoggedIn() {
        return currentAdmin != null;
    }

    public static Admin getCurrentAdmin() {
        return currentAdmin;
    }

    public static void logout() {
        currentAdmin = null;
    }
}
