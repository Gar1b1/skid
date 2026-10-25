package com.matmon.skidb.ui;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Timestamped log of every operation sent to the student's adapter. */
final class LogPanel extends JPanel {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final JTextPane pane = new JTextPane();

    LogPanel() {
        super(new BorderLayout(0, 6));
        setBackground(Theme.BG);

        pane.setEditable(false);
        pane.setFont(Theme.MONO);
        pane.setBackground(Theme.PANEL);

        JButton clear = Theme.button("Clear log");
        clear.addActionListener(e -> pane.setText(""));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(Theme.sectionHeader("Operation log"), BorderLayout.CENTER);
        top.add(clear, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);
        add(Theme.scroll(pane), BorderLayout.CENTER);
    }

    void success(String message) {
        append("✔ ", Theme.SUCCESS, message);
    }

    void error(String message) {
        append("✖ ", Theme.ERROR, message);
    }

    void info(String message) {
        append("• ", Theme.MUTED, message);
    }

    private void append(String mark, Color color, String message) {
        StyledDocument doc = pane.getStyledDocument();
        try {
            doc.insertString(doc.getLength(), "[" + LocalTime.now().format(TIME) + "] ", attrs(Theme.MUTED));
            doc.insertString(doc.getLength(), mark, attrs(color));
            doc.insertString(doc.getLength(), message + "\n", attrs(color == Theme.MUTED ? Theme.TEXT : color));
        } catch (BadLocationException ignored) {
            // cannot happen when appending at the end
        }
        pane.setCaretPosition(doc.getLength());
    }

    private static SimpleAttributeSet attrs(Color color) {
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setForeground(a, color);
        return a;
    }
}
