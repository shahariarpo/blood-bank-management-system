package com.bloodbank.ui;

import com.bloodbank.model.Admin;
import com.bloodbank.service.AuthService;
import com.bloodbank.util.Validators;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/** Hand-written Swing login screen (no GUI builder). */
public class LoginFrame extends JFrame {

    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private final JButton loginButton = new JButton("Sign in");

    public LoginFrame() {
        super("Blood Bank Management System - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setSize(470, 330);
        setLocationRelativeTo(null);

        add(buildLoginPanel(), BorderLayout.CENTER);
        getRootPane().setDefaultButton(loginButton);
        loginButton.addActionListener(event -> performLogin());
        setVisible(true);
    }

    private JPanel buildLoginPanel() {
        JPanel panel = new JPanel(new BorderLayout(16, 16));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(26, 34, 26, 34));

        JLabel title = new JLabel("Blood Bank Management", SwingConstants.CENTER);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        title.setForeground(UIComponents.RED);
        JLabel subtitle = new JLabel("Administrator sign in", SwingConstants.CENTER);
        subtitle.setForeground(UIComponents.MUTED);

        JPanel header = new JPanel(new GridLayout(2, 1, 0, 4));
        header.setBackground(Color.WHITE);
        header.add(title);
        header.add(subtitle);
        panel.add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(6, 6, 6, 6);

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.LINE_END;
        form.add(new JLabel("Username:"), constraints);

        constraints.gridx = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        form.add(usernameField, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.anchor = GridBagConstraints.LINE_END;
        form.add(new JLabel("Password:"), constraints);

        constraints.gridx = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        form.add(passwordField, constraints);
        panel.add(form, BorderLayout.CENTER);

        loginButton.setBackground(UIComponents.RED);
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setBorderPainted(false);
        loginButton.setOpaque(true);
        loginButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        loginButton.setPreferredSize(new Dimension(150, 36));

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setBackground(Color.WHITE);
        footer.add(loginButton);
        panel.add(footer, BorderLayout.SOUTH);

        return panel;
    }

    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (Validators.isBlank(username) || Validators.isBlank(password)) {
            JOptionPane.showMessageDialog(this,
                    "Please enter both username and password.",
                    "Validation error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        loginButton.setEnabled(false);
        loginButton.setText("Signing in...");
        new LoginWorker(username, password).execute();
    }

    /** Database work runs off the Event Dispatch Thread so the UI stays responsive. */
    private class LoginWorker extends SwingWorker<Optional<Admin>, Void> {

        private final String username;
        private final String password;

        LoginWorker(String username, String password) {
            this.username = username;
            this.password = password;
        }

        @Override
        protected Optional<Admin> doInBackground() throws Exception {
            return AuthService.login(username, password);
        }

        @Override
        protected void done() {
            loginButton.setEnabled(true);
            loginButton.setText("Sign in");

            try {
                Optional<Admin> result = get();
                if (result.isPresent()) {
                    passwordField.setText("");
                    dispose();
                    SwingUtilities.invokeLater(() -> new MainFrame());
                } else {
                    JOptionPane.showMessageDialog(LoginFrame.this,
                            "Invalid username or password.",
                            "Login failed", JOptionPane.ERROR_MESSAGE);
                    passwordField.setText("");
                    passwordField.requestFocus();
                }
            } catch (ExecutionException e) {
                JOptionPane.showMessageDialog(LoginFrame.this,
                        "Database error: " + e.getCause().getMessage(),
                        "Login failed", JOptionPane.ERROR_MESSAGE);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
