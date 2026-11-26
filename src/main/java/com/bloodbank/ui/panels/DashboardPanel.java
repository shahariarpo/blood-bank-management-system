package com.bloodbank.ui.panels;

import com.bloodbank.ui.UIComponents;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.GridLayout;

/**
 * Dashboard layout. The figure cards are placeholders on purpose:
 * real SQLite-backed statistics arrive in Milestone 8.
 */
public class DashboardPanel extends JPanel {

    public DashboardPanel() {
        setLayout(new BorderLayout());
        setBackground(UIComponents.LIGHT_GREY);
        add(UIComponents.createPageHeader(
                "Dashboard", "Overview of your blood bank"), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setBackground(UIComponents.LIGHT_GREY);
        body.setBorder(BorderFactory.createEmptyBorder(8, 28, 24, 28));

        JPanel metrics = new JPanel(new GridLayout(1, 5, 12, 12));
        metrics.setBackground(UIComponents.LIGHT_GREY);
        metrics.add(UIComponents.createStatCard("Total donors", "—", UIComponents.DARK));
        metrics.add(UIComponents.createStatCard("Available units", "—", UIComponents.RED));
        metrics.add(UIComponents.createStatCard("Pending requests", "—", UIComponents.DARK));
        metrics.add(UIComponents.createStatCard("Fulfilled requests", "—", UIComponents.DARK));
        metrics.add(UIComponents.createStatCard("Expired units", "—", UIComponents.RED));
        body.add(metrics, BorderLayout.NORTH);

        JPanel details = new JPanel(new GridLayout(1, 2, 12, 12));
        details.setBackground(UIComponents.LIGHT_GREY);
        details.add(UIComponents.createSection(
                "Inventory by blood group",
                new JScrollPane(UIComponents.createTable(
                        new String[]{"Blood group", "Units"}))));

        JTabbedPane recent = new JTabbedPane();
        recent.addTab("Recent donations", new JScrollPane(UIComponents.createTable(
                new String[]{"ID", "Donor", "Date", "Status"})));
        recent.addTab("Recent requests", new JScrollPane(UIComponents.createTable(
                new String[]{"ID", "Patient", "Date", "Status"})));
        details.add(UIComponents.createSection("Recent activity", recent));
        body.add(details, BorderLayout.CENTER);

        body.add(UIComponents.createNoteLine(
                "Live figures will be loaded from SQLite in Milestone 8."), BorderLayout.SOUTH);

        add(body, BorderLayout.CENTER);
    }
}
