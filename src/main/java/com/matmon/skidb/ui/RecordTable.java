package com.matmon.skidb.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * A scrollable table of records: a key column, one column per schema field, then any field that
 * appears in a record but not in the schema. Fields outside the schema and values of the wrong
 * type are shown in red: the schema was not enforced.
 */
final class RecordTable extends JScrollPane {

    private static final String NOT_IN_SCHEMA = " ⚠";

    private final DefaultTableModel model = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private Map<String, String> shownSchema = Map.of();
    private List<String> shownFields = List.of();
    private boolean updating;

    /** @param onSelect called with the key of a row the user clicks */
    RecordTable(Consumer<String> onSelect) {
        setViewportView(table);
        getViewport().setBackground(Theme.PANEL);
        setBackground(Theme.PANEL);

        table.setRowHeight(26);
        table.setShowGrid(true);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setFont(Theme.BOLD);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus, int row, int col) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(t, value, selected, false, row, col);
                c.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                c.setFont(col == 0 ? Theme.BOLD : Theme.BASE);
                c.setToolTipText(null);
                boolean bad = t.getColumnName(col).endsWith(NOT_IN_SCHEMA);
                if (col > 0 && value != null) {
                    String expected = shownSchema.get(shownFields.get(col - 1));
                    c.setToolTipText(FieldTypes.nameOf(value) + (expected == null ? "" : "  (schema: " + expected + ")"));
                    if (expected != null && !FieldTypes.matches(expected, value)) {
                        bad = true;
                        c.setToolTipText("Wrong type: " + FieldTypes.nameOf(value) + ", schema says " + expected);
                    }
                }
                c.setForeground(bad ? Theme.ERROR : Theme.TEXT);
                return c;
            }
        });
        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (!updating && !e.getValueIsAdjusting() && row >= 0) {
                onSelect.accept((String) model.getValueAt(row, 0));
            }
        });
        clear();
    }

    void clear() {
        shownSchema = Map.of();
        shownFields = List.of();
        setTable(new Object[]{"key"}, new Object[0][]);
    }

    void show(Map<String, String> schema, Map<String, Map<String, Object>> records) {
        List<String> fields = new ArrayList<>(schema.keySet());
        for (Map<String, Object> value : records.values()) {
            if (value != null) {
                value.keySet().stream().filter(f -> !fields.contains(f)).forEach(fields::add);
            }
        }
        shownSchema = schema;
        shownFields = fields;
        Object[] columns = new Object[fields.size() + 1];
        columns[0] = "key";
        for (int i = 0; i < fields.size(); i++) {
            String f = fields.get(i);
            columns[i + 1] = schema.containsKey(f) ? f : f + NOT_IN_SCHEMA;
        }

        List<String> keys = sortedKeys(records.keySet());
        Object[][] rows = new Object[keys.size()][];
        for (int r = 0; r < keys.size(); r++) {
            Map<String, Object> value = records.get(keys.get(r));
            Object[] row = new Object[columns.length];
            row[0] = keys.get(r);
            for (int i = 0; i < fields.size(); i++) {
                row[i + 1] = value == null ? null : value.get(fields.get(i));
            }
            rows[r] = row;
        }
        setTable(columns, rows);
    }

    /** Selects the row with the given key, if it is shown. */
    void select(String key) {
        for (int r = 0; r < model.getRowCount(); r++) {
            if (Objects.equals(model.getValueAt(r, 0), key)) {
                updating = true;
                try {
                    table.setRowSelectionInterval(r, r);
                    table.scrollRectToVisible(table.getCellRect(r, 0, true));
                } finally {
                    updating = false;
                }
                return;
            }
        }
    }

    private void setTable(Object[] columns, Object[][] rows) {
        updating = true;
        try {
            model.setDataVector(rows, columns);
            if (table.getColumnCount() > 0) {
                table.getColumnModel().getColumn(0).setMaxWidth(120);
                table.getColumnModel().getColumn(0).setPreferredWidth(80);
            }
        } finally {
            updating = false;
        }
    }

    /** Keys sorted numerically when both are numbers ("2" before "10"), alphabetically otherwise. */
    static List<String> sortedKeys(Collection<String> keys) {
        List<String> sorted = new ArrayList<>(keys);
        sorted.sort((a, b) -> {
            Long x = asLong(a);
            Long y = asLong(b);
            if (x != null && y != null) {
                return x.compareTo(y);
            }
            if (x != null || y != null) {
                return x != null ? -1 : 1;
            }
            return String.valueOf(a).compareTo(String.valueOf(b));
        });
        return sorted;
    }

    private static Long asLong(String s) {
        try {
            return s == null ? null : Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
