package com.bloodbank.ui.panels;

import com.bloodbank.model.BloodComponent;
import com.bloodbank.model.BloodUnit;
import com.bloodbank.service.InventoryService;
import com.bloodbank.ui.UIComponents;
import com.bloodbank.util.Validators;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/** Real inventory screen with filters, stock counts, expiry monitoring and quarantine. */
public class InventoryPanel extends JPanel {

    private static final String[] UNIT_COLUMNS = {
            "ID", "Donation", "Group", "Component", "Collected", "Expires", "Status",
            "Reserved for", "Donor"
    };
    private static final String[] SUMMARY_COLUMNS = {"Blood group", "Available units"};
    private static final int EXPIRY_WINDOW_DAYS = 7;

    private final JTextField searchField = new JTextField(18);
    private final JComboBox<String> groupFilter = new JComboBox<>();
    private final JComboBox<String> statusFilter = new JComboBox<>();
    private final JButton searchButton = new JButton("Search");
    private final JButton clearButton = new JButton("Clear");
    private final JButton refreshButton = new JButton("Refresh");
    private final JButton quarantineButton = new JButton("Quarantine");
    private final JButton releaseButton = new JButton("Release");

    private final JTable table = UIComponents.createTable(UNIT_COLUMNS);
    private final DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
    private final JTable summaryTable = UIComponents.createTable(SUMMARY_COLUMNS);
    private final DefaultTableModel summaryModel = (DefaultTableModel) summaryTable.getModel();
    private final JLabel expiringLabel = UIComponents.createSubtitle("Loading expiry monitor...");

    private List<BloodUnit> displayedUnits = new ArrayList<>();
    private Map<Integer, Integer> displayedReservations = Map.of();
    private boolean busy;

