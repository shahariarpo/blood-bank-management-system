package com.bloodbank.ui.panels;

import com.bloodbank.model.BloodIssue;
import com.bloodbank.model.BloodRequest;
import com.bloodbank.model.BloodReservation;
import com.bloodbank.model.Donation;
import com.bloodbank.service.BackupService;
import com.bloodbank.service.ReportService;
import com.bloodbank.ui.UIComponents;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/** Real reports screen with live figures, histories and SQLite backup. */
public class ReportsPanel extends JPanel {

    private final UIComponents.StatCard donorCard =
            UIComponents.createStatCard("Total donors", "…", UIComponents.DARK);
    private final UIComponents.StatCard patientCard =
            UIComponents.createStatCard("Total patients", "…", UIComponents.DARK);
    private final UIComponents.StatCard donationCard =
            UIComponents.createStatCard("Total donations", "…", UIComponents.RED);
    private final UIComponents.StatCard acceptedCard =
            UIComponents.createStatCard("Accepted donations", "…", UIComponents.DARK);
    private final UIComponents.StatCard issuedCard =
            UIComponents.createStatCard("Issued units", "…", UIComponents.RED);
    private final UIComponents.StatCard expiredCard =
            UIComponents.createStatCard("Expired units", "…", UIComponents.RED);
    private final UIComponents.StatCard reservedCard =
            UIComponents.createStatCard("Reserved units", "…", UIComponents.RED);
    private final UIComponents.StatCard pendingCard =
            UIComponents.createStatCard("Pending requests", "…", UIComponents.DARK);
    private final UIComponents.StatCard fulfilledCard =
            UIComponents.createStatCard("Fulfilled requests", "…", UIComponents.DARK);

    private final JTable inventoryTable = UIComponents.createTable(
            new String[]{"Blood group", "Available units"});
    private final DefaultTableModel inventoryModel = (DefaultTableModel) inventoryTable.getModel();
    private final JTable unitStatusTable = UIComponents.createTable(
            new String[]{"Unit status", "Units"});
    private final DefaultTableModel unitStatusModel = (DefaultTableModel) unitStatusTable.getModel();
    private final JTable requestStatusTable = UIComponents.createTable(
            new String[]{"Request status", "Requests"});
    private final DefaultTableModel requestStatusModel =
            (DefaultTableModel) requestStatusTable.getModel();
    private final JTable donationTable = UIComponents.createTable(
            new String[]{"ID", "Donor", "Date", "Qty (ml)", "Status"});
    private final DefaultTableModel donationModel = (DefaultTableModel) donationTable.getModel();
    private final JTable requestTable = UIComponents.createTable(
            new String[]{"ID", "Patient", "Group", "Qty", "Date", "Priority", "Status"});
    private final DefaultTableModel requestModel = (DefaultTableModel) requestTable.getModel();
    private final JTable issueTable = UIComponents.createTable(
            new String[]{"Issue", "Request", "Patient", "Unit", "Group", "Component", "Date"});
    private final DefaultTableModel issueModel = (DefaultTableModel) issueTable.getModel();
    private final JTable reservationTable = UIComponents.createTable(
            new String[]{"Request", "Patient", "Unit", "Group", "Component", "Expires", "Reserved"});
    private final DefaultTableModel reservationModel =
            (DefaultTableModel) reservationTable.getModel();

    private final JButton refreshButton = new JButton("Refresh");
    private final JButton backupButton = UIComponents.createPrimaryButton("Backup database");
    private final JLabel statusLabel = UIComponents.createSubtitle("Loading report...");
    private boolean busy;

    public ReportsPanel() {
        setLayout(new BorderLayout());
        setBackground(UIComponents.LIGHT_GREY);

        JPanel top = new JPanel(new BorderLayout());
        top.add(UIComponents.createPageHeader(
                "Reports", "Donation, inventory and issuing summaries"), BorderLayout.NORTH);
        top.add(createToolbar(), BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setBackground(UIComponents.LIGHT_GREY);
        body.setBorder(BorderFactory.createEmptyBorder(8, 28, 18, 28));

        JPanel metrics = new JPanel(new GridLayout(3, 3, 12, 12));
        metrics.setBackground(UIComponents.LIGHT_GREY);
        metrics.add(donorCard);
        metrics.add(patientCard);
        metrics.add(donationCard);
        metrics.add(acceptedCard);
        metrics.add(issuedCard);
        metrics.add(expiredCard);
        metrics.add(reservedCard);
        metrics.add(pendingCard);
        metrics.add(fulfilledCard);
        body.add(metrics, BorderLayout.NORTH);
        body.add(createHistoryTabs(), BorderLayout.CENTER);

        add(body, BorderLayout.CENTER);
        refreshButton.addActionListener(event -> loadData());
        backupButton.addActionListener(event -> openBackupDialog());
        loadData();
    }

    /** Called when the Reports card becomes visible so live figures are never stale. */
    public void reload() {
        loadData();
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.setBackground(Color.WHITE);
        toolbar.setBorder(BorderFactory.createEmptyBorder(6, 22, 8, 22));
        toolbar.add(refreshButton);
        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.add(backupButton);
        toolbar.add(Box.createHorizontalStrut(12));
        toolbar.add(statusLabel);
        return toolbar;
    }

    private JTabbedPane createHistoryTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Inventory by group", new JScrollPane(inventoryTable));
        tabs.addTab("Unit statuses", new JScrollPane(unitStatusTable));
        tabs.addTab("Request statuses", new JScrollPane(requestStatusTable));
        tabs.addTab("Donation history", new JScrollPane(donationTable));
        tabs.addTab("Request history", new JScrollPane(requestTable));
        tabs.addTab("Issuing history", new JScrollPane(issueTable));
        tabs.addTab("Reservations", new JScrollPane(reservationTable));
        return tabs;
    }

