package com.matmon.skidb.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Right column: a form for one record. It has a key, one input per schema field, and optional
 * "extra" fields that are not in the schema, used to check that the DBMS rejects them.
 * Each input has a "send as" type, which defaults to the schema type and can be changed to
 * send a wrong type on purpose. Empty inputs are not sent, so Update sends only the fields
 * the user filled in.
 */
final class RecordFormPanel extends JPanel {

    private final MainFrame frame;
    private final JLabel subtitle = Theme.muted(" ");
    private final JTextField keyField = Theme.field();
    private final Map<String, FieldInput> schemaFields = new LinkedHashMap<>();
    private final List<ExtraRow> extraRows = new ArrayList<>();
    private final JPanel body = new JPanel(new GridBagLayout());
    private final JPanel extraPanel = new JPanel();
    private final List<JComponent> actions = new ArrayList<>();

    private String collection;
    private Map<String, String> schema = Map.of();

    RecordFormPanel(MainFrame frame) {
        super(new BorderLayout(0, 8));
        this.frame = frame;
        setBackground(Theme.PANEL);
        setBorder(Theme.cardBorder());

        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setOpaque(false);
        header.add(Theme.sectionHeader("Record"), BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);

        body.setBackground(Theme.PANEL);
        extraPanel.setLayout(new BoxLayout(extraPanel, BoxLayout.Y_AXIS));
        extraPanel.setOpaque(false);
        JPanel bodyHolder = new JPanel(new BorderLayout());
        bodyHolder.setBackground(Theme.PANEL);
        bodyHolder.add(body, BorderLayout.NORTH);
        JScrollPane scroll = Theme.scroll(bodyHolder);
        scroll.setBorder(null);

        JButton add = Theme.button("Add", true);
        add.setToolTipText("Add a new record with this key and the filled-in fields");
        add.addActionListener(e -> withKey(key -> withValues(v -> frame.addRecord(key, v))));
        JButton get = Theme.button("Get");
        get.setToolTipText("Read the record with this key");
        get.addActionListener(e -> withKey(frame::getRecord));
        JButton update = Theme.button("Update");
        update.setToolTipText("Update only the filled-in fields of the record with this key");
        update.addActionListener(e -> withKey(key -> withValues(v -> frame.updateRecord(key, v))));
        JButton delete = Theme.button("Delete");
        delete.setToolTipText("Delete the record with this key");
        delete.addActionListener(e -> withKey(this::confirmDelete));
        JButton clear = Theme.button("Clear form");
        clear.addActionListener(e -> clear());

        JPanel grid = new JPanel(new GridLayout(2, 2, 8, 8));
        grid.setOpaque(false);
        grid.add(add);
        grid.add(get);
        grid.add(update);
        grid.add(delete);
        JLabel hint = Theme.muted("<html>Empty fields are not sent · Update sends only filled fields."
                + "<br>Change a field's type to send a wrong type on purpose.</html>");
        hint.setBorder(new EmptyBorder(4, 0, 0, 0));
        JPanel south = new JPanel(new BorderLayout(0, 8));
        south.setOpaque(false);
        south.add(grid, BorderLayout.NORTH);
        south.add(clear, BorderLayout.CENTER);
        south.add(hint, BorderLayout.SOUTH);
        actions.addAll(List.of(add, get, update, delete, clear, keyField));

        add(header, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        rebuild(null, Map.of());
    }

    /** Shows the inputs for a collection. Typed values are kept if the collection and schema did not change. */
    void setCollection(String name, Map<String, String> newSchema) {
        if (!Objects.equals(name, collection) || !newSchema.equals(schema)) {
            rebuild(name, newSchema);
        }
    }

    private void rebuild(String name, Map<String, String> newSchema) {
        collection = name;
        schema = new LinkedHashMap<>(newSchema);
        subtitle.setText(name == null ? "Select a collection first." : "in \"" + name + "\"");
        actions.forEach(c -> c.setEnabled(name != null));

        schemaFields.clear();
        extraRows.clear();
        extraPanel.removeAll();
        body.removeAll();
        keyField.setText("");

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.insets = new Insets(0, 0, 8, 8);
        c.anchor = GridBagConstraints.WEST;
        addRow(c, "key", Theme.BOLD, keyField, null);
        for (Map.Entry<String, String> e : schema.entrySet()) {
            FieldInput input = new FieldInput(e.getValue());
            schemaFields.put(e.getKey(), input);
            addRow(c, e.getKey(), Theme.BASE, input.text, input.sendAs);
        }

        if (name != null) {
            c.gridx = 0;
            c.gridwidth = 3;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(12, 0, 6, 0);
            JLabel extraTitle = Theme.muted("Extra fields (not in schema) — to test validation");
            extraTitle.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER));
            body.add(extraTitle, c);
            c.gridy++;
            c.insets = new Insets(0, 0, 6, 0);
            body.add(extraPanel, c);
            c.gridy++;
            JButton addExtra = Theme.button("+ Extra field");
            addExtra.addActionListener(e -> addExtraRow("", "", FieldTypes.STRING));
            c.fill = GridBagConstraints.NONE;
            body.add(addExtra, c);
        }
        body.revalidate();
        body.repaint();
    }

    /** Fills the form with a record; fields that are not in the schema go to extra rows. */
    void fill(String key, Map<String, Object> value) {
        keyField.setText(key);
        schemaFields.forEach((field, input) -> {
            input.text.setText(value.containsKey(field) ? String.valueOf(value.get(field)) : "");
            input.sendAs.setSelectedItem(input.schemaType);
        });
        clearExtras();
        value.forEach((field, v) -> {
            if (!schemaFields.containsKey(field)) {
                String type = FieldTypes.nameOf(v);
                addExtraRow(field, String.valueOf(v), Arrays.asList(FieldTypes.ALL).contains(type) ? type : FieldTypes.STRING);
            }
        });
    }

    void clear() {
        keyField.setText("");
        schemaFields.values().forEach(f -> {
            f.text.setText("");
            f.sendAs.setSelectedItem(f.schemaType);
        });
        clearExtras();
    }

    /**
     * The non-empty schema inputs, plus every extra row that has a field name, each converted to
     * its "send as" type.
     *
     * @throws IllegalArgumentException if a value cannot be converted to its type
     */
    private Map<String, Object> values() {
        Map<String, Object> values = new LinkedHashMap<>();
        schemaFields.forEach((field, input) -> {
            if (!input.text.getText().isEmpty()) {
                values.put(field, convert(field, input.selectedType(), input.text.getText()));
            }
        });
        for (ExtraRow row : extraRows) {
            String field = row.name.getText().trim();
            if (!field.isEmpty()) {
                values.put(field, convert(field, (String) row.type.getSelectedItem(), row.value.getText()));
            }
        }
        return values;
    }

    private static Object convert(String field, String type, String text) {
        try {
            return FieldTypes.parse(type, text);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Field \"" + field + "\": " + e.getMessage());
        }
    }

    private void withKey(Consumer<String> action) {
        String key = keyField.getText().trim();
        if (key.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a key.", "Record", JOptionPane.WARNING_MESSAGE);
            keyField.requestFocusInWindow();
            return;
        }
        action.accept(key);
    }

    /** Runs the action with the form's values, or shows why a value could not be converted. */
    private void withValues(Consumer<Map<String, Object>> action) {
        Map<String, Object> values;
        try {
            values = values();
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage()
                            + "\n\nFix the value, or change the field's type to send it as a different type.",
                    "Record", JOptionPane.WARNING_MESSAGE);
            return;
        }
        action.accept(values);
    }

    private void confirmDelete(String key) {
        int answer = JOptionPane.showConfirmDialog(this, "Delete record " + key + " from \"" + collection + "\"?",
                "Delete record", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer == JOptionPane.OK_OPTION) {
            frame.deleteRecord(key);
        }
    }

    /** Adds a label / input / optional type row; the key row spans the type column. */
    private void addRow(GridBagConstraints c, String label, Font font, JComponent input, JComponent type) {
        JLabel l = Theme.label(label);
        l.setFont(font);
        c.gridwidth = 1;
        c.gridx = 0;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        body.add(l, c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        if (type == null) {
            c.gridwidth = 2;
            c.insets = new Insets(0, 0, 8, 0);
            body.add(input, c);
        } else {
            body.add(input, c);
            c.gridx = 2;
            c.weightx = 0;
            c.insets = new Insets(0, 0, 8, 0);
            body.add(type, c);
        }
        c.gridwidth = 1;
        c.insets = new Insets(0, 0, 8, 8);
        c.gridy++;
    }

    private void addExtraRow(String field, String value, String type) {
        ExtraRow row = new ExtraRow(field, value, type);
        extraRows.add(row);
        extraPanel.add(row);
        body.revalidate();
        body.repaint();
    }

    private void clearExtras() {
        extraRows.clear();
        extraPanel.removeAll();
        body.revalidate();
        body.repaint();
    }

    private static JComboBox<String> typeCombo() {
        JComboBox<String> combo = new JComboBox<>(FieldTypes.ALL);
        combo.setPreferredSize(new Dimension(92, 28));
        combo.setToolTipText("Send this value as (Boolean: true / false)");
        combo.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        return combo;
    }

    /** A schema field's input and its "send as" type, shown in red when it differs from the schema. */
    private static final class FieldInput {
        final String schemaType;
        final JTextField text = Theme.field();
        final JComboBox<String> sendAs = typeCombo();

        FieldInput(String schemaType) {
            this.schemaType = schemaType;
            text.setToolTipText("Schema type: " + schemaType);
            if (!Arrays.asList(FieldTypes.ALL).contains(schemaType)) {
                sendAs.addItem(schemaType);
            }
            sendAs.setSelectedItem(schemaType);
            sendAs.addActionListener(e -> sendAs.setForeground(
                    schemaType.equals(sendAs.getSelectedItem()) ? Theme.TEXT : Theme.ERROR));
        }

        String selectedType() {
            return (String) sendAs.getSelectedItem();
        }
    }

    private final class ExtraRow extends JPanel {
        final JTextField name = Theme.field();
        final JTextField value = Theme.field();
        final JComboBox<String> type = typeCombo();

        ExtraRow(String field, String text, String fieldType) {
            super(new GridBagLayout());
            setOpaque(false);
            name.setText(field);
            name.setColumns(5);
            name.setToolTipText("Field name");
            name.setForeground(Theme.ERROR);
            value.setText(text);
            value.setColumns(5);
            value.setToolTipText("Value");
            type.setSelectedItem(fieldType);
            JButton remove = Theme.button("✕");
            remove.addActionListener(e -> {
                extraRows.remove(this);
                extraPanel.remove(this);
                body.revalidate();
                body.repaint();
            });
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(0, 0, 6, 6);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1;
            add(name, c);
            add(value, c);
            c.weightx = 0;
            add(type, c);
            c.insets = new Insets(0, 0, 6, 0);
            add(remove, c);
        }
    }
}
