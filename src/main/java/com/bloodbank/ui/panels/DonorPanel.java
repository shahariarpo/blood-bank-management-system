package com.bloodbank.ui.panels;

import com.bloodbank.model.Donor;
import com.bloodbank.service.DonorService;
import com.bloodbank.ui.UIComponents;
import com.bloodbank.ui.dialogs.DonorDialog;
import com.bloodbank.ui.dialogs.DonorProfileDialog;

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

/** Real donor management screen with search, CRUD actions and profiles. */
public class DonorPanel extends JPanel {

    private final JTextField searchField = new JTextField(22);
    private final JButton searchButton = new JButton("Search");
    private final JButton clearButton = new JButton("Clear");
    private final JButton refreshButton = new JButton("Refresh");
    private final JButton addButton = UIComponents.createPrimaryButton("Add donor");
    private final JButton editButton = new JButton("Edit");
    private final JButton profileButton = new JButton("Profile");
    private final JButton deleteButton = new JButton("Delete");

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Name", "Age", "Gender", "Blood group",
                    "Phone", "Address", "Registered"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    private List<Donor> displayedDonors = new ArrayList<>();
    private boolean busy;

    public DonorPanel() {
        setLayout(new BorderLayout());
        setBackground(UIComponents.LIGHT_GREY);
        JPanel top = new JPanel(new BorderLayout());
        top.add(UIComponents.createPageHeader(
                "Donors", "Register, search and manage blood donors"), BorderLayout.NORTH);
        top.add(createToolbar(), BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        add(createActions(), BorderLayout.SOUTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(26);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(5).setPreferredWidth(110);
        table.setFillsViewportHeight(true);
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateActionButtons();
            }
        });
        add(new JScrollPane(table), BorderLayout.CENTER);

        searchButton.addActionListener(event -> loadDonors());
        searchField.addActionListener(event -> loadDonors());
        clearButton.addActionListener(event -> {
            searchField.setText("");
            loadDonors();
        });
        refreshButton.addActionListener(event -> loadDonors());
        addButton.addActionListener(event -> openAddDialog());
        editButton.addActionListener(event -> openEditDialog());
        profileButton.addActionListener(event -> openProfileDialog());
        deleteButton.addActionListener(event -> deleteSelectedDonor());

        updateActionButtons();
        loadDonors();
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
        actions.add(editButton);
        actions.add(profileButton);
        actions.add(deleteButton);
        return actions;
    }

    private void loadDonors() {
        busy = true;
        updateActionButtons();
        searchButton.setEnabled(false);
        refreshButton.setEnabled(false);
        addButton.setEnabled(false);

        String keyword = searchField.getText();
        new SwingWorker<List<Donor>, Void>() {
            @Override
            protected List<Donor> doInBackground() throws Exception {
                return DonorService.search(keyword);
            }

            @Override
            protected void done() {
                busy = false;
                try {
                    populate(get());
                } catch (ExecutionException e) {
                    JOptionPane.showMessageDialog(DonorPanel.this,
                            e.getCause().getMessage(), "Could not load donors",
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

    private void populate(List<Donor> donors) {
        displayedDonors = donors;
        tableModel.setRowCount(0);
        for (Donor donor : donors) {
            tableModel.addRow(new Object[]{
                    donor.getDonorId(),
                    donor.getName(),
                    donor.getAge(),
                    donor.getGender(),
                    donor.getBloodGroup(),
                    donor.getPhone(),
                    donor.getAddress() == null ? "" : donor.getAddress(),
                    donor.getCreatedAt() == null ? "" : donor.getCreatedAt()
            });
        }
    }

    private Donor selectedDonor() {
        int row = table.getSelectedRow();
        return row < 0 || row >= displayedDonors.size() ? null : displayedDonors.get(row);
    }

    private void updateActionButtons() {
        boolean enabled = !busy && selectedDonor() != null;
        editButton.setEnabled(enabled);
        profileButton.setEnabled(enabled);
        deleteButton.setEnabled(enabled);
    }

    private Frame ownerFrame() {
        return (Frame) SwingUtilities.getWindowAncestor(this);
    }

    private void openAddDialog() {
        DonorDialog dialog = new DonorDialog(ownerFrame(), null);
        if (dialog.isSaved()) {
            loadDonors();
        }
    }

    private void openEditDialog() {
        Donor donor = selectedDonor();
        if (donor == null) {
            return;
        }
        DonorDialog dialog = new DonorDialog(ownerFrame(), donor);
        if (dialog.isSaved()) {
            loadDonors();
        }
    }

    private void openProfileDialog() {
        Donor donor = selectedDonor();
        if (donor != null) {
            new DonorProfileDialog(ownerFrame(), donor);
        }
    }

    private void deleteSelectedDonor() {
        Donor donor = selectedDonor();
        if (donor == null) {
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this,
                "Delete donor " + donor.getName() + "? This cannot be undone.",
                "Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        busy = true;
        updateActionButtons();
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                DonorService.delete(donor.getDonorId());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    loadDonors();
                } catch (ExecutionException e) {
                    busy = false;
                    updateActionButtons();
                    JOptionPane.showMessageDialog(DonorPanel.this,
                            e.getCause().getMessage(), "Could not delete donor",
                            JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException e) {
                    busy = false;
                    Thread.currentThread().interrupt();
                    updateActionButtons();
                }
            }
        }.execute();
    }
}
