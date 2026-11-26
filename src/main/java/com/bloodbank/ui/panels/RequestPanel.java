package com.bloodbank.ui.panels;

import com.bloodbank.ui.UIComponents;

import javax.swing.JPanel;
import java.awt.BorderLayout;

/** Placeholder until Milestone 7 implements patients, requests and issuing. */
public class RequestPanel extends JPanel {

    public RequestPanel() {
        setLayout(new BorderLayout());
        setBackground(UIComponents.LIGHT_GREY);
        add(UIComponents.createPageHeader(
                "Requests", "Manage patients, blood requests and issuing"), BorderLayout.NORTH);
        add(UIComponents.createPlaceholder(
                "Blood requests",
                "Patient records, request approval/rejection and transaction-safe issuing arrive in Milestone 7."),
                BorderLayout.CENTER);
    }
}
