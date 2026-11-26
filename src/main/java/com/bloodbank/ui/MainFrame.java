package com.bloodbank.ui;

import com.bloodbank.service.AuthService;
import com.bloodbank.ui.panels.DashboardPanel;
import com.bloodbank.ui.panels.DonationPanel;
import com.bloodbank.ui.panels.DonorPanel;
import com.bloodbank.ui.panels.InventoryPanel;
import com.bloodbank.ui.panels.PatientPanel;
import com.bloodbank.ui.panels.ReportsPanel;
import com.bloodbank.ui.panels.RequestPanel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.LinkedHashMap;
import java.util.Map;

/** Main application window: dark sidebar + CardLayout content area. */
public class MainFrame extends JFrame {

    private static final String DASHBOARD = "dashboard";
    private static final String DONORS = "donors";
    private static final String PATIENTS = "patients";
    private static final String DONATIONS = "donations";
    private static final String INVENTORY = "inventory";
    private static final String REQUESTS = "requests";
    private static final String REPORTS = "reports";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);
    private final Map<String, JButton> navigationButtons = new LinkedHashMap<>();
    private final DashboardPanel dashboardPanel = new DashboardPanel();
    private final ReportsPanel reportsPanel = new ReportsPanel();
    private boolean navigationReady;

    public MainFrame() {
        super("Blood Bank Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 760);
        setMinimumSize(new Dimension(980, 640));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(createSidebar(), BorderLayout.WEST);

        contentPanel.setBackground(UIComponents.LIGHT_GREY);
        contentPanel.add(dashboardPanel, DASHBOARD);
        contentPanel.add(new DonorPanel(), DONORS);
        contentPanel.add(new PatientPanel(), PATIENTS);
        contentPanel.add(new DonationPanel(), DONATIONS);
        contentPanel.add(new InventoryPanel(), INVENTORY);
        contentPanel.add(new RequestPanel(), REQUESTS);
        contentPanel.add(reportsPanel, REPORTS);
        add(contentPanel, BorderLayout.CENTER);

        showCard(DASHBOARD);
        setVisible(true);
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(UIComponents.DARK);
        sidebar.setPreferredSize(new Dimension(230, 0));

        sidebar.add(createBrand());
        sidebar.add(Box.createVerticalStrut(26));

        Map<String, String> navigation = new LinkedHashMap<>();
        navigation.put("Dashboard", DASHBOARD);
        navigation.put("Donors", DONORS);
        navigation.put("Patients", PATIENTS);
        navigation.put("Donations", DONATIONS);
        navigation.put("Inventory", INVENTORY);
        navigation.put("Requests", REQUESTS);
        navigation.put("Reports", REPORTS);

        navigation.forEach((label, card) -> {
            JButton button = UIComponents.createSidebarButton(label);
            button.addActionListener(event -> showCard(card));
            navigationButtons.put(card, button);
            sidebar.add(button);
            sidebar.add(Box.createVerticalStrut(6));
        });

        sidebar.add(Box.createVerticalGlue());

        String username = AuthService.getCurrentAdmin() == null
                ? "Administrator" : AuthService.getCurrentAdmin().getUsername();
        JLabel userLabel = new JLabel("Signed in as " + username, SwingConstants.CENTER);
        userLabel.setForeground(new Color(148, 163, 184));
        userLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        userLabel.setAlignmentX(CENTER_ALIGNMENT);
        sidebar.add(userLabel);
        sidebar.add(Box.createVerticalStrut(12));

        JButton logoutButton = UIComponents.createPrimaryButton("Logout");
        logoutButton.setAlignmentX(CENTER_ALIGNMENT);
        logoutButton.setMaximumSize(new Dimension(180, 42));
        logoutButton.addActionListener(event -> logout());
        sidebar.add(logoutButton);
        sidebar.add(Box.createVerticalStrut(22));

        return sidebar;
    }

    private JPanel createBrand() {
        JPanel brand = new JPanel();
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setBackground(UIComponents.DARK);
        brand.setBorder(BorderFactory.createEmptyBorder(26, 20, 18, 20));

        JLabel name = new JLabel("BLOOD BANK", SwingConstants.LEFT);
        name.setForeground(Color.WHITE);
        name.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        name.setAlignmentX(LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("MANAGEMENT SYSTEM", SwingConstants.LEFT);
        subtitle.setForeground(UIComponents.RED);
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        subtitle.setAlignmentX(LEFT_ALIGNMENT);

        JPanel stripe = UIComponents.createPanel(UIComponents.RED);
        stripe.setMaximumSize(new Dimension(48, 4));
        stripe.setAlignmentX(LEFT_ALIGNMENT);

        brand.add(name);
        brand.add(Box.createVerticalStrut(3));
        brand.add(subtitle);
        brand.add(Box.createVerticalStrut(14));
        brand.add(stripe);
        return brand;
    }

    private void showCard(String card) {
        cardLayout.show(contentPanel, card);
        navigationButtons.forEach((name, button) ->
                button.setBackground(name.equals(card) ? UIComponents.RED : UIComponents.DARK));

        // Panels load once in their constructors. Refresh live summaries when revisited.
        if (navigationReady) {
            if (DASHBOARD.equals(card)) {
                dashboardPanel.reload();
            } else if (REPORTS.equals(card)) {
                reportsPanel.reload();
            }
        }
        navigationReady = true;
    }

    private void logout() {
        int answer = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to log out?", "Logout",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        AuthService.logout();
        dispose();
        SwingUtilities.invokeLater(() -> new LoginFrame());
    }
}
