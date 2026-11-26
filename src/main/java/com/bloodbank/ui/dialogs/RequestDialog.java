package com.bloodbank.ui.dialogs;

import com.bloodbank.model.BloodRequest;
import com.bloodbank.model.Patient;
import com.bloodbank.service.PatientService;
import com.bloodbank.service.RequestService;
import com.bloodbank.ui.UIComponents;
import com.bloodbank.util.Validators;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/** Form for creating a blood request. */
public class RequestDialog extends JDialog {

    private final JComboBox<String> patientCombo = new JComboBox<>();
    private final Map<String, Patient> patientsByLabel = new LinkedHashMap<>();
    private final JComboBox<String> bloodGroupCombo =
            new JComboBox<>(Validators.BLOOD_GROUPS.toArray(new String[0]));
    private final JTextField quantityField = new JTextField("1", 8);
    private final JTextField dateField = new JTextField(LocalDate.now().toString(), 12);
    private final JComboBox<String> priorityCombo =
            new JComboBox<>(RequestService.PRIORITIES.toArray(new String[0]));
    private final JButton saveButton = UIComponents.createPrimaryButton("Save request");

    private boolean saved;

    public RequestDialog(Frame owner) {
        super(owner, "New blood request", true);
        buildUi();
        pack();
        setLocationRelativeTo(owner);
        loadPatients();
        setVisible(true);
    }

    private void buildUi() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 12, 24));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.LINE_END;

        addField(form, c, 0, "Patient:", patientCombo);
        addField(form, c, 1, "Requested group:", bloodGroupCombo);
        addField(form, c, 2, "Quantity (units):", quantityField);
        addField(form, c, 3, "Date (yyyy-MM-dd):", dateField);
        addField(form, c, 4, "Priority:", priorityCombo);
        add(form, BorderLayout.CENTER);

        patientCombo.addActionListener(event -> {
            Patient patient = patientsByLabel.get(patientCombo.getSelectedItem());
            if (patient != null) {
                bloodGroupCombo.setSelectedItem(patient.getBloodGroup());
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(event -> dispose());
        saveButton.addActionListener(event -> save());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setBorder(BorderFactory.createEmptyBorder(8, 24, 20, 24));
        buttons.add(cancelButton);
        buttons.add(saveButton);
        add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(saveButton);
    }

    private void addField(JPanel panel, GridBagConstraints c, int row,
                          String label, Component field) {
        c.gridx = 0;
        c.gridy = row;
        panel.add(new JLabel(label), c);
        c.gridx = 1;
        c.anchor = GridBagConstraints.LINE_START;
        panel.add(field, c);
        c.anchor = GridBagConstraints.LINE_END;
    }

    private void loadPatients() {
        new SwingWorker<List<Patient>, Void>() {
            @Override
            protected List<Patient> doInBackground() throws Exception {
                return PatientService.getAll();
            }

            @Override
            protected void done() {
                try {
                    for (Patient patient : get()) {
                        String label = patient.getPatientId() + " - " + patient.getName()
                                + " (" + patient.getBloodGroup() + ")";
                        patientsByLabel.put(label, patient);
                        patientCombo.addItem(label);
                    }
                    if (patientCombo.getItemCount() == 0) {
                        JOptionPane.showMessageDialog(RequestDialog.this,
                                "Register a patient before creating a request.",
                                "No patients", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (ExecutionException e) {
                    JOptionPane.showMessageDialog(RequestDialog.this,
                            e.getCause().getMessage(), "Could not load patients",
                            JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private BloodRequest collectRequest() {
        Patient patient = patientsByLabel.get(patientCombo.getSelectedItem());
        if (patient == null) {
            JOptionPane.showMessageDialog(this, "Select a patient.",
                    "Validation error", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityField.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Quantity must be a whole number.",
                    "Validation error", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateField.getText().trim());
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Date must be in yyyy-MM-dd format.",
                    "Validation error", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        return new BloodRequest(patient.getPatientId(),
                (String) bloodGroupCombo.getSelectedItem(), quantity, date,
                (String) priorityCombo.getSelectedItem());
    }

    private void save() {
        BloodRequest request = collectRequest();
        if (request == null) {
            return;
        }

        saveButton.setEnabled(false);
        saveButton.setText("Saving...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                RequestService.add(request);
                return null;
            }

            @Override
            protected void done() {
                saveButton.setEnabled(true);
                saveButton.setText("Save request");
                try {
                    get();
                    saved = true;
                    dispose();
                } catch (ExecutionException e) {
                    JOptionPane.showMessageDialog(RequestDialog.this,
                            e.getCause().getMessage(), "Could not save request",
                            JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    public boolean isSaved() {
        return saved;
    }
}
