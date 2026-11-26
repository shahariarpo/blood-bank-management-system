package com.bloodbank.ui.panels;

import com.bloodbank.ui.UIComponents;

import javax.swing.JPanel;
import java.awt.BorderLayout;

/** Placeholder until Milestone 6 implements blood unit tracking. */
public class InventoryPanel extends JPanel {

    public InventoryPanel() {
        setLayout(new BorderLayout());
        setBackground(UIComponents.LIGHT_GREY);
        add(UIComponents.createPageHeader(
                "Inventory", "Track individual blood units and expiry dates"), BorderLayout.NORTH);
        add(UIComponents.createPlaceholder(
                "Blood inventory",
                "Unit listing, filters, expiry monitoring and FEFO selection arrive in Milestone 6."),
                BorderLayout.CENTER);
    }
}
