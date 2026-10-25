package com.matmon.skidb.ui;

import com.matmon.skidb.api.IndexType;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.stream.Collectors;

/** The "Records" tab: the selected collection's schema and a table of all its records. */
final class RecordsPanel extends JPanel {

    private final RecordTable table;
    private final JLabel title = new JLabel();
    private final JLabel schemaLabel = Theme.muted(" ");
    private final JLabel countLabel = Theme.muted(" ");
    private final JButton generate = Theme.button("Generate data…");

    RecordsPanel(MainFrame frame) {
        super(new BorderLayout(0, 8));
        setBackground(Theme.PANEL);
        setBorder(Theme.cardBorder());
        table = new RecordTable(frame::onRecordSelected);

        title.setFont(Theme.SECTION);
        title.setForeground(Theme.ACCENT);
        generate.setToolTipText("Add many random records, to measure query speed with and without an index");
        generate.addActionListener(e -> frame.generateRecords());

        JPanel east = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        east.setOpaque(false);
        east.add(countLabel);
        east.add(generate);

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(title, BorderLayout.WEST);
        titleRow.add(east, BorderLayout.EAST);
        titleRow.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.ACCENT_DARK),
                BorderFactory.createEmptyBorder(0, 0, 4, 0)));
        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setOpaque(false);
        header.add(titleRow, BorderLayout.NORTH);
        header.add(schemaLabel, BorderLayout.SOUTH);

        add(header, BorderLayout.NORTH);
        add(table, BorderLayout.CENTER);
        clear();
    }

    void clear() {
        title.setText("No collection selected");
        schemaLabel.setText("Create a collection or select one from the list.");
        countLabel.setText(" ");
        generate.setEnabled(false);
        table.clear();
    }

    void show(String name, Map<String, String> schema, Map<String, IndexType> indexed, String partitionField,
              Map<String, Map<String, Object>> records) {
        title.setText("Collection: " + name);
        schemaLabel.setText(schemaText(schema, indexed, partitionField));
        countLabel.setText(records.size() + (records.size() == 1 ? " record" : " records"));
        generate.setEnabled(!schema.isEmpty());
        table.show(schema, records);
    }

    void select(String key) {
        table.select(key);
    }

    /** "Schema: firstName : String (HASH index) · at : DateTime (partitions)", with the notes in the accent color. */
    static String schemaText(Map<String, String> schema, Map<String, IndexType> indexed, String partitionField) {
        if (schema.isEmpty()) {
            return "Schema: (no fields)";
        }
        String accent = String.format("#%06X", Theme.ACCENT.getRGB() & 0xFFFFFF);
        return "<html>Schema:&nbsp; " + schema.entrySet().stream()
                .map(e -> e.getKey() + " : " + e.getValue()
                        + (indexed.containsKey(e.getKey()) ? " <font color='" + accent + "'>(" + indexed.get(e.getKey()) + " index)</font>" : "")
                        + (e.getKey().equals(partitionField) ? " <font color='" + accent + "'>(partitions by hour)</font>" : ""))
                .collect(Collectors.joining(" &nbsp;·&nbsp; ")) + "</html>";
    }
}
