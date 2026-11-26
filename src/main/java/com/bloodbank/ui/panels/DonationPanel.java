package com.bloodbank.ui.panels;

import com.bloodbank.model.Donation;
import com.bloodbank.service.DonationService;
import com.bloodbank.ui.UIComponents;
import com.bloodbank.ui.dialogs.DonationDialog;
import com.bloodbank.ui.dialogs.DonationProcessDialog;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** Real donation history with screening actions. */
public class DonationPanel extends JPanel {

    private static final String[] COLUMNS = {
            "ID", "Donor", "Group", "Date", "Qty (ml)", "Screening", "Notes"
    };

    private final JTextField searchField = new JTextField(22);
    private final JButton searchButton = new JButton("Search");
    private final JButton clearButton = new JButton("Clear");
    private final JButton refreshButton = new JButton("Refresh");
    private final JButton addButton = UIComponents.createPrimaryButton("Record donation");
    private final JButton processButton = new JButton("Process screening");

    private final JTable table = UIComponents.createTable(COLUMNS);
    private final DefaultTableModel tableModel = (DefaultTableModel) table.getModel();

    private List<Donation> displayedDonations = new ArrayList<>();
    private boolean busy;

    public DonationPanel() {
        setLayout(new BorderLayout());
        setBackground(UIComponents.LIGHT_GREY);

        JPanel top = new JPanel(new BorderLayout());
        top.add(UIComponents.createPageHeader(
                "Donations", "Record donations and screening results"), BorderLayout.NORTH);
        top.add(createToolbar(), BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        add(createActions(), BorderLayout.SOUTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setPreferredWidth(70);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.getColumnModel().getColumn(4).setPreferredWidth(80);
        table.getColumnModel().getColumn(5).setPreferredWidth(100);
        table.getColumnModel().getColumn(6).setPreferredWidth(220);
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateActionButtons();
            }
        });
        add(new JScrollPane(table), BorderLayout.CENTER);

        searchButton.addActionListener(event -> loadDonations());
        searchField.addActionListener(event -> loadDonations());
        clearButton.addActionListener(event -> {
            searchField.setText("");
            loadDonations();
        });
        refreshButton.addActionListener(event -> loadDonations());
        addButton.addActionListener(event -> openAddDialog());
        processButton.addActionListener(event -> openProcessDialog());

        updateActionButtons();
        loadDonations();
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.setBackground(Color.WHITE);
        toolbar.setBorder(BorderFactory.createEmptyBorder(8, 22, 8, 22));
        toolbar.add(new JLabel("Search:"));
        toolbar.add(searchField);
        toolbar.add(searchButton);
        toolbar.add(clearButton);
        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.add(refreshButton);
        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.add(addButton);
        return toolbar;
    }

    private JPanel createActions() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.setBackground(UIComponents.LIGHT_GREY);
        actions.setBorder(BorderFactory.createEmptyBorder(8, 22, 10, 22));
        processButton.setToolTipText("Accept or reject a PENDING donation");
        actions.add(processButton);
        actions.add(Box.createHorizontalStrut(8));
        actions.add(UIComponents.createSubtitle(
                "ACCEPTED donations automatically create an available blood unit."));
        return actions;
    }

    private void loadDonations() {
        busy = true;
        updateActionButtons();
        searchButton.setEnabled(false);
        refreshButton.setEnabled(false);
        addButton.setEnabled(false);

        String keyword = searchField.getText();
        new SwingWorker<List<Donation>, Void>() {
            @Override
            protected List<Donation> doInBackground() throws Exception {
                return DonationService.search(keyword);
            }

            @Override
            protected void done() {
                busy = false;
                try {
                    populate(get());
                } catch (ExecutionException e) {
                    JOptionPane.showMessageDialog(DonationPanel.this,
                            e.getCause().getMessage(), "Could not load donations",
                            JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    searchButton.setEnabled(true);
                    refreshButton.setEnabled(true);
                    addButton.setEnabled(true);
                    updateActionButtons();
                }
            }
        }.execute();
    }

    private void populate(List<Donation> donations) {
        displayedDonations = donations;
        tableModel.setRowCount(0);
        for (Donation donation : donations) {
            tableModel.addRow(new Object[]{
                    donation.getDonationId(),
                    donation.getDonorName(),
                    donation.getBloodGroup(),
                    donation.getDonationDate(),
                    donation.getQuantityMl(),
                    donation.getScreeningStatus(),
                    donation.getNotes() == null ? "" : donation.getNotes()
            });
        }
    }

    private Donation selectedDonation() {
        int row = table.getSelectedRow();
        return row < 0 || row >= displayedDonations.size() ? null : displayedDonations.get(row);
    }

    private void updateActionButtons() {
        Donation donation = selectedDonation();
        boolean pending = donation != null && "PENDING".equals(donation.getScreeningStatus());
        processButton.setEnabled(!busy && pending);
    }

    private Frame ownerFrame() {
        return (Frame) SwingUtilities.getWindowAncestor(this);
    }

    private void openAddDialog() {
        DonationDialog dialog = new DonationDialog(ownerFrame());
        if (dialog.isSaved()) {
            loadDonations();
        }
    }

    private void openProcessDialog() {
        Donation donation = selectedDonation();
        if (donation == null || !"PENDING".equals(donation.getScreeningStatus())) {
            return;
        }
        DonationProcessDialog dialog = new DonationProcessDialog(ownerFrame(), donation);
        if (dialog.isChanged()) {
            loadDonations();
        }
    }
}
