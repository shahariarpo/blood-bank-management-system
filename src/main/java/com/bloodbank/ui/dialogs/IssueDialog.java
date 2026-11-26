package com.bloodbank.ui.dialogs;

import com.bloodbank.model.BloodRequest;
import com.bloodbank.model.BloodUnit;
import com.bloodbank.service.IssueService;
import com.bloodbank.ui.UIComponents;
import com.bloodbank.util.BloodCompatibility;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** Transactional issuing screen for an approved request. */
public class IssueDialog extends JDialog {

    private static final String[] PREVIEW_COLUMNS = {"Unit", "Group", "Component", "Expires"};

    private final BloodRequest request;
    private final JTextField quantityField;
    private final JCheckBox allowPartial = new JCheckBox("Allow partial issue");
    private final JLabel eligibleLabel = UIComponents.createSubtitle("Checking eligible units...");
    private final JLabel compatibilityLabel;
    private final JTable previewTable = UIComponents.createTable(PREVIEW_COLUMNS);
    private final DefaultTableModel previewModel = (DefaultTableModel) previewTable.getModel();
    private final JButton issueButton = UIComponents.createPrimaryButton("Issue units");

    private List<BloodUnit> eligibleUnits = new ArrayList<>();
    private boolean changed;

    public IssueDialog(Frame owner, BloodRequest request) {
        super(owner, "Issue blood for request #" + request.getRequestId(), true);
        this.request = request;
        this.quantityField = new JTextField(String.valueOf(request.getQuantity()), 8);
        this.compatibilityLabel = UIComponents.createSubtitle(
                "Eligible donor groups: "
                        + String.join(", ", BloodCompatibility.compatibleDonorGroups(
                                request.getBloodGroup())));

        setSize(620, 520);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        add(buildDetails(), BorderLayout.NORTH);
        add(createPreview(), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);

        loadEligibleUnits();
        setVisible(true);
    }

    private JPanel buildDetails() {
        JPanel details = new JPanel(new GridBagLayout());
        details.setBackground(Color.WHITE);
        details.setBorder(BorderFactory.createEmptyBorder(18, 24, 8, 24));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.LINE_END;

        addRow(details, c, 0, "Request:", "#" + request.getRequestId());
        addRow(details, c, 1, "Patient:", request.getPatientName());
        addRow(details, c, 2, "Requested group:", request.getBloodGroup());
        addRow(details, c, 3, "Requested units:", String.valueOf(request.getQuantity()));
        addRow(details, c, 4, "Priority:", request.getPriority());
        addRow(details, c, 5, "Issue quantity:", quantityField);
        addRow(details, c, 6, "Options:", allowPartial);
        addRow(details, c, 7, "Eligible now:", eligibleLabel);
        addRow(details, c, 8, "Compatibility:", compatibilityLabel);

        JLabel warning = UIComponents.createSubtitle(
                "Illustrative compatibility only. This is NOT clinical authorization: "
                        + "crossmatching and clinical approval are required.");
        warning.setForeground(UIComponents.RED);
        c.gridx = 0;
        c.gridy = 9;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.LINE_START;
        details.add(warning, c);
        return details;
    }

    private JPanel createPreview() {
        previewTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        previewTable.getColumnModel().getColumn(1).setPreferredWidth(70);
        previewTable.getColumnModel().getColumn(2).setPreferredWidth(130);
        previewTable.getColumnModel().getColumn(3).setPreferredWidth(110);
        JScrollPane scroll = new JScrollPane(previewTable);
        scroll.setPreferredSize(new Dimension(560, 180));
        return UIComponents.createSection("Eligible units (FEFO order)", scroll);
    }

    private JPanel buildButtons() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setBorder(BorderFactory.createEmptyBorder(10, 20, 18, 20));
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(event -> dispose());
        issueButton.addActionListener(event -> issue());
        issueButton.setEnabled(false);
        buttons.add(cancelButton);
        buttons.add(issueButton);
        return buttons;
    }

    private void addRow(JPanel panel, GridBagConstraints c, int row,
                        String label, Object value) {
        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.LINE_END;
        panel.add(new JLabel(label), c);
        c.gridx = 1;
        c.anchor = GridBagConstraints.LINE_START;
        panel.add(value instanceof java.awt.Component
                ? (java.awt.Component) value : new JLabel(String.valueOf(value)), c);
    }

    private void loadEligibleUnits() {
        new SwingWorker<List<BloodUnit>, Void>() {
            @Override
            protected List<BloodUnit> doInBackground() throws Exception {
                return IssueService.findEligibleUnits(request.getRequestId());
            }

            @Override
            protected void done() {
                try {
                    eligibleUnits = get();
                    populatePreview();
                    eligibleLabel.setText(eligibleUnits.size() + " available unit(s)");
                    eligibleLabel.setForeground(eligibleUnits.isEmpty()
                            ? UIComponents.RED : UIComponents.MUTED);
                    quantityField.setText(String.valueOf(
                            Math.min(request.getQuantity(), Math.max(eligibleUnits.size(), 1))));
                    issueButton.setEnabled(!eligibleUnits.isEmpty());
                } catch (ExecutionException e) {
                    eligibleLabel.setText("Unavailable");
                    eligibleLabel.setForeground(UIComponents.RED);
                    JOptionPane.showMessageDialog(IssueDialog.this,
                            e.getCause().getMessage(), "Could not load eligible units",
                            JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void populatePreview() {
        previewModel.setRowCount(0);
        for (BloodUnit unit : eligibleUnits) {
            previewModel.addRow(new Object[]{
                    unit.getUnitId(),
                    unit.getBloodGroup(),
                    unit.getComponentType(),
                    unit.getExpiryDate()
            });
        }
    }

    private void issue() {
        int quantity;
        try {
            quantity = Integer.parseInt(quantityField.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Quantity must be a whole number.",
                    "Validation error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        issueButton.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                IssueService.issue(request.getRequestId(), quantity, allowPartial.isSelected());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    changed = true;
                    dispose();
                } catch (ExecutionException e) {
                    issueButton.setEnabled(true);
                    JOptionPane.showMessageDialog(IssueDialog.this,
                            e.getCause().getMessage(), "Could not issue units",
                            JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException e) {
                    issueButton.setEnabled(true);
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    public boolean isChanged() {
        return changed;
    }
}
