package com.bloodbank.ui.dialogs;

import com.bloodbank.dao.DonationDAO;
import com.bloodbank.model.Donation;
import com.bloodbank.model.Donor;
import com.bloodbank.ui.UIComponents;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** Shows one donor's details and their donation history. */
public class DonorProfileDialog extends JDialog {

    private final Donor donor;
    private final DefaultTableModel historyModel = new DefaultTableModel(
            new String[]{"ID", "Date", "Qty (ml)", "Screening", "Notes"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public DonorProfileDialog(Frame owner, Donor donor) {
        super(owner, "Donor profile", true);
        this.donor = donor;

        setSize(640, 440);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        add(buildDetails(), BorderLayout.NORTH);

        JTable historyTable = new JTable(historyModel);
        historyTable.setRowHeight(26);
        JScrollPane scrollPane = new JScrollPane(historyTable);
        add(UIComponents.createSection("Donation history", scrollPane), BorderLayout.CENTER);

        JButton closeButton = UIComponents.createPrimaryButton("Close");
        closeButton.addActionListener(event -> dispose());
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBorder(BorderFactory.createEmptyBorder(8, 20, 16, 20));
        footer.add(closeButton);
        add(footer, BorderLayout.SOUTH);

        loadHistory();
        setVisible(true);
    }

    private JPanel buildDetails() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 24, 12, 24));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.LINE_END;

        addRow(panel, c, 0, "Donor ID:", String.valueOf(donor.getDonorId()));
        addRow(panel, c, 1, "Name:", donor.getName());
        addRow(panel, c, 2, "Age / Gender:", donor.getAge() + " / " + donor.getGender());
        addRow(panel, c, 3, "Blood group:", donor.getBloodGroup());
        addRow(panel, c, 4, "Phone:", donor.getPhone());
        addRow(panel, c, 5, "Address:", donor.getAddress() == null ? "-" : donor.getAddress());
        addRow(panel, c, 6, "Registered:", donor.getCreatedAt() == null ? "-" : donor.getCreatedAt());
        return panel;
    }

    private void addRow(JPanel panel, GridBagConstraints c,
                        int row, String label, String value) {
        c.gridx = 0;
        c.gridy = row;
        panel.add(new JLabel(label), c);
        c.gridx = 1;
        c.anchor = GridBagConstraints.LINE_START;
        panel.add(new JLabel(value), c);
        c.anchor = GridBagConstraints.LINE_END;
    }

    private void loadHistory() {
        new SwingWorker<List<Donation>, Void>() {
            @Override
            protected List<Donation> doInBackground() throws Exception {
                return DonationDAO.findByDonorId(donor.getDonorId());
            }

            @Override
            protected void done() {
                try {
                    List<Donation> donations = get();
                    historyModel.setRowCount(0);
                    donations.forEach(d -> historyModel.addRow(new Object[]{
                            d.getDonationId(),
                            d.getDonationDate(),
                            d.getQuantityMl(),
                            d.getScreeningStatus(),
                            d.getNotes() == null ? "" : d.getNotes()
                    }));
                } catch (ExecutionException e) {
                    historyModel.setRowCount(0);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }
}
