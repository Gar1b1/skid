package com.matmon.skidb.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/** Center: the selected collection's schema and a table of all its records. */
final class RecordsPanel extends JPanel {

    private static final String NOT_IN_SCHEMA = " ⚠";

    private final DefaultTableModel model = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JLabel title = new JLabel();
    private final JLabel schemaLabel = Theme.muted(" ");
    private final JLabel countLabel = Theme.muted(" ");
    private boolean updating;

    RecordsPanel(MainFrame frame) {
        super(new BorderLayout(0, 8));
        setBackground(Theme.PANEL);
        setBorder(Theme.cardBorder());

        title.setFont(Theme.SECTION);
        title.setForeground(Theme.ACCENT);

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
                c.setForeground(t.getColumnName(col).endsWith(NOT_IN_SCHEMA) ? Theme.ERROR : Theme.TEXT);
                return c;
            }
        });
        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (!updating && !e.getValueIsAdjusting() && row >= 0) {
                frame.onRecordSelected((String) model.getValueAt(row, 0));
            }
        });

        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setOpaque(false);
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(title, BorderLayout.WEST);
        titleRow.add(countLabel, BorderLayout.EAST);
        titleRow.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.ACCENT_DARK),
                BorderFactory.createEmptyBorder(0, 0, 4, 0)));
        header.add(titleRow, BorderLayout.NORTH);
        header.add(schemaLabel, BorderLayout.SOUTH);

        add(header, BorderLayout.NORTH);
        add(Theme.scroll(table), BorderLayout.CENTER);
        clear();
    }

    void clear() {
        title.setText("No collection selected");
        schemaLabel.setText("Create a collection or select one from the list.");
        countLabel.setText(" ");
        setTable(new Object[]{"key"}, new Object[0][]);
    }

    /**
     * Shows a collection. Columns are the key, every schema field, and then any field that
     * appears in a record but not in the schema (marked in red - the schema was not enforced).
     */
    void show(String name, Map<String, String> schema, Map<String, Map<String, Object>> records) {
        title.setText("Collection: " + name);
        schemaLabel.setText(schema.isEmpty() ? "Schema: (no fields)" : "Schema:  " + schema.entrySet().stream()
                .map(e -> e.getKey() + " : " + e.getValue())
                .collect(Collectors.joining("   ·   ")));
        countLabel.setText(records.size() + (records.size() == 1 ? " record" : " records"));

        List<String> fields = new ArrayList<>(schema.keySet());
        for (Map<String, Object> value : records.values()) {
            if (value != null) {
                value.keySet().stream().filter(f -> !fields.contains(f)).forEach(fields::add);
            }
        }
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
