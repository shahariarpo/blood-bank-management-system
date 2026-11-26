package com.bloodbank.ui.dialogs;

import com.bloodbank.model.Patient;
import com.bloodbank.service.PatientService;
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
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.concurrent.ExecutionException;

/** Modal form for adding or editing a patient. */
public class PatientDialog extends JDialog {

    private final JTextField nameField = new JTextField(22);
    private final JTextField ageField = new JTextField(6);
    private final JComboBox<String> genderCombo =
            new JComboBox<>(new String[]{"Male", "Female", "Other"});
    private final JComboBox<String> bloodGroupCombo =
            new JComboBox<>(Validators.BLOOD_GROUPS.toArray(new String[0]));
    private final JTextField phoneField = new JTextField(22);
    private final JTextField hospitalField = new JTextField(22);
    private final JButton saveButton = UIComponents.createPrimaryButton("Save");

    private final Patient existing;
    private boolean saved;

    public PatientDialog(Frame owner, Patient patient) {
        super(owner, patient == null ? "Register patient" : "Edit patient", true);
        this.existing = patient;

        buildUi();
        if (patient != null) {
            populate(patient);
        }
        pack();
        setLocationRelativeTo(owner);
        setVisible(true);
    }

    private void buildUi() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 12, 24));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.LINE_END;

        addField(form, c, 0, "Name:", nameField);
        addField(form, c, 1, "Age:", ageField);
        addField(form, c, 2, "Gender:", genderCombo);
        addField(form, c, 3, "Blood group:", bloodGroupCombo);
        addField(form, c, 4, "Phone:", phoneField);
        addField(form, c, 5, "Hospital:", hospitalField);
        add(form, BorderLayout.CENTER);

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
                          String label, java.awt.Component field) {
        c.gridx = 0;
        c.gridy = row;
        panel.add(new JLabel(label), c);
        c.gridx = 1;
        c.anchor = GridBagConstraints.LINE_START;
        panel.add(field, c);
        c.anchor = GridBagConstraints.LINE_END;
    }

    private void populate(Patient patient) {
        nameField.setText(patient.getName());
        ageField.setText(String.valueOf(patient.getAge()));
        genderCombo.setSelectedItem(patient.getGender());
        bloodGroupCombo.setSelectedItem(patient.getBloodGroup());
        phoneField.setText(patient.getPhone());
        hospitalField.setText(patient.getHospital() == null ? "" : patient.getHospital());
    }

    private Patient collectPatient() {
        int age;
        try {
            age = Integer.parseInt(ageField.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Age must be a whole number.",
                    "Validation error", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return new Patient(
                nameField.getText(),
                age,
                (String) genderCombo.getSelectedItem(),
                (String) bloodGroupCombo.getSelectedItem(),
                phoneField.getText(),
                hospitalField.getText());
    }

    private void save() {
        Patient patient = collectPatient();
        if (patient == null) {
            return;
        }

        saveButton.setEnabled(false);
        saveButton.setText("Saving...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                if (existing == null) {
                    PatientService.add(patient);
                } else {
                    patient.setPatientId(existing.getPatientId());
                    PatientService.update(patient);
                }
                return null;
            }

            @Override
            protected void done() {
                saveButton.setEnabled(true);
                saveButton.setText("Save");
                try {
                    get();
                    saved = true;
                    dispose();
                } catch (ExecutionException e) {
                    JOptionPane.showMessageDialog(PatientDialog.this,
                            e.getCause().getMessage(), "Could not save patient",
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
