package com.bloodbank.ui.panels;

import com.bloodbank.model.BloodRequest;
import com.bloodbank.model.Donation;
import com.bloodbank.service.ReportService;
import com.bloodbank.ui.UIComponents;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/** Live dashboard backed entirely by SQLite through ReportService. */
public class DashboardPanel extends JPanel {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final UIComponents.StatCard donorCard =
            UIComponents.createStatCard("Total donors", "…", UIComponents.DARK);
    private final UIComponents.StatCard availableCard =
            UIComponents.createStatCard("Available units", "…", UIComponents.RED);
    private final UIComponents.StatCard pendingCard =
            UIComponents.createStatCard("Pending requests", "…", UIComponents.DARK);
    private final UIComponents.StatCard fulfilledCard =
            UIComponents.createStatCard("Fulfilled requests", "…", UIComponents.DARK);
    private final UIComponents.StatCard expiredCard =
            UIComponents.createStatCard("Expired units", "…", UIComponents.RED);

    private final JTable inventoryTable = UIComponents.createTable(
            new String[]{"Blood group", "Available units"});
    private final DefaultTableModel inventoryModel = (DefaultTableModel) inventoryTable.getModel();
    private final JTable donationTable = UIComponents.createTable(
            new String[]{"ID", "Donor", "Date", "Status"});
    private final DefaultTableModel donationModel = (DefaultTableModel) donationTable.getModel();
    private final JTable requestTable = UIComponents.createTable(
            new String[]{"ID", "Patient", "Date", "Status"});
    private final DefaultTableModel requestModel = (DefaultTableModel) requestTable.getModel();

    private final JButton refreshButton = new JButton("Refresh");
    private final JLabel updatedLabel = UIComponents.createSubtitle("Loading live figures...");
    private boolean busy;

    public DashboardPanel() {
        setLayout(new BorderLayout());
        setBackground(UIComponents.LIGHT_GREY);

        JPanel top = new JPanel(new BorderLayout());
        top.add(UIComponents.createPageHeader(
                "Dashboard", "Overview of your blood bank"), BorderLayout.NORTH);
        top.add(createToolbar(), BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setBackground(UIComponents.LIGHT_GREY);
        body.setBorder(BorderFactory.createEmptyBorder(8, 28, 18, 28));

        JPanel metrics = new JPanel(new GridLayout(1, 5, 12, 12));
        metrics.setBackground(UIComponents.LIGHT_GREY);
        metrics.add(donorCard);
        metrics.add(availableCard);
        metrics.add(pendingCard);
        metrics.add(fulfilledCard);
        metrics.add(expiredCard);
        body.add(metrics, BorderLayout.NORTH);

        JTabbedPane recent = new JTabbedPane();
        recent.addTab("Recent donations", new JScrollPane(donationTable));
        recent.addTab("Recent requests", new JScrollPane(requestTable));

        JPanel details = new JPanel(new GridLayout(1, 2, 12, 12));
        details.setBackground(UIComponents.LIGHT_GREY);
        details.add(UIComponents.createSection(
                "Available units by blood group", new JScrollPane(inventoryTable)));
        details.add(UIComponents.createSection("Recent activity", recent));
        body.add(details, BorderLayout.CENTER);

        body.add(UIComponents.createNoteLine(
                "All figures are calculated from current database rows."), BorderLayout.SOUTH);

        add(body, BorderLayout.CENTER);
        refreshButton.addActionListener(event -> loadData());
        loadData();
    }

    /** Called when the Dashboard card becomes visible so live counts are never stale. */
    public void reload() {
        loadData();
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.setBackground(Color.WHITE);
        toolbar.setBorder(BorderFactory.createEmptyBorder(6, 22, 8, 22));
        toolbar.add(Box.createHorizontalStrut(0));
        toolbar.add(refreshButton);
        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.add(updatedLabel);
        return toolbar;
    }

    private void loadData() {
        if (busy) {
            return;
        }
        busy = true;
        refreshButton.setEnabled(false);
        updatedLabel.setText("Loading live figures...");

        new SwingWorker<ReportService.DashboardData, Void>() {
            @Override
            protected ReportService.DashboardData doInBackground() throws Exception {
                return ReportService.getDashboardData();
            }

            @Override
            protected void done() {
                try {
                    populate(get());
                } catch (ExecutionException e) {
                    updatedLabel.setText("Could not load dashboard figures.");
                    JOptionPane.showMessageDialog(DashboardPanel.this,
                            e.getCause().getMessage(), "Could not load dashboard",
                            JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    updatedLabel.setText("Load interrupted.");
                } finally {
                    busy = false;
                    refreshButton.setEnabled(true);
                }
            }
        }.execute();
    }

    private void populate(ReportService.DashboardData data) {
        donorCard.setValue(String.valueOf(data.getTotalDonors()));
        availableCard.setValue(String.valueOf(data.getAvailableUnits()));
        pendingCard.setValue(String.valueOf(data.getPendingRequests()));
        fulfilledCard.setValue(String.valueOf(data.getFulfilledRequests()));
        expiredCard.setValue(String.valueOf(data.getExpiredUnits()));

        inventoryModel.setRowCount(0);
        for (Map.Entry<String, Integer> entry : data.getInventoryByGroup().entrySet()) {
            inventoryModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
        }

        donationModel.setRowCount(0);
        for (Donation donation : data.getRecentDonations()) {
            donationModel.addRow(new Object[]{
                    donation.getDonationId(),
                    donation.getDonorName(),
                    donation.getDonationDate(),
                    donation.getScreeningStatus()
            });
        }

        requestModel.setRowCount(0);
        for (BloodRequest request : data.getRecentRequests()) {
            requestModel.addRow(new Object[]{
                    request.getRequestId(),
                    request.getPatientName(),
                    request.getRequestDate(),
                    request.getStatus()
            });
        }

        updatedLabel.setText("Updated " + LocalTime.now().format(TIME));
    }
}
