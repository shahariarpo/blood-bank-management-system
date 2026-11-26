package com.bloodbank.ui.panels;

import com.bloodbank.model.BloodIssue;
import com.bloodbank.model.BloodRequest;
import com.bloodbank.service.IssueService;
import com.bloodbank.service.RequestService;
import com.bloodbank.service.ReservationService;
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
import java.util.Map;
import java.util.concurrent.ExecutionException;

/** Requests, approvals and transaction-safe issuing. */
public class RequestPanel extends JPanel {

    private static final String[] REQUEST_COLUMNS = {
            "ID", "Patient", "Patient group", "Requested", "Qty", "Reserved",
            "Date", "Priority", "Status"
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
    private final JButton cancelButton = new JButton("Cancel");
    private final JButton issueButton = new JButton("Issue blood");

    private final JTable requestTable = UIComponents.createTable(REQUEST_COLUMNS);
    private final DefaultTableModel requestModel = (DefaultTableModel) requestTable.getModel();
    private final JTable issueTable = UIComponents.createTable(ISSUE_COLUMNS);
    private final DefaultTableModel issueModel = (DefaultTableModel) issueTable.getModel();

    private List<BloodRequest> displayedRequests = new ArrayList<>();
    private Map<Integer, Integer> displayedReservations = Map.of();
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
        requestTable.getColumnModel().getColumn(8).setPreferredWidth(150);
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
        approveButton.addActionListener(event -> approveSelectedRequest());
        rejectButton.addActionListener(event -> rejectSelectedRequest());
        cancelButton.addActionListener(event -> cancelSelectedRequest());
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
        actions.add(cancelButton);
        actions.add(issueButton);
        actions.add(Box.createHorizontalStrut(8));
        actions.add(UIComponents.createSubtitle(
                "Approval reserves FEFO units; issuing is transactional and not clinical authorization."));
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
                        IssueService.getIssuingHistory(),
                        ReservationService.getCountsByRequest());
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
        displayedReservations = snapshot.reservations;
        requestModel.setRowCount(0);
        for (BloodRequest request : snapshot.requests) {
            int reserved = snapshot.reservations.getOrDefault(request.getRequestId(), 0);
            requestModel.addRow(new Object[]{
                    request.getRequestId(),
                    request.getPatientName(),
                    request.getPatientBloodGroup(),
                    request.getBloodGroup(),
                    request.getQuantity(),
                    reserved + " / " + request.getQuantity(),
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
        boolean rejectable = !busy && request != null
                && List.of("PENDING", "APPROVED").contains(request.getStatus());
        boolean cancellable = !busy && request != null
                && List.of("APPROVED", "PARTIALLY_FULFILLED").contains(request.getStatus());
        approveButton.setEnabled(pending);
        rejectButton.setEnabled(rejectable);
        cancelButton.setEnabled(cancellable);
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

    private void approveSelectedRequest() {
        BloodRequest request = selectedRequest();
        if (request == null) {
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this,
                "Approve request #" + request.getRequestId()
                        + "? Compatible FEFO units will be reserved and removed from "
                        + "available inventory.",
                "Confirm approval", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        busy = true;
        updateActionButtons();
        setControlsEnabled(false);
        new SwingWorker<RequestService.ApprovalResult, Void>() {
            @Override
            protected RequestService.ApprovalResult doInBackground() throws Exception {
                return RequestService.approve(request.getRequestId());
            }

            @Override
            protected void done() {
                busy = false;
                try {
                    RequestService.ApprovalResult result = get();
                    loadData();
                    int type = result.isPartial()
                            ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE;
                    JOptionPane.showMessageDialog(RequestPanel.this,
                            result.message(), "Request approved", type);
                } catch (ExecutionException e) {
                    controlsFailed(e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    controlsFailed(e);
                }
            }
        }.execute();
    }

    private void rejectSelectedRequest() {
        BloodRequest request = selectedRequest();
        if (request == null) {
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this,
                "Reject request #" + request.getRequestId()
                        + "? Any reservation will be released.",
                "Confirm rejection", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        runAction(() -> RequestService.reject(request.getRequestId()));
    }

    private void cancelSelectedRequest() {
        BloodRequest request = selectedRequest();
        if (request == null) {
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this,
                "Cancel request #" + request.getRequestId()
                        + "? Any reservation will be released.",
                "Confirm cancellation", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        runAction(() -> RequestService.cancel(request.getRequestId()));
    }

    private void controlsFailed(Exception e) {
        setControlsEnabled(true);
        updateActionButtons();
        String message = e.getCause() == null ? e.getMessage() : e.getCause().getMessage();
        JOptionPane.showMessageDialog(RequestPanel.this,
                message, "Could not update request", JOptionPane.ERROR_MESSAGE);
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
        private final Map<Integer, Integer> reservations;

        private RequestSnapshot(List<BloodRequest> requests, List<BloodIssue> issues,
                                Map<Integer, Integer> reservations) {
            this.requests = requests;
            this.issues = issues;
            this.reservations = reservations;
        }
    }
}