    public InventoryPanel() {
        setLayout(new BorderLayout());
        setBackground(UIComponents.LIGHT_GREY);

        groupFilter.addItem("ALL");
        Validators.BLOOD_GROUPS.forEach(groupFilter::addItem);
        statusFilter.addItem("ALL");
        InventoryService.STATUSES.forEach(statusFilter::addItem);

        JPanel top = new JPanel(new BorderLayout());
        top.add(UIComponents.createPageHeader(
                "Inventory", "Track individual blood units and expiry dates"), BorderLayout.NORTH);
        top.add(createToolbar(), BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        add(createActions(), BorderLayout.SOUTH);

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setBackground(UIComponents.LIGHT_GREY);
        center.setBorder(BorderFactory.createEmptyBorder(8, 28, 24, 28));

        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(70);
        table.getColumnModel().getColumn(2).setPreferredWidth(70);
        table.getColumnModel().getColumn(3).setPreferredWidth(110);
        table.getColumnModel().getColumn(4).setPreferredWidth(100);
        table.getColumnModel().getColumn(5).setPreferredWidth(100);
        table.getColumnModel().getColumn(6).setPreferredWidth(110);
        table.getColumnModel().getColumn(7).setPreferredWidth(100);
        table.getColumnModel().getColumn(8).setPreferredWidth(150);
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateActionButtons();
            }
        });
        center.add(UIComponents.createSection("Blood units", new JScrollPane(table)),
                BorderLayout.CENTER);

        summaryTable.setRowHeight(24);
        JScrollPane summaryScroll = new JScrollPane(summaryTable);
        summaryScroll.setPreferredSize(new Dimension(260, 190));

        JPanel bottom = new JPanel(new GridLayout(1, 2, 12, 12));
        bottom.setBackground(UIComponents.LIGHT_GREY);
        bottom.add(UIComponents.createSection(
                "Available units by blood group", summaryScroll));
        bottom.add(createHealthPanel());
        center.add(bottom, BorderLayout.SOUTH);

        add(center, BorderLayout.CENTER);

        searchButton.addActionListener(event -> loadUnits());
        searchField.addActionListener(event -> loadUnits());
        clearButton.addActionListener(event -> {
            searchField.setText("");
            groupFilter.setSelectedItem("ALL");
            statusFilter.setSelectedItem("ALL");
            loadUnits();
        });
        refreshButton.addActionListener(event -> loadUnits());
        groupFilter.addActionListener(event -> loadUnits());
        statusFilter.addActionListener(event -> loadUnits());
        quarantineButton.addActionListener(event -> changeStatus("QUARANTINED"));
        releaseButton.addActionListener(event -> changeStatus("AVAILABLE"));

        updateActionButtons();
        loadUnits();
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.setBackground(Color.WHITE);
        toolbar.setBorder(BorderFactory.createEmptyBorder(8, 22, 8, 22));
        toolbar.add(new JLabel("Search:"));
        toolbar.add(searchField);
        toolbar.add(new JLabel("Group:"));
        toolbar.add(groupFilter);
        toolbar.add(new JLabel("Status:"));
        toolbar.add(statusFilter);
        toolbar.add(searchButton);
        toolbar.add(clearButton);
        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.add(refreshButton);
        return toolbar;
    }

    private JPanel createActions() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.setBackground(UIComponents.LIGHT_GREY);
        actions.setBorder(BorderFactory.createEmptyBorder(8, 22, 10, 22));
        quarantineButton.setToolTipText("Temporarily remove this unit from availability");
        releaseButton.setToolTipText("Return a quarantined unit to availability");
        actions.add(quarantineButton);
        actions.add(releaseButton);
        actions.add(Box.createHorizontalStrut(8));
        actions.add(UIComponents.createSubtitle(
                "RESERVED units are held by approvals. ISSUED/EXPIRED cannot be changed here."));
        return actions;
    }

    private JPanel createHealthPanel() {
        JPanel panel = UIComponents.createPanel(Color.WHITE);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIComponents.BORDER),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));

        JLabel title = UIComponents.createSectionTitle("Expiry monitor");
        title.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        expiringLabel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        JLabel note = UIComponents.createSubtitle(
                "Expired units are flagged automatically on load.");
        note.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        JLabel fefo = UIComponents.createSubtitle(
                "Approval reserves FEFO units; issuing consumes those holds first.");
        fefo.setAlignmentX(JComponent.LEFT_ALIGNMENT);

        panel.add(title);
        panel.add(Box.createVerticalStrut(8));
        panel.add(expiringLabel);
        panel.add(Box.createVerticalStrut(4));
        panel.add(note);
        panel.add(Box.createVerticalStrut(4));
        panel.add(fefo);
        return panel;
    }

    private void loadUnits() {
        busy = true;
        updateActionButtons();
        setControlsEnabled(false);

        String keyword = searchField.getText();
        String group = (String) groupFilter.getSelectedItem();
        String status = (String) statusFilter.getSelectedItem();

        new SwingWorker<InventorySnapshot, Void>() {
            @Override
            protected InventorySnapshot doInBackground() throws Exception {
                return new InventorySnapshot(
                        InventoryService.search(keyword, group, status),
                        InventoryService.getAvailableCountsByGroup(),
                        InventoryService.findExpiringSoon(EXPIRY_WINDOW_DAYS),
                        InventoryService.getReservedRequestIds());
            }

            @Override
            protected void done() {
                busy = false;
                try {
                    populate(get());
                } catch (ExecutionException e) {
                    JOptionPane.showMessageDialog(InventoryPanel.this,
                            e.getCause().getMessage(), "Could not load inventory",
                            JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    setControlsEnabled(true);
                    updateActionButtons();
                }
            }
        }.execute();
    }

    private void setControlsEnabled(boolean enabled) {
        searchButton.setEnabled(enabled);
        clearButton.setEnabled(enabled);
        refreshButton.setEnabled(enabled);
        groupFilter.setEnabled(enabled);
        statusFilter.setEnabled(enabled);
    }

    private void populate(InventorySnapshot snapshot) {
        displayedUnits = snapshot.units;
        displayedReservations = snapshot.reservations;
        tableModel.setRowCount(0);
        for (BloodUnit unit : snapshot.units) {
            Integer requestId = snapshot.reservations.get(unit.getUnitId());
            tableModel.addRow(new Object[]{
                    unit.getUnitId(),
                    unit.getDonationId(),
                    unit.getBloodGroup(),
                    componentLabel(unit.getComponentType()),
                    unit.getCollectionDate(),
                    unit.getExpiryDate(),
                    requestId == null ? unit.getStatus() : "RESERVED",
                    requestId == null ? "-" : "#" + requestId,
                    unit.getDonorName() == null ? "-" : unit.getDonorName()
            });
        }

        summaryModel.setRowCount(0);
        for (String group : Validators.BLOOD_GROUPS) {
            summaryModel.addRow(new Object[]{
                    group, snapshot.counts.getOrDefault(group, 0)
            });
        }

        expiringLabel.setText(snapshot.expiring.size() + " available unit(s) expire within "
                + EXPIRY_WINDOW_DAYS + " days.");
        expiringLabel.setForeground(snapshot.expiring.isEmpty()
                ? UIComponents.MUTED : UIComponents.RED);
    }

    private String componentLabel(String raw) {
        if (raw == null) {
            return "-";
        }
        try {
            return BloodComponent.valueOf(raw).getDisplayName();
        } catch (IllegalArgumentException e) {
            return raw;
        }
    }

    private BloodUnit selectedUnit() {
        int row = table.getSelectedRow();
        return row < 0 || row >= displayedUnits.size() ? null : displayedUnits.get(row);
    }

    private void updateActionButtons() {
        BloodUnit unit = selectedUnit();
        boolean reserved = unit != null && displayedReservations.containsKey(unit.getUnitId());
        boolean usable = !busy && unit != null && !reserved && !InventoryService.isExpired(unit);
        quarantineButton.setEnabled(usable && "AVAILABLE".equals(unit.getStatus()));
        releaseButton.setEnabled(usable && "QUARANTINED".equals(unit.getStatus()));
    }

    private void changeStatus(String target) {
        BloodUnit unit = selectedUnit();
        if (unit == null) {
            return;
        }

        busy = true;
        updateActionButtons();
        setControlsEnabled(false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                InventoryService.setStatus(unit.getUnitId(), target);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    loadUnits();
                } catch (ExecutionException e) {
                    busy = false;
                    setControlsEnabled(true);
                    updateActionButtons();
                    JOptionPane.showMessageDialog(InventoryPanel.this,
                            e.getCause().getMessage(), "Could not update unit",
                            JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException e) {
                    busy = false;
                    setControlsEnabled(true);
                    Thread.currentThread().interrupt();
                    updateActionButtons();
                }
            }
        }.execute();
    }

    private static final class InventorySnapshot {
        private final List<BloodUnit> units;
        private final Map<String, Integer> counts;
        private final List<BloodUnit> expiring;
        private final Map<Integer, Integer> reservations;

        private InventorySnapshot(List<BloodUnit> units, Map<String, Integer> counts,
                                  List<BloodUnit> expiring, Map<Integer, Integer> reservations) {
            this.units = units;
            this.counts = counts;
            this.expiring = expiring;
            this.reservations = reservations;
        }
    }
}
