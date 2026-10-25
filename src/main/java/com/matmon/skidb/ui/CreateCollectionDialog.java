package com.matmon.skidb.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Modal dialog that asks for a collection name and its schema (field name + type). */
final class CreateCollectionDialog extends JDialog {

    /** Stage A supports String fields only. */
    private static final String[] TYPES = {"String"};

    private final JTextField nameField = Theme.field();
    private final JPanel rowsPanel = new JPanel();
    private final List<FieldRow> rows = new ArrayList<>();

    private String collectionName;
    private Map<String, String> schema;

    CreateCollectionDialog(Window owner) {
        super(owner, "New collection", ModalityType.APPLICATION_MODAL);
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBackground(Theme.PANEL);
        content.setBorder(new EmptyBorder(16, 16, 16, 16));
        setContentPane(content);

        JPanel nameRow = new JPanel(new BorderLayout(8, 0));
        nameRow.setOpaque(false);
        nameRow.add(Theme.label("Collection name"), BorderLayout.WEST);
        nameRow.add(nameField, BorderLayout.CENTER);

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(nameRow, BorderLayout.NORTH);
        top.add(Theme.sectionHeader("Schema"), BorderLayout.SOUTH);

        rowsPanel.setLayout(new BoxLayout(rowsPanel, BoxLayout.Y_AXIS));
        rowsPanel.setBackground(Theme.PANEL);
        JPanel rowsHolder = new JPanel(new BorderLayout());
        rowsHolder.setBackground(Theme.PANEL);
        rowsHolder.add(rowsPanel, BorderLayout.NORTH);
        JScrollPane scroll = Theme.scroll(rowsHolder);
        scroll.setBorder(null);
        scroll.setPreferredSize(new Dimension(420, 200));

        JButton addField = Theme.button("+ Add field");
        addField.addActionListener(e -> addRow(""));
        JButton cancel = Theme.button("Cancel");
        cancel.addActionListener(e -> dispose());
        JButton create = Theme.button("Create", true);
        create.addActionListener(e -> submit());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(cancel);
        right.add(create);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(addField, BorderLayout.WEST);
        bottom.add(right, BorderLayout.EAST);

        content.add(top, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);
        content.add(bottom, BorderLayout.SOUTH);

        addRow("firstName");
        addRow("lastName");

        getRootPane().setDefaultButton(create);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack();
        setLocationRelativeTo(owner);
    }

    /** @return the entered name, or null if the dialog was cancelled */
    String getCollectionName() {
        return collectionName;
    }

    /** @return field name -> type, in the order entered */
    Map<String, String> getSchema() {
        return schema;
    }

    private void addRow(String fieldName) {
        FieldRow row = new FieldRow(fieldName);
        rows.add(row);
        rowsPanel.add(row);
        rowsPanel.revalidate();
        rowsPanel.repaint();
        row.name.requestFocusInWindow();
    }

    private void removeRow(FieldRow row) {
        rows.remove(row);
        rowsPanel.remove(row);
        rowsPanel.revalidate();
        rowsPanel.repaint();
    }

    private void submit() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            warn("Please enter a collection name.");
            return;
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (FieldRow row : rows) {
            String field = row.name.getText().trim();
            if (field.isEmpty()) {
                continue;
            }
            if (result.containsKey(field)) {
                warn("Field \"" + field + "\" appears more than once.");
                return;
            }
            result.put(field, (String) row.type.getSelectedItem());
        }
        collectionName = name;
        schema = result;
        dispose();
    }

    private void warn(String message) {
        JOptionPane.showMessageDialog(this, message, "New collection", JOptionPane.WARNING_MESSAGE);
    }

    private final class FieldRow extends JPanel {
        final JTextField name = Theme.field();
        final JComboBox<String> type = new JComboBox<>(TYPES);

        FieldRow(String fieldName) {
            super(new BorderLayout(8, 0));
            setOpaque(false);
            setBorder(new EmptyBorder(0, 0, 6, 0));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            name.setText(fieldName);
            name.setToolTipText("Field name");
            type.setToolTipText("Stage A supports String fields only");
            type.setPreferredSize(new Dimension(110, 28));
            JButton remove = Theme.button("✕");
            remove.setToolTipText("Remove field");
            remove.addActionListener(e -> removeRow(this));

            JPanel east = new JPanel(new BorderLayout(8, 0));
            east.setOpaque(false);
            east.add(type, BorderLayout.CENTER);
            east.add(remove, BorderLayout.EAST);
            add(name, BorderLayout.CENTER);
            add(east, BorderLayout.EAST);
        }
    }
}
