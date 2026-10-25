package com.matmon.skidb.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * The "Query" tab: build conditions (field = value, all must match), run them against the
 * student's query(), and see the results with timing and a check against a full scan.
 */
final class QueryPanel extends JPanel {

    private final MainFrame frame;
    private final JLabel subtitle = Theme.muted(" ");
    private final JPanel conditionsPanel = new JPanel();
    private final List<ConditionRow> conditions = new ArrayList<>();
    private final JButton addCondition = Theme.button("+ Condition");
    private final JButton run = Theme.button("Run query", true);
    private final JLabel status = new JLabel(" ");
    private final RecordTable results;

    private String collection;
    private Map<String, String> schema = Map.of();
    private Collection<String> indexed = List.of();

    QueryPanel(MainFrame frame) {
        super(new BorderLayout(0, 8));
        this.frame = frame;
        setBackground(Theme.PANEL);
        setBorder(Theme.cardBorder());
        results = new RecordTable(frame::onRecordSelected);

        conditionsPanel.setLayout(new BoxLayout(conditionsPanel, BoxLayout.Y_AXIS));
        conditionsPanel.setOpaque(false);
        addCondition.addActionListener(e -> addCondition(null));
        run.addActionListener(e -> runQuery());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(addCondition);
        buttons.add(run);

        status.setBorder(new EmptyBorder(4, 0, 0, 0));
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        for (JComponent c : List.of(Theme.sectionHeader("Query"), subtitle, conditionsPanel, buttons, status)) {
            c.setAlignmentX(LEFT_ALIGNMENT);
            top.add(c);
            top.add(Box.createVerticalStrut(6));
        }

        add(top, BorderLayout.NORTH);
        add(results, BorderLayout.CENTER);
        setCollection(null, Map.of(), List.of());
    }

    /** Shows the conditions for a collection. Conditions are kept if the collection and schema did not change. */
    void setCollection(String name, Map<String, String> newSchema, Collection<String> newIndexed) {
        boolean changed = !Objects.equals(name, collection) || !newSchema.equals(schema);
        collection = name;
        schema = new LinkedHashMap<>(newSchema);
        indexed = new ArrayList<>(newIndexed);
        subtitle.setText(name == null ? "Select a collection first."
                : "Find the records in \"" + name + "\" whose fields equal ALL the conditions.");
        addCondition.setEnabled(name != null && !schema.isEmpty());
        run.setEnabled(name != null && !schema.isEmpty());
        if (changed) {
            conditions.clear();
            conditionsPanel.removeAll();
            status.setText(" ");
            results.clear();
            if (name != null && !schema.isEmpty()) {
                addCondition(indexed.isEmpty() ? null : indexed.iterator().next());
            }
        }
        conditions.forEach(ConditionRow::refreshIndexed);
        conditionsPanel.revalidate();
        conditionsPanel.repaint();
    }

    /** Shows the result of a query; {@code ok} colors the status line. */
    void showResult(String statusHtml, boolean ok, Map<String, Map<String, Object>> records) {
        status.setText(statusHtml);
        status.setForeground(ok ? Theme.SUCCESS : Theme.ERROR);
        results.show(schema, records);
    }

    void showFailure(String message) {
        status.setText("✖ " + message);
        status.setForeground(Theme.ERROR);
        results.clear();
    }

    private void addCondition(String field) {
        ConditionRow row = new ConditionRow(field != null ? field : schema.keySet().iterator().next());
        conditions.add(row);
        conditionsPanel.add(row);
        conditionsPanel.revalidate();
        conditionsPanel.repaint();
    }

    private void runQuery() {
        Map<String, Object> values = new LinkedHashMap<>();
        for (ConditionRow row : conditions) {
            String field = (String) row.field.getSelectedItem();
            if (values.containsKey(field)) {
                warn("Field \"" + field + "\" appears in more than one condition.");
                return;
            }
            try {
                values.put(field, FieldTypes.parse((String) row.type.getSelectedItem(), row.value.getText()));
            } catch (IllegalArgumentException e) {
                warn("Field \"" + field + "\": " + e.getMessage());
                return;
            }
        }
        if (values.isEmpty()) {
            warn("Add at least one condition.");
            return;
        }
        frame.runQuery(values);
    }

    private void warn(String message) {
        JOptionPane.showMessageDialog(this, message, "Query", JOptionPane.WARNING_MESSAGE);
    }

    /** field [combo]  =  value [text]  as [type]  (indexed)  ✕ */
    private final class ConditionRow extends JPanel {
        final JComboBox<String> field = new JComboBox<>(schema.keySet().toArray(new String[0]));
        final JTextField value = Theme.field();
        final JComboBox<String> type = new JComboBox<>(FieldTypes.ALL);
        final JLabel indexedLabel = new JLabel();

        ConditionRow(String initialField) {
            super(new FlowLayout(FlowLayout.LEFT, 6, 0));
            setOpaque(false);
            setBorder(new EmptyBorder(0, 0, 6, 0));
            field.setPreferredSize(new Dimension(150, 30));
            field.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
            value.setColumns(14);
            type.setPreferredSize(new Dimension(92, 30));
            type.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
            type.setToolTipText("Send this value as");
            field.addActionListener(e -> {
                type.setSelectedItem(schema.getOrDefault((String) field.getSelectedItem(), FieldTypes.STRING));
                refreshIndexed();
            });
            type.addActionListener(e -> type.setForeground(
                    Objects.equals(type.getSelectedItem(), schema.get((String) field.getSelectedItem())) ? Theme.TEXT : Theme.ERROR));
            field.setSelectedItem(initialField);
            type.setSelectedItem(schema.getOrDefault(initialField, FieldTypes.STRING));
            value.addActionListener(e -> runQuery());
            JButton remove = Theme.button("✕");
            remove.addActionListener(e -> {
                conditions.remove(this);
                conditionsPanel.remove(this);
                conditionsPanel.revalidate();
                conditionsPanel.repaint();
            });
            add(field);
            add(Theme.label("="));
            add(value);
            add(Theme.muted("as"));
            add(type);
            add(remove);
            add(indexedLabel);
            refreshIndexed();
        }

        void refreshIndexed() {
            boolean isIndexed = indexed.contains((String) field.getSelectedItem());
            indexedLabel.setText(isIndexed ? "indexed" : "not indexed");
            indexedLabel.setForeground(isIndexed ? Theme.ACCENT : Theme.MUTED);
        }
    }
}
