package com.bloodbank.ui.panels;

import com.bloodbank.model.BloodIssue;
import com.bloodbank.model.BloodRequest;
import com.bloodbank.service.IssueService;
import com.bloodbank.service.RequestService;
import com.bloodbank.ui.UIComponents;
import com.bloodbank.ui.dialogs.IssueDialog;
import com.bloodbank.ui.dialogs.RequestDialog;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
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

/** Requests, approvals and transaction-safe issuing. */
public class RequestPanel extends JPanel {

    private static final String[] REQUEST_COLUMNS = {
            "ID", "Patient", "Patient group", "Requested", "Qty", "Date", "Priority", "Status"
    };
    private static final String[] ISSUE_COLUMNS = {
            "Issue", "Request", "Patient", "Unit", "Blood group", "Component", "Date"
    };

    private final JTextField searchField = new JTextField(20);
    private final JComboBox<String> statusFilter = new JComboBox<>();
    private final JButton searchButton = new JButton("Search");
    private final JButton clearButton = new JButton("Clear");
    private final JButton refreshButton = new JButton("Refresh");
    private final JButton addButton = UIComponents.createPrimaryButton("New request");
    private final JButton approveButton = new JButton("Approve");
    private final JButton rejectButton = new JButton("Reject");
    private final JButton issueButton = new JButton("Issue blood");

    private final JTable requestTable = UIComponents.createTable(REQUEST_COLUMNS);
    private final DefaultTableModel requestModel = (DefaultTableModel) requestTable.getModel();
    private final JTable issueTable = UIComponents.createTable(ISSUE_COLUMNS);
    private final DefaultTableModel issueModel = (DefaultTableModel) issueTable.getModel();

    private List<BloodRequest> displayedRequests = new ArrayList<>();
    private boolean busy;

