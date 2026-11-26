package com.bloodbank.ui.panels;

import com.bloodbank.ui.UIComponents;

import javax.swing.JPanel;
import java.awt.BorderLayout;

/** Placeholder until Milestone 5 implements donation recording. */
public class DonationPanel extends JPanel {

    public DonationPanel() {
        setLayout(new BorderLayout());
        setBackground(UIComponents.LIGHT_GREY);
        add(UIComponents.createPageHeader(
                "Donations", "Record donations and screening results"), BorderLayout.NORTH);
        add(UIComponents.createPlaceholder(
                "Donation management",
                "Donation form, screening status and automatic blood unit creation arrive in Milestone 5."),
                BorderLayout.CENTER);
    }
}
