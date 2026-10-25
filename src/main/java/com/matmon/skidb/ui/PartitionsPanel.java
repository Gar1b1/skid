package com.matmon.skidb.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * The "Partitions" tab: the collection's hour partitions next to what they should hold according
 * to the records, and which partitions the last query read compared with the ones it needed.
 */
final class PartitionsPanel extends JPanel {

    private final JLabel subtitle = Theme.muted(" ");
    private final JLabel lastQuery = new JLabel(" ");
    private final JPanel content = new JPanel(new BorderLayout());

    PartitionsPanel() {
        super(new BorderLayout(0, 8));
        setBackground(Theme.PANEL);
        setBorder(Theme.cardBorder());

        lastQuery.setBorder(new EmptyBorder(2, 0, 4, 0));
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        for (JComponent c : java.util.List.of(Theme.sectionHeader("Partitions"), subtitle, lastQuery)) {
            c.setAlignmentX(LEFT_ALIGNMENT);
            header.add(c);
            header.add(Box.createVerticalStrut(6));
        }

        content.setBackground(Theme.PANEL);
        JScrollPane scroll = Theme.scroll(content);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        add(header, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        clear();
    }

    void clear() {
        subtitle.setText("Select a collection first.");
        subtitle.setForeground(Theme.MUTED);
        lastQuery.setText(" ");
        content.removeAll();
        content.revalidate();
        content.repaint();
    }

    /** @param check the partitions compared with the records, or null if the collection is not partitioned */
    void show(String partitionField, IndexCheck check) {
        content.removeAll();
        if (partitionField == null || check == null) {
            subtitle.setText("This collection is not partitioned. Create a collection with \"Partition by hour\" to use partitions.");
            subtitle.setForeground(Theme.MUTED);
        } else {
            long count = check.rows.stream().filter(r -> !r.actual().isEmpty() || !r.expected().isEmpty()).count();
            subtitle.setText("Partitioned by the hour of \"" + partitionField + "\" · " + count
                    + (count == 1 ? " partition" : " partitions") + (check.ok() ? " · all records in the right partition" : " · out of sync"));
            subtitle.setForeground(check.ok() ? Theme.MUTED : Theme.ERROR);
            JPanel holder = new JPanel(new BorderLayout());
            holder.setBackground(Theme.PANEL);
            holder.add(IndexesPanel.view(check, partitionField + "  ·  one partition per hour"), BorderLayout.NORTH);
            content.add(holder, BorderLayout.NORTH);
        }
        content.revalidate();
        content.repaint();
    }

    /** Shows how the last query used the partitions; {@code ok} colors the line. */
    void showLastQuery(String html, boolean ok) {
        lastQuery.setText(html);
        lastQuery.setForeground(ok ? Theme.SUCCESS : Theme.ACCENT);
    }
}
