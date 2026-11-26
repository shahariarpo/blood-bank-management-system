package com.bloodbank.ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

/**
 * Small factory methods for the Swing widgets used across panels.
 * Reusing these keeps spacing, colours and fonts consistent.
 */
public final class UIComponents {

    public static final Color RED = new Color(183, 28, 28);
    public static final Color DARK = new Color(31, 41, 51);
    public static final Color LIGHT_GREY = new Color(245, 246, 248);
    public static final Color BORDER = new Color(220, 224, 229);
    public static final Color MUTED = new Color(105, 112, 121);

    private UIComponents() {
        // Utility class - no instances.
    }

    public static JPanel createPanel(Color background) {
        JPanel panel = new JPanel();
        panel.setBackground(background);
        return panel;
    }

    public static JLabel createTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 23));
        label.setForeground(DARK);
        return label;
    }

    public static JLabel createSectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        label.setForeground(DARK);
        return label;
    }

    public static JLabel createSubtitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        label.setForeground(MUTED);
        return label;
    }

    /** Header shown at the top of every content panel. */
    public static JPanel createPageHeader(String title, String subtitle) {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createEmptyBorder(22, 28, 16, 28));

        JLabel titleLabel = createTitle(title);
        titleLabel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        JLabel subtitleLabel = createSubtitle(subtitle);
        subtitleLabel.setAlignmentX(JComponent.LEFT_ALIGNMENT);

        header.add(titleLabel);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitleLabel);
        return header;
    }

    public static JButton createPrimaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(RED);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        button.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        return button;
    }

    /** Full-width button for the dark sidebar; highlight it by changing its background. */
    public static JButton createSidebarButton(String text) {
        JButton button = new JButton(text);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBackground(DARK);
        button.setForeground(new Color(226, 232, 240));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        button.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        button.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        return button;
    }

    /** Dashboard metric card with a label and a large value. */
    public static JPanel createStatCard(String label, String value, Color valueColor) {
        JPanel card = createPanel(Color.WHITE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));

        JLabel labelField = createSubtitle(label);
        labelField.setAlignmentX(JComponent.LEFT_ALIGNMENT);

        JLabel valueField = new JLabel(value, SwingConstants.LEFT);
        valueField.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        valueField.setForeground(valueColor);
        valueField.setAlignmentX(JComponent.LEFT_ALIGNMENT);

        card.add(labelField);
        card.add(Box.createVerticalStrut(8));
        card.add(valueField);
        return card;
    }

    /** White panel with a heading and any content component inside it. */
    public static JPanel createSection(String title, JComponent content) {
        JPanel section = createPanel(Color.WHITE);
        section.setLayout(new BorderLayout(0, 10));
        section.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        section.add(createSectionTitle(title), BorderLayout.NORTH);
        section.add(content, BorderLayout.CENTER);
        return section;
    }

    /** Read-only table with a fixed header. */
    public static JTable createTable(String[] columnNames) {
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.setRowHeight(28);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        return table;
    }

    /** Centred "coming later" message used by panels not yet implemented. */
    public static JPanel createPlaceholder(String title, String message) {
        JPanel wrapper = createPanel(LIGHT_GREY);
        wrapper.setLayout(new java.awt.GridBagLayout());

        JPanel box = createPanel(Color.WHITE);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(32, 40, 32, 40)));

        JLabel heading = createSectionTitle(title);
        heading.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        JLabel detail = createSubtitle(message);
        detail.setAlignmentX(JComponent.CENTER_ALIGNMENT);

        box.add(heading);
        box.add(Box.createVerticalStrut(10));
        box.add(detail);
        wrapper.add(box);
        return wrapper;
    }

    /** Small note line used under the dashboard. */
    public static JPanel createNoteLine(String text) {
        JPanel note = createPanel(LIGHT_GREY);
        note.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        note.add(createSubtitle(text));
        return note;
    }
}
