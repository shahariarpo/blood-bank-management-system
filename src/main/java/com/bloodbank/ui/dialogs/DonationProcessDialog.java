package com.bloodbank.ui.dialogs;

import com.bloodbank.model.BloodComponent;
import com.bloodbank.model.Donation;
import com.bloodbank.service.DonationService;
import com.bloodbank.ui.UIComponents;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.concurrent.ExecutionException;

/** Screening decision for a PENDING donation. */
public class DonationProcessDialog extends JDialog {

    private final Donation donation;
    private final JComboBox<BloodComponent> componentCombo =
            new JComboBox<>(BloodComponent.values());
    private final JButton acceptButton = UIComponents.createPrimaryButton("Accept & create unit");
    private final JButton rejectButton = new JButton("Reject");
    private boolean changed;

    public DonationProcessDialog(Frame owner, Donation donation) {
        super(owner, "Process donation #" + donation.getDonationId(), true);
        this.donation = donation;

        setSize(480, 330);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        JPanel details = new JPanel(new GridBagLayout());
        details.setBackground(Color.WHITE);
        details.setBorder(BorderFactory.createEmptyBorder(18, 24, 8, 24));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.LINE_END;

        addRow(details, c, 0, "Donor:", donation.getDonorName());
        addRow(details, c, 1, "Blood group:", donation.getBloodGroup());
        addRow(details, c, 2, "Date:", donation.getDonationDate().toString());
        addRow(details, c, 3, "Quantity:", donation.getQuantityMl() + " ml");
        addRow(details, c, 4, "Component:", componentCombo);
        add(details, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setBorder(BorderFactory.createEmptyBorder(8, 20, 18, 20));
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(event -> dispose());
        rejectButton.addActionListener(event -> reject());
        acceptButton.addActionListener(event -> accept());
        buttons.add(cancelButton);
        buttons.add(rejectButton);
        buttons.add(acceptButton);
        add(buttons, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void addRow(JPanel panel, GridBagConstraints c, int row,
                        String label, Object value) {
        c.gridx = 0;
        c.gridy = row;
        panel.add(new JLabel(label), c);
        c.gridx = 1;
        c.anchor = GridBagConstraints.LINE_START;
        panel.add(value instanceof java.awt.Component
                ? (java.awt.Component) value : new JLabel(String.valueOf(value)), c);
        c.anchor = GridBagConstraints.LINE_END;
    }

    private void accept() {
        runDecision(true);
    }

    private void reject() {
        int answer = JOptionPane.showConfirmDialog(this,
                "Reject this donation? No blood unit will be created.",
                "Confirm rejection", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer == JOptionPane.YES_OPTION) {
            runDecision(false);
        }
    }

    private void runDecision(boolean accepting) {
        acceptButton.setEnabled(false);
        rejectButton.setEnabled(false);
        BloodComponent component = (BloodComponent) componentCombo.getSelectedItem();

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                if (accepting) {
                    DonationService.accept(donation.getDonationId(), component);
                } else {
                    DonationService.reject(donation.getDonationId());
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    changed = true;
                    dispose();
                } catch (ExecutionException e) {
                    acceptButton.setEnabled(true);
                    rejectButton.setEnabled(true);
                    JOptionPane.showMessageDialog(DonationProcessDialog.this,
                            e.getCause().getMessage(), "Could not process donation",
                            JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    public boolean isChanged() {
        return changed;
    }
}
