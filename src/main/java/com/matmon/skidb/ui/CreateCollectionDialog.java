package com.matmon.skidb.ui;

import com.matmon.skidb.api.IndexType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Modal dialog that asks for a collection name and its schema (field name + type). */
final class CreateCollectionDialog extends JDialog {

    private static final String NO_INDEX = "No index";
    private static final String NO_PARTITION = "None";

    private final JTextField nameField = Theme.field();
    private final JPanel rowsPanel = new JPanel();
    private final List<FieldRow> rows = new ArrayList<>();

    private String collectionName;
    private Map<String, String> schema;
    private Map<String, IndexType> indexedFields;
    private String partitionField;
    private final JComboBox<String> partitionCombo = new JComboBox<>(new String[]{NO_PARTITION});

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
        scroll.setPreferredSize(new Dimension(500, 200));

        JButton addField = Theme.button("+ Add field");
        addField.addActionListener(e -> addRow("", FieldTypes.STRING));
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

        partitionCombo.setPreferredSize(new Dimension(180, 28));
        partitionCombo.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        partitionCombo.setToolTipText("Split the records into one partition per hour of a DateTime field");
        partitionCombo.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                refreshPartitionChoices();
            }

            @Override
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
            }
        });
        JPanel partitionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        partitionRow.setOpaque(false);
        partitionRow.setBorder(new EmptyBorder(8, 0, 0, 0));
        partitionRow.add(Theme.label("Partition by hour of"));
        partitionRow.add(partitionCombo);
        partitionRow.add(Theme.muted("(a DateTime field)"));
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(scroll, BorderLayout.CENTER);
        center.add(partitionRow, BorderLayout.SOUTH);

        content.add(top, BorderLayout.NORTH);
        content.add(center, BorderLayout.CENTER);
        content.add(bottom, BorderLayout.SOUTH);

        addRow("", FieldTypes.STRING);

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

    /** @return the fields to create an index on and the index type, in the order entered */
    Map<String, IndexType> getIndexedFields() {
        return indexedFields;
    }

    /** @return the DateTime field to partition by the hour of, or null for no partitions */
    String getPartitionField() {
        return partitionField;
    }

    /** Offers the DateTime fields currently in the dialog, keeping the selection if it is still one. */
    private void refreshPartitionChoices() {
        Object selected = partitionCombo.getSelectedItem();
        partitionCombo.removeAllItems();
        partitionCombo.addItem(NO_PARTITION);
        for (FieldRow row : rows) {
            String field = row.name.getText().trim();
            if (!field.isEmpty() && FieldTypes.DATETIME.equals(row.type.getSelectedItem())) {
                partitionCombo.addItem(field);
            }
        }
        partitionCombo.setSelectedItem(selected);
        if (partitionCombo.getSelectedIndex() < 0) {
            partitionCombo.setSelectedIndex(0);
        }
    }

    private void addRow(String fieldName, String type) {
        FieldRow row = new FieldRow(fieldName, type);
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
        Map<String, IndexType> indexed = new LinkedHashMap<>();
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
            if (row.index.getSelectedItem() instanceof IndexType t) {
                indexed.put(field, t);
            }
        }
        Object partition = partitionCombo.getSelectedItem();
        if (partition != null && !NO_PARTITION.equals(partition)
                && !FieldTypes.DATETIME.equals(result.get((String) partition))) {
            warn("The partition field \"" + partition + "\" must be a DateTime field of the schema.");
            return;
        }
        collectionName = name;
        schema = result;
        indexedFields = indexed;
        partitionField = partition == null || NO_PARTITION.equals(partition) ? null : (String) partition;
        dispose();
    }

    private void warn(String message) {
        JOptionPane.showMessageDialog(this, message, "New collection", JOptionPane.WARNING_MESSAGE);
    }

    private final class FieldRow extends JPanel {
        final JTextField name = Theme.field();
        final JComboBox<String> type = new JComboBox<>(FieldTypes.ALL);
        final JComboBox<Object> index = new JComboBox<>(new Object[]{NO_INDEX, IndexType.HASH, IndexType.TREE});

        FieldRow(String fieldName, String fieldType) {
            super(new BorderLayout(8, 0));
            setOpaque(false);
            setBorder(new EmptyBorder(0, 0, 6, 0));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            name.setText(fieldName);
            name.setToolTipText("Field name");
            type.setSelectedItem(fieldType);
            type.setToolTipText("Field type");
            type.setPreferredSize(new Dimension(110, 28));
            JButton remove = Theme.button("✕");
            remove.setToolTipText("Remove field");
            remove.addActionListener(e -> removeRow(this));

            JPanel east = new JPanel(new BorderLayout(8, 0));
            east.setOpaque(false);
            index.setPreferredSize(new Dimension(100, 28));
            index.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
            index.setToolTipText("Create an index on this field: HASH for \"=\" lookups, TREE also for ranges");
            JPanel typeAndIndex = new JPanel(new BorderLayout(8, 0));
            typeAndIndex.setOpaque(false);
            typeAndIndex.add(type, BorderLayout.CENTER);
            typeAndIndex.add(index, BorderLayout.EAST);
            east.add(typeAndIndex, BorderLayout.CENTER);
            east.add(remove, BorderLayout.EAST);
            add(name, BorderLayout.CENTER);
            add(east, BorderLayout.EAST);
        }
    }
}
