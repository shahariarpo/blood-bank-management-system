package com.bloodbank.ui.panels;

import com.bloodbank.ui.UIComponents;

import javax.swing.JPanel;
import java.awt.BorderLayout;

/** Placeholder until Milestone 8 implements the basic reports. */
public class ReportsPanel extends JPanel {

    public ReportsPanel() {
        setLayout(new BorderLayout());
        setBackground(UIComponents.LIGHT_GREY);
        add(UIComponents.createPageHeader(
                "Reports", "Donation, inventory and issuing summaries"), BorderLayout.NORTH);
        add(UIComponents.createPlaceholder(
                "Reports",
                "Donor counts, donation history, inventory by group and issuing history arrive in Milestone 8."),
                BorderLayout.CENTER);
    }
}
