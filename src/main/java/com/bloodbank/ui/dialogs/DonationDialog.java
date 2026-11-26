package com.bloodbank.ui.dialogs;

import com.bloodbank.model.BloodComponent;
import com.bloodbank.model.Donation;
import com.bloodbank.model.Donor;
import com.bloodbank.service.DonationService;
import com.bloodbank.service.DonorService;
import com.bloodbank.ui.UIComponents;

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

/** Form for recording a donation and optionally screening it as accepted. */
public class DonationDialog extends JDialog {

    private final JComboBox<String> donorCombo = new JComboBox<>();
    private final Map<String, Integer> donorIds = new LinkedHashMap<>();
    private final JTextField dateField = new JTextField(LocalDate.now().toString(), 12);
    private final JTextField quantityField = new JTextField("350", 12);
    private final JComboBox<String> statusCombo =
            new JComboBox<>(new String[]{"PENDING", "ACCEPTED", "REJECTED"});
    private final JComboBox<BloodComponent> componentCombo =
            new JComboBox<>(BloodComponent.values());
    private final JTextField notesField = new JTextField(22);
    private final JButton saveButton = UIComponents.createPrimaryButton("Save donation");

    private boolean saved;

    public DonationDialog(Frame owner) {
        super(owner, "Record donation", true);
        buildUi();
        pack();
        setLocationRelativeTo(owner);
        loadDonors();
        setVisible(true);
    }

    private void buildUi() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 12, 24));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.LINE_END;

        addField(form, c, 0, "Donor:", donorCombo);
        addField(form, c, 1, "Date (yyyy-MM-dd):", dateField);
        addField(form, c, 2, "Quantity (ml):", quantityField);
        addField(form, c, 3, "Screening:", statusCombo);
        addField(form, c, 4, "Component:", componentCombo);
        addField(form, c, 5, "Notes:", notesField);
        add(form, BorderLayout.CENTER);

        componentCombo.setEnabled(false);
        statusCombo.addActionListener(event ->
                componentCombo.setEnabled("ACCEPTED".equals(statusCombo.getSelectedItem())));

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

    private void loadDonors() {
        new SwingWorker<List<Donor>, Void>() {
            @Override
            protected List<Donor> doInBackground() throws Exception {
                return DonorService.getAll();
            }

            @Override
            protected void done() {
                try {
                    for (Donor donor : get()) {
                        String label = donor.getDonorId() + " - " + donor.getName()
                                + " (" + donor.getBloodGroup() + ")";
                        donorIds.put(label, donor.getDonorId());
                        donorCombo.addItem(label);
                    }
                    if (donorCombo.getItemCount() == 0) {
                        JOptionPane.showMessageDialog(DonationDialog.this,
                                "Register a donor before recording a donation.",
                                "No donors", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (ExecutionException e) {
                    JOptionPane.showMessageDialog(DonationDialog.this,
                            e.getCause().getMessage(), "Could not load donors",
                            JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private Donation collectDonation() {
        Integer donorId = donorIds.get(donorCombo.getSelectedItem());
        if (donorId == null) {
            JOptionPane.showMessageDialog(this, "Select a donor.",
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

        int quantity;
        try {
            quantity = Integer.parseInt(quantityField.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Quantity must be a whole number.",
                    "Validation error", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        return new Donation(donorId, date, quantity,
                (String) statusCombo.getSelectedItem(), notesField.getText());
    }

    private void save() {
        Donation donation = collectDonation();
        if (donation == null) {
            return;
        }
        BloodComponent component = "ACCEPTED".equals(donation.getScreeningStatus())
                ? (BloodComponent) componentCombo.getSelectedItem() : null;

        saveButton.setEnabled(false);
        saveButton.setText("Saving...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                DonationService.add(donation, component);
                return null;
            }

            @Override
            protected void done() {
                saveButton.setEnabled(true);
                saveButton.setText("Save donation");
                try {
                    get();
                    saved = true;
                    dispose();
                } catch (ExecutionException e) {
                    JOptionPane.showMessageDialog(DonationDialog.this,
                            e.getCause().getMessage(), "Could not save donation",
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
