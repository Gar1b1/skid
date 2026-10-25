package com.matmon.skidb.ui;

import com.matmon.skidb.api.IndexType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The "Indexes" tab: create an index on a field, and see the content of every index next to what
 * it should hold according to the records. Entries that are out of sync are shown in red.
 */
final class IndexesPanel extends JPanel {

    private final JLabel subtitle = Theme.muted(" ");
    private final JComboBox<String> fieldCombo = new JComboBox<>();
    private final JComboBox<IndexType> typeCombo = new JComboBox<>(IndexType.values());
    private final JButton create = Theme.button("Create index", true);
    private final JPanel list = new JPanel();

    IndexesPanel(MainFrame frame) {
        super(new BorderLayout(0, 8));
        setBackground(Theme.PANEL);
        setBorder(Theme.cardBorder());

        fieldCombo.setPreferredSize(new Dimension(180, 30));
        fieldCombo.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        typeCombo.setPreferredSize(new Dimension(90, 30));
        typeCombo.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        typeCombo.setToolTipText("HASH: fast \"=\" lookups · TREE: sorted, also answers range conditions");
        create.addActionListener(e -> {
            String field = (String) fieldCombo.getSelectedItem();
            if (field != null) {
                frame.createIndex(field, (IndexType) typeCombo.getSelectedItem());
            }
        });
        JPanel createRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        createRow.setOpaque(false);
        createRow.add(Theme.label("Create an index on"));
        createRow.add(fieldCombo);
        createRow.add(Theme.label("of type"));
        createRow.add(typeCombo);
        createRow.add(create);

        JPanel header = new JPanel(new BorderLayout(0, 8));
        header.setOpaque(false);
        header.add(Theme.sectionHeader("Indexes"), BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.CENTER);
        header.add(createRow, BorderLayout.SOUTH);

        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(Theme.PANEL);
        JPanel holder = new JPanel(new BorderLayout());
        holder.setBackground(Theme.PANEL);
        holder.add(list, BorderLayout.NORTH);
        JScrollPane scroll = Theme.scroll(holder);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        add(header, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        clear();
    }

    void clear() {
        subtitle.setText("Select a collection first.");
        fieldCombo.removeAllItems();
        fieldCombo.setEnabled(false);
        typeCombo.setEnabled(false);
        create.setEnabled(false);
        list.removeAll();
        list.revalidate();
        list.repaint();
    }

    void show(Map<String, String> schema, Collection<String> indexed, List<IndexCheck> checks) {
        fieldCombo.removeAllItems();
        schema.keySet().stream().filter(f -> !indexed.contains(f)).forEach(fieldCombo::addItem);
        fieldCombo.setEnabled(fieldCombo.getItemCount() > 0);
        typeCombo.setEnabled(fieldCombo.getItemCount() > 0);
        create.setEnabled(fieldCombo.getItemCount() > 0);

        long bad = checks.stream().filter(c -> !c.ok()).count();
        subtitle.setText(checks.isEmpty() ? "No indexes yet. Queries on this collection will scan every record."
                : checks.size() + (checks.size() == 1 ? " index" : " indexes")
                + (bad == 0 ? " · all in sync with the records" : " · " + bad + " out of sync"));
        subtitle.setForeground(bad == 0 ? Theme.MUTED : Theme.ERROR);

        list.removeAll();
        for (IndexCheck check : checks) {
            list.add(view(check));
        }
        list.revalidate();
        list.repaint();
    }

    private static JComponent view(IndexCheck check) {
        JLabel title = new JLabel(check.field + "  (" + check.type + ")  ·  " + check.indexType);
        title.setFont(Theme.BOLD);
        title.setForeground(Theme.TEXT);
        boolean rowsOk = check.rows.stream().allMatch(IndexCheck.Row::ok);
        JLabel status = new JLabel(check.ok() ? "✔ in sync" : check.error != null ? "✖ could not be read"
                : rowsOk ? "✖ wrong data structure" : "✖ out of sync");
        status.setForeground(check.ok() ? Theme.SUCCESS : Theme.ERROR);
        JPanel head = new JPanel(new BorderLayout(12, 2));
        head.setOpaque(false);
        head.add(title, BorderLayout.WEST);
        head.add(status, BorderLayout.CENTER);
        String detail = check.error != null ? check.error : check.structureProblem;
        if (detail != null) {
            JLabel d = new JLabel("<html>" + detail.replace("&", "&amp;").replace("<", "&lt;") + "</html>");
            d.setForeground(Theme.ERROR);
            head.add(d, BorderLayout.SOUTH);
        }

        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(10, 0, 6, 0));
        p.add(head, BorderLayout.NORTH);
        if (check.error == null) {
            p.add(table(check), BorderLayout.CENTER);
        }
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height));
        return p;
    }

    private static JComponent table(IndexCheck check) {
        boolean allOk = check.rows.stream().allMatch(IndexCheck.Row::ok);
        String[] columns = allOk ? new String[]{"value", "keys"} : new String[]{"value", "keys in your index", "expected keys"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (IndexCheck.Row row : check.rows) {
            String actual = "{" + String.join(", ", row.actual()) + "}";
            model.addRow(allOk ? new Object[]{row.value(), actual}
                    : new Object[]{row.value(), actual, "{" + String.join(", ", row.expected()) + "}"});
        }
        JTable table = new JTable(model);
        table.setRowHeight(24);
        table.setFocusable(false);
        table.setRowSelectionAllowed(false);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setFont(Theme.BOLD);
        table.getColumnModel().getColumn(0).setPreferredWidth(160);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus, int row, int col) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(t, value, false, false, row, col);
                c.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                c.setForeground(check.rows.get(row).ok() ? Theme.TEXT : Theme.ERROR);
                c.setFont(col == 0 ? Theme.BOLD : Theme.BASE);
                return c;
            }
        });

        // show up to 12 rows, scroll the rest
        JScrollPane scroll = Theme.scroll(table);
        int header = table.getTableHeader().getPreferredSize().height;
        scroll.setPreferredSize(new Dimension(100, header + Math.max(1, Math.min(check.rows.size(), 12)) * table.getRowHeight() + 4));
        return scroll;
    }
}