    public RequestPanel() {
        setLayout(new BorderLayout());
        setBackground(UIComponents.LIGHT_GREY);

        statusFilter.addItem("ALL");
        RequestService.STATUSES.forEach(statusFilter::addItem);

        JPanel top = new JPanel(new BorderLayout());
        top.add(UIComponents.createPageHeader(
                "Requests", "Manage patients, blood requests and issuing"), BorderLayout.NORTH);
        top.add(createToolbar(), BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        add(createActions(), BorderLayout.SOUTH);

        requestTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        requestTable.getColumnModel().getColumn(1).setPreferredWidth(140);
        requestTable.getColumnModel().getColumn(7).setPreferredWidth(150);
        requestTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateActionButtons();
            }
        });

        issueTable.getColumnModel().getColumn(0).setPreferredWidth(55);
        issueTable.getColumnModel().getColumn(1).setPreferredWidth(65);
        issueTable.getColumnModel().getColumn(2).setPreferredWidth(140);
        issueTable.getColumnModel().getColumn(3).setPreferredWidth(65);
        issueTable.getColumnModel().getColumn(6).setPreferredWidth(110);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                UIComponents.createSection("Blood requests", new JScrollPane(requestTable)),
                UIComponents.createSection("Issuing history", new JScrollPane(issueTable)));
        split.setResizeWeight(0.68);
        split.setBorder(BorderFactory.createEmptyBorder(8, 28, 18, 28));
        add(split, BorderLayout.CENTER);

        searchButton.addActionListener(event -> loadData());
        searchField.addActionListener(event -> loadData());
        clearButton.addActionListener(event -> {
            searchField.setText("");
            statusFilter.setSelectedItem("ALL");
            loadData();
        });
        refreshButton.addActionListener(event -> loadData());
        statusFilter.addActionListener(event -> loadData());
        addButton.addActionListener(event -> openNewDialog());
        approveButton.addActionListener(event -> changeStatus("APPROVED"));
        rejectButton.addActionListener(event -> changeStatus("REJECTED"));
        issueButton.addActionListener(event -> openIssueDialog());

        updateActionButtons();
        loadData();
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.setBackground(Color.WHITE);
        toolbar.setBorder(BorderFactory.createEmptyBorder(8, 22, 8, 22));
        toolbar.add(new JLabel("Search:"));
        toolbar.add(searchField);
        toolbar.add(new JLabel("Status:"));
        toolbar.add(statusFilter);
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
        actions.add(approveButton);
        actions.add(rejectButton);
        actions.add(issueButton);
        actions.add(Box.createHorizontalStrut(8));
        actions.add(UIComponents.createSubtitle(
                "Issuing is FEFO, transactional, and not clinical authorization."));
        return actions;
    }

    private void loadData() {
        busy = true;
        updateActionButtons();
        setControlsEnabled(false);

        String keyword = searchField.getText();
        String status = (String) statusFilter.getSelectedItem();
        new SwingWorker<RequestSnapshot, Void>() {
            @Override
            protected RequestSnapshot doInBackground() throws Exception {
                return new RequestSnapshot(
                        "ALL".equals(status)
                                ? RequestService.search(keyword)
                                : RequestService.findByStatus(status),
                        IssueService.getIssuingHistory());
            }

            @Override
            protected void done() {
                busy = false;
                try {
                    populate(get());
                } catch (ExecutionException e) {
                    JOptionPane.showMessageDialog(RequestPanel.this,
                            e.getCause().getMessage(), "Could not load requests",
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
        statusFilter.setEnabled(enabled);
        addButton.setEnabled(enabled);
    }

    private void populate(RequestSnapshot snapshot) {
        displayedRequests = snapshot.requests;
        requestModel.setRowCount(0);
        for (BloodRequest request : snapshot.requests) {
            requestModel.addRow(new Object[]{
                    request.getRequestId(),
                    request.getPatientName(),
                    request.getPatientBloodGroup(),
                    request.getBloodGroup(),
                    request.getQuantity(),
                    request.getRequestDate(),
                    request.getPriority(),
                    request.getStatus()
            });
        }

        issueModel.setRowCount(0);
        for (BloodIssue issue : snapshot.issues) {
            issueModel.addRow(new Object[]{
                    issue.getIssueId(),
                    issue.getRequestId(),
                    issue.getPatientName(),
                    issue.getUnitId(),
                    issue.getBloodGroup(),
                    issue.getComponentType(),
                    issue.getIssueDate()
            });
        }
    }

    private BloodRequest selectedRequest() {
        int row = requestTable.getSelectedRow();
        return row < 0 || row >= displayedRequests.size() ? null : displayedRequests.get(row);
    }

    private void updateActionButtons() {
        BloodRequest request = selectedRequest();
        boolean pending = !busy && request != null && "PENDING".equals(request.getStatus());
        boolean issuable = !busy && request != null
                && RequestService.ISSUABLE_STATUSES.contains(request.getStatus());
        approveButton.setEnabled(pending);
        rejectButton.setEnabled(pending);
        issueButton.setEnabled(issuable);
    }

    private Frame ownerFrame() {
        return (Frame) SwingUtilities.getWindowAncestor(this);
    }

    private void openNewDialog() {
        RequestDialog dialog = new RequestDialog(ownerFrame());
        if (dialog.isSaved()) {
            loadData();
        }
    }

    private void changeStatus(String target) {
        BloodRequest request = selectedRequest();
        if (request == null) {
            return;
        }
        String verb = "APPROVED".equals(target) ? "approve" : "reject";
        int answer = JOptionPane.showConfirmDialog(this,
                verb.substring(0, 1).toUpperCase() + verb.substring(1)
                        + " request #" + request.getRequestId() + "?",
                "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        runAction(() -> {
            if ("APPROVED".equals(target)) {
                RequestService.approve(request.getRequestId());
            } else {
                RequestService.reject(request.getRequestId());
            }
        });
    }

    private void openIssueDialog() {
        BloodRequest request = selectedRequest();
        if (request == null || !RequestService.ISSUABLE_STATUSES.contains(request.getStatus())) {
            return;
        }
        IssueDialog dialog = new IssueDialog(ownerFrame(), request);
        if (dialog.isChanged()) {
            loadData();
        }
    }

    private void runAction(DatabaseAction action) {
        busy = true;
        updateActionButtons();
        setControlsEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                action.run();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    loadData();
                } catch (ExecutionException e) {
                    busy = false;
                    setControlsEnabled(true);
                    updateActionButtons();
                    JOptionPane.showMessageDialog(RequestPanel.this,
                            e.getCause().getMessage(), "Could not update request",
                            JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException e) {
                    busy = false;
                    setControlsEnabled(true);
                    updateActionButtons();
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    @FunctionalInterface
    private interface DatabaseAction {
        void run() throws Exception;
    }

    private static final class RequestSnapshot {
        private final List<BloodRequest> requests;
        private final List<BloodIssue> issues;

        private RequestSnapshot(List<BloodRequest> requests, List<BloodIssue> issues) {
            this.requests = requests;
            this.issues = issues;
        }
    }
}
