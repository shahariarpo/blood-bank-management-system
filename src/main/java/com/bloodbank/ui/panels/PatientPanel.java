package com.bloodbank.ui.panels;

import com.bloodbank.model.Patient;
import com.bloodbank.service.PatientService;
import com.bloodbank.ui.UIComponents;
import com.bloodbank.ui.dialogs.PatientDialog;

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

/** Real patient management screen. */
public class PatientPanel extends JPanel {

    private final JTextField searchField = new JTextField(22);
    private final JButton searchButton = new JButton("Search");
    private final JButton clearButton = new JButton("Clear");
    private final JButton refreshButton = new JButton("Refresh");
    private final JButton addButton = UIComponents.createPrimaryButton("Add patient");
    private final JButton editButton = new JButton("Edit");
    private final JButton deleteButton = new JButton("Delete");

    private final JTable table = UIComponents.createTable(new String[]{
            "ID", "Name", "Age", "Gender", "Group", "Phone", "Hospital"
    });
    private final DefaultTableModel tableModel = (DefaultTableModel) table.getModel();

    private List<Patient> displayedPatients = new ArrayList<>();
    private boolean busy;

    public PatientPanel() {
        setLayout(new BorderLayout());
        setBackground(UIComponents.LIGHT_GREY);

        JPanel top = new JPanel(new BorderLayout());
        top.add(UIComponents.createPageHeader(
                "Patients", "Register and manage patients needing blood"), BorderLayout.NORTH);
        top.add(createToolbar(), BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        add(createActions(), BorderLayout.SOUTH);

        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(160);
        table.getColumnModel().getColumn(5).setPreferredWidth(120);
        table.getColumnModel().getColumn(6).setPreferredWidth(160);
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateActionButtons();
            }
        });
        add(new JScrollPane(table), BorderLayout.CENTER);

        searchButton.addActionListener(event -> loadPatients());
        searchField.addActionListener(event -> loadPatients());
        clearButton.addActionListener(event -> {
            searchField.setText("");
            loadPatients();
        });
        refreshButton.addActionListener(event -> loadPatients());
        addButton.addActionListener(event -> openAddDialog());
        editButton.addActionListener(event -> openEditDialog());
        deleteButton.addActionListener(event -> deleteSelectedPatient());

        updateActionButtons();
        loadPatients();
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
        actions.add(deleteButton);
        actions.add(Box.createHorizontalStrut(8));
        actions.add(UIComponents.createSubtitle(
                "A patient with requests cannot be deleted while those records exist."));
        return actions;
    }

    private void loadPatients() {
        busy = true;
        updateActionButtons();
        searchButton.setEnabled(false);
        refreshButton.setEnabled(false);
        addButton.setEnabled(false);

        String keyword = searchField.getText();
        new SwingWorker<List<Patient>, Void>() {
            @Override
            protected List<Patient> doInBackground() throws Exception {
                return PatientService.search(keyword);
            }

            @Override
            protected void done() {
                busy = false;
                try {
                    populate(get());
                } catch (ExecutionException e) {
                    JOptionPane.showMessageDialog(PatientPanel.this,
                            e.getCause().getMessage(), "Could not load patients",
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

    private void populate(List<Patient> patients) {
        displayedPatients = patients;
        tableModel.setRowCount(0);
        for (Patient patient : patients) {
            tableModel.addRow(new Object[]{
                    patient.getPatientId(),
                    patient.getName(),
                    patient.getAge(),
                    patient.getGender(),
                    patient.getBloodGroup(),
                    patient.getPhone(),
                    patient.getHospital() == null ? "" : patient.getHospital()
            });
        }
    }

    private Patient selectedPatient() {
        int row = table.getSelectedRow();
        return row < 0 || row >= displayedPatients.size() ? null : displayedPatients.get(row);
    }

    private void updateActionButtons() {
        boolean enabled = !busy && selectedPatient() != null;
        editButton.setEnabled(enabled);
        deleteButton.setEnabled(enabled);
    }

    private Frame ownerFrame() {
        return (Frame) SwingUtilities.getWindowAncestor(this);
    }

    private void openAddDialog() {
        PatientDialog dialog = new PatientDialog(ownerFrame(), null);
        if (dialog.isSaved()) {
            loadPatients();
        }
    }

    private void openEditDialog() {
        Patient patient = selectedPatient();
        if (patient != null && new PatientDialog(ownerFrame(), patient).isSaved()) {
            loadPatients();
        }
    }

    private void deleteSelectedPatient() {
        Patient patient = selectedPatient();
        if (patient == null) {
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this,
                "Delete patient " + patient.getName() + "? This cannot be undone.",
                "Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        busy = true;
        updateActionButtons();
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                PatientService.delete(patient.getPatientId());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    loadPatients();
                } catch (ExecutionException e) {
                    busy = false;
                    updateActionButtons();
                    JOptionPane.showMessageDialog(PatientPanel.this,
                            e.getCause().getMessage(), "Could not delete patient",
                            JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException e) {
                    busy = false;
                    updateActionButtons();
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }
}
