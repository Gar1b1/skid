package com.matmon.skidb.ui;

import com.matmon.skidb.api.Condition;
import com.matmon.skidb.api.Condition.Operator;
import com.matmon.skidb.api.IndexType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * The "Query" tab: build conditions (field, operator, value - all must match), run them against
 * the student's query(), and see the results with timing and a check against a full scan.
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
    private Map<String, IndexType> indexes = Map.of();

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
        setCollection(null, Map.of(), Map.of());
    }

    /** Shows the conditions for a collection. Conditions are kept if the collection and schema did not change. */
    void setCollection(String name, Map<String, String> newSchema, Map<String, IndexType> newIndexes) {
        boolean changed = !Objects.equals(name, collection) || !newSchema.equals(schema);
        collection = name;
        schema = new LinkedHashMap<>(newSchema);
        indexes = new LinkedHashMap<>(newIndexes);
        subtitle.setText(name == null ? "Select a collection first."
                : "Find the records in \"" + name + "\" that satisfy ALL the conditions. Ranges use a TREE index.");
        addCondition.setEnabled(name != null && !schema.isEmpty());
        run.setEnabled(name != null && !schema.isEmpty());
        if (changed) {
            conditions.clear();
            conditionsPanel.removeAll();
            status.setText(" ");
            results.clear();
            if (name != null && !schema.isEmpty()) {
                addCondition(indexes.isEmpty() ? null : indexes.keySet().iterator().next());
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
        List<Condition> list = new ArrayList<>();
        for (ConditionRow row : conditions) {
            try {
                list.add(row.toCondition());
            } catch (IllegalArgumentException e) {
                warn(e.getMessage());
                return;
            }
        }
        if (list.isEmpty()) {
            warn("Add at least one condition.");
            return;
        }
        frame.runQuery(list);
    }

    private void warn(String message) {
        JOptionPane.showMessageDialog(this, message, "Query", JOptionPane.WARNING_MESSAGE);
    }

    /** field [combo]  operator [combo]  value [text]  (and [text])  as [type]  ✕, with the index hint below */
    private final class ConditionRow extends JPanel {
        final JComboBox<String> field = new JComboBox<>(schema.keySet().toArray(new String[0]));
        final JComboBox<Operator> operator = new JComboBox<>(Operator.values());
        final JTextField value = Theme.field();
        final JLabel and = Theme.muted("and");
        final JTextField to = Theme.field();
        final JComboBox<String> type = new JComboBox<>(FieldTypes.ALL);
        final JLabel indexedLabel = new JLabel();

        ConditionRow(String initialField) {
            super(new BorderLayout(0, 2));
            setOpaque(false);
            setBorder(new EmptyBorder(0, 0, 6, 0));
            JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            controls.setOpaque(false);
            indexedLabel.setBorder(new EmptyBorder(0, 8, 0, 0));
            style(field, 130);
            style(operator, 90);
            style(type, 88);
            value.setColumns(8);
            to.setColumns(8);
            type.setToolTipText("Send the value(s) as");
            field.addActionListener(e -> onFieldChanged());
            operator.setToolTipText("Range operators are not used on Boolean fields");
            operator.addActionListener(e -> onOperatorChanged());
            type.addActionListener(e -> type.setForeground(
                    Objects.equals(type.getSelectedItem(), schema.get((String) field.getSelectedItem())) ? Theme.TEXT : Theme.ERROR));
            value.addActionListener(e -> runQuery());
            to.addActionListener(e -> runQuery());
            JButton remove = Theme.button("✕");
            remove.addActionListener(e -> {
                conditions.remove(this);
                conditionsPanel.remove(this);
                conditionsPanel.revalidate();
                conditionsPanel.repaint();
            });
            controls.add(field);
            controls.add(operator);
            controls.add(value);
            controls.add(and);
            controls.add(to);
            controls.add(Theme.muted("as"));
            controls.add(type);
            controls.add(remove);
            add(controls, BorderLayout.NORTH);
            add(indexedLabel, BorderLayout.SOUTH);
            field.setSelectedItem(initialField);
            operator.setSelectedItem(Operator.EQUALS);
            onFieldChanged();
            onOperatorChanged();
        }

        private void onFieldChanged() {
            String t = schema.getOrDefault((String) field.getSelectedItem(), FieldTypes.STRING);
            type.setSelectedItem(t);
            if (FieldTypes.BOOLEAN.equals(t)) {
                operator.setSelectedItem(Operator.EQUALS);
            }
            operator.setEnabled(!FieldTypes.BOOLEAN.equals(t));
            refreshIndexed();
        }

        private void onOperatorChanged() {
            boolean between = operator.getSelectedItem() == Operator.BETWEEN;
            and.setVisible(between);
            to.setVisible(between);
            refreshIndexed();
            revalidate();
        }

        Condition toCondition() {
            String f = (String) field.getSelectedItem();
            Operator op = (Operator) operator.getSelectedItem();
            String t = (String) type.getSelectedItem();
            Object from = parse(f, t, value.getText());
            Object upper = op == Operator.BETWEEN ? parse(f, t, to.getText()) : null;
            return new Condition(f, op, from, upper);
        }

        private Object parse(String f, String t, String text) {
            try {
                return FieldTypes.parse(t, text);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Field \"" + f + "\": " + e.getMessage());
            }
        }

        /** Shows which index, if any, can answer this condition. */
        void refreshIndexed() {
            IndexType index = indexes.get((String) field.getSelectedItem());
            boolean range = operator.getSelectedItem() != Operator.EQUALS;
            if (index == null) {
                indexedLabel.setText("not indexed");
                indexedLabel.setForeground(Theme.MUTED);
            } else if (range && index == IndexType.HASH) {
                indexedLabel.setText("HASH index — can't help a range");
                indexedLabel.setForeground(Theme.MUTED);
            } else {
                indexedLabel.setText(index + " index");
                indexedLabel.setForeground(Theme.ACCENT);
            }
        }

        private static void style(JComboBox<?> combo, int width) {
            combo.setPreferredSize(new Dimension(width, 30));
            combo.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        }
    }
}
