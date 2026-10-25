package com.matmon.skidb.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/** Left column: the list of collections with create / delete actions. */
final class CollectionsPanel extends JPanel {

    private final MainFrame frame;
    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final JList<String> list = new JList<>(model);
    private final JButton delete = Theme.button("Delete");
    private boolean updating;

    CollectionsPanel(MainFrame frame) {
        super(new BorderLayout(0, 8));
        this.frame = frame;
        setBackground(Theme.PANEL);
        setBorder(Theme.cardBorder());

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellHeight(28);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object value, int index, boolean selected, boolean focus) {
                JLabel c = (JLabel) super.getListCellRendererComponent(l, value, index, selected, false);
                c.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 3, 0, 0, selected ? Theme.ACCENT : Theme.PANEL),
                        new EmptyBorder(0, 8, 0, 4)));
                c.setFont(selected ? Theme.BOLD : Theme.BASE);
                return c;
            }
        });
        list.addListSelectionListener(e -> {
            if (!updating && !e.getValueIsAdjusting()) {
                delete.setEnabled(list.getSelectedValue() != null);
                frame.onCollectionSelected(list.getSelectedValue());
            }
        });

        JButton create = Theme.button("+ New", true);
        create.addActionListener(e -> createCollection());
        delete.setEnabled(false);
        delete.addActionListener(e -> deleteCollection());

        JPanel buttons = new JPanel(new GridLayout(1, 2, 8, 0));
        buttons.setOpaque(false);
        buttons.add(create);
        buttons.add(delete);

        add(Theme.sectionHeader("Collections"), BorderLayout.NORTH);
        add(Theme.scroll(list), BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
    }

    /** Replaces the list content without firing selection events back to the frame. */
    void setNames(List<String> names, String selected) {
        updating = true;
        try {
            model.clear();
            names.forEach(model::addElement);
            list.setSelectedValue(selected, true);
            delete.setEnabled(selected != null);
        } finally {
            updating = false;
        }
    }

    private void createCollection() {
        CreateCollectionDialog dialog = new CreateCollectionDialog(SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);
        if (dialog.getCollectionName() != null) {
            frame.createCollection(dialog.getCollectionName(), dialog.getSchema());
        }
    }

    private void deleteCollection() {
        String name = list.getSelectedValue();
        if (name == null) {
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this,
                "Delete collection \"" + name + "\" and all of its records?",
                "Delete collection", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer == JOptionPane.OK_OPTION) {
            frame.deleteCollection(name);
        }
    }
}