    private void loadData() {
        if (busy) {
            return;
        }
        busy = true;
        refreshButton.setEnabled(false);
        statusLabel.setText("Loading report...");

        new SwingWorker<ReportService.ReportData, Void>() {
            @Override
            protected ReportService.ReportData doInBackground() throws Exception {
                return ReportService.getReportData();
            }

            @Override
            protected void done() {
                try {
                    populate(get());
                    statusLabel.setText("Report loaded from SQLite.");
                } catch (ExecutionException e) {
                    statusLabel.setText("Could not load report.");
                    JOptionPane.showMessageDialog(ReportsPanel.this,
                            e.getCause().getMessage(), "Could not load report",
                            JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText("Load interrupted.");
                } finally {
                    busy = false;
                    refreshButton.setEnabled(true);
                }
            }
        }.execute();
    }

    private void populate(ReportService.ReportData data) {
        donorCard.setValue(String.valueOf(data.getTotalDonors()));
        patientCard.setValue(String.valueOf(data.getTotalPatients()));
        donationCard.setValue(String.valueOf(data.getTotalDonations()));
        acceptedCard.setValue(String.valueOf(data.getAcceptedDonations()));
        issuedCard.setValue(String.valueOf(data.getIssuedUnits()));
        expiredCard.setValue(String.valueOf(data.getExpiredUnits()));
        reservedCard.setValue(String.valueOf(data.getReservedUnits()));
        pendingCard.setValue(String.valueOf(data.getPendingRequests()));
        fulfilledCard.setValue(String.valueOf(data.getFulfilledRequests()));

        fillCounts(inventoryModel, data.getInventoryByGroup());
        fillCounts(unitStatusModel, data.getUnitStatuses());
        fillCounts(requestStatusModel, data.getRequestStatuses());

        donationModel.setRowCount(0);
        for (Donation donation : data.getDonationHistory()) {
            donationModel.addRow(new Object[]{
                    donation.getDonationId(),
                    donation.getDonorName(),
                    donation.getDonationDate(),
                    donation.getQuantityMl(),
                    donation.getScreeningStatus()
            });
        }

        requestModel.setRowCount(0);
        for (BloodRequest request : data.getRequestHistory()) {
            requestModel.addRow(new Object[]{
                    request.getRequestId(),
                    request.getPatientName(),
                    request.getBloodGroup(),
                    request.getQuantity(),
                    request.getRequestDate(),
                    request.getPriority(),
                    request.getStatus()
            });
        }

        issueModel.setRowCount(0);
        for (BloodIssue issue : data.getIssuingHistory()) {
            issueModel.addRow(new Object[]{
                    issue.getIssueId(),
                    issue.getRequestId(),
                    issue.getPatientName(),
                    issue.getUnitId(),
                    issue.getBloodGroup(),
                    issue.getComponentType(),
                    issue.getIssueDate()
            });
        }

        reservationModel.setRowCount(0);
        for (BloodReservation reservation : data.getReservations()) {
            reservationModel.addRow(new Object[]{
                    reservation.getRequestId(),
                    reservation.getPatientName(),
                    reservation.getUnitId(),
                    reservation.getBloodGroup(),
                    reservation.getComponentType(),
                    reservation.getExpiryDate(),
                    reservation.getReservedOn()
            });
        }
    }

    private void fillCounts(DefaultTableModel model, Map<String, Integer> counts) {
        model.setRowCount(0);
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            model.addRow(new Object[]{entry.getKey(), entry.getValue()});
        }
    }

    private void openBackupDialog() {
        if (busy) {
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save database backup");
        chooser.setFileFilter(new FileNameExtensionFilter("SQLite database (*.db)", "db"));
        chooser.setSelectedFile(new java.io.File(
                BackupService.suggestedFileName()));
        int answer = chooser.showSaveDialog(this);
        if (answer != JFileChooser.APPROVE_OPTION) {
            return;
        }

        java.io.File selected = chooser.getSelectedFile();
        String name = selected.getName();
        if (!name.toLowerCase().endsWith(".db")) {
            selected = new java.io.File(selected.getParentFile(), name + ".db");
        }
        createBackup(selected.toPath());
    }

    private void createBackup(Path destination) {
        busy = true;
        refreshButton.setEnabled(false);
        backupButton.setEnabled(false);
        statusLabel.setText("Creating backup...");

        new SwingWorker<Path, Void>() {
            @Override
            protected Path doInBackground() throws Exception {
                Path backup = BackupService.createBackup(destination);
                BackupService.verifyBackup(backup);
                return backup;
            }

            @Override
            protected void done() {
                try {
                    Path backup = get();
                    statusLabel.setText("Backup saved: " + backup.getFileName());
                    JOptionPane.showMessageDialog(ReportsPanel.this,
                            "Backup created successfully:\n" + backup,
                            "Database backup", JOptionPane.INFORMATION_MESSAGE);
                } catch (ExecutionException e) {
                    statusLabel.setText("Backup failed.");
                    JOptionPane.showMessageDialog(ReportsPanel.this,
                            e.getCause().getMessage(), "Could not create backup",
                            JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText("Backup interrupted.");
                } finally {
                    busy = false;
                    refreshButton.setEnabled(true);
                    backupButton.setEnabled(true);
                }
            }
        }.execute();
    }
}
