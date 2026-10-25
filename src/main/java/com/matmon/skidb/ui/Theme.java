package com.matmon.skidb.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.*;

/** Colors, fonts and small factory helpers for the dark "Matmon" look. */
final class Theme {

    static final Color BG = new Color(0x1B1B1D);
    static final Color PANEL = new Color(0x242427);
    static final Color FIELD = new Color(0x2F2F33);
    static final Color BORDER = new Color(0x3D3D42);
    static final Color TEXT = new Color(0xECECEC);
    static final Color MUTED = new Color(0x9A9AA0);
    static final Color ACCENT = new Color(0xF08A24);
    static final Color ACCENT_DARK = new Color(0xB8651A);
    static final Color SUCCESS = new Color(0x5FCB7A);
    static final Color ERROR = new Color(0xF0605A);
    static final Color SELECTION = new Color(0x5A3A1A);

    static final Font BASE = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    static final Font BOLD = BASE.deriveFont(Font.BOLD);
    static final Font SECTION = BASE.deriveFont(Font.BOLD, 14f);
    static final Font TITLE = BASE.deriveFont(Font.BOLD, 24f);
    static final Font BRAND = BASE.deriveFont(Font.BOLD | Font.ITALIC, 20f);
    static final Font MONO = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    private Theme() {
    }

    /** Must run before any component is created. Metal is used so custom colors apply on every OS. */
    static void install() {
        try {
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        } catch (UnsupportedLookAndFeelException ignored) {
            // fall back to whatever is active
        }
        put("Panel.background", BG);
        put("OptionPane.background", PANEL);
        put("OptionPane.messageForeground", TEXT);
        put("Label.foreground", TEXT);
        put("TextField.background", FIELD);
        put("TextField.foreground", TEXT);
        put("TextField.caretForeground", ACCENT);
        put("TextField.inactiveForeground", MUTED);
        put("TextField.selectionBackground", SELECTION);
        put("TextField.selectionForeground", TEXT);
        put("TextPane.background", FIELD);
        put("TextPane.foreground", TEXT);
        put("List.background", PANEL);
        put("List.foreground", TEXT);
        put("List.selectionBackground", SELECTION);
        put("List.selectionForeground", TEXT);
        put("Table.background", PANEL);
        put("Table.foreground", TEXT);
        put("Table.gridColor", BORDER);
        put("Table.selectionBackground", SELECTION);
        put("Table.selectionForeground", TEXT);
        put("TableHeader.background", FIELD);
        put("TableHeader.foreground", ACCENT);
        put("ScrollPane.background", BG);
        put("Viewport.background", PANEL);
        put("ComboBox.background", FIELD);
        put("ComboBox.foreground", TEXT);
        put("ComboBox.selectionBackground", SELECTION);
        put("ComboBox.selectionForeground", TEXT);
        put("ComboBox.disabledBackground", FIELD);
        put("ComboBox.disabledForeground", MUTED);
        put("SplitPane.background", BG);
        put("SplitPaneDivider.draggingColor", ACCENT);
        put("ToolTip.background", FIELD);
        put("ToolTip.foreground", TEXT);
        put("ScrollBar.background", BG);
        put("ScrollBar.thumb", BORDER);
        put("ScrollBar.track", BG);
        UIManager.put("ScrollPane.border", BorderFactory.createLineBorder(BORDER));
        UIManager.put("TextField.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(4, 6, 4, 6)));
        UIManager.put("TableHeader.cellBorder", BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 1, BORDER), new EmptyBorder(4, 6, 4, 6)));
        for (String key : new String[]{"Label.font", "Button.font", "TextField.font", "List.font",
                "Table.font", "TableHeader.font", "ComboBox.font", "OptionPane.messageFont",
                "OptionPane.buttonFont", "ToolTip.font"}) {
            UIManager.put(key, BASE);
        }
    }

    private static void put(String key, Color color) {
        UIManager.put(key, new ColorUIResource(color));
    }

    static JButton button(String text) {
        return button(text, false);
    }

    /** A flat button; {@code primary} buttons are filled with the accent color. */
    static JButton button(String text, boolean primary) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setFont(BOLD);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBackground(primary ? ACCENT : FIELD);
        b.setForeground(primary ? BG : TEXT);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary ? ACCENT_DARK : BORDER),
                new EmptyBorder(6, 12, 6, 12)));
        b.setContentAreaFilled(false);
        b.setOpaque(true);
        b.getModel().addChangeListener(e -> {
            ButtonModel m = b.getModel();
            Color base = primary ? ACCENT : FIELD;
            b.setBackground(!b.isEnabled() ? PANEL : m.isPressed() ? base.darker() : m.isRollover() ? base.brighter() : base);
        });
        return b;
    }

    static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT);
        return l;
    }

    static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(MUTED);
        return l;
    }

    static JTextField field() {
        JTextField f = new JTextField();
        f.setColumns(12);
        return f;
    }

    /** A section header: orange title with an underline, as in the exam document. */
    static JComponent sectionHeader(String text) {
        JLabel l = new JLabel(text);
        l.setFont(SECTION);
        l.setForeground(ACCENT);
        l.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ACCENT_DARK),
                new EmptyBorder(0, 0, 4, 0)));
        return l;
    }

    static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(10, 10, 10, 10));
    }

    static JPanel card() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(PANEL);
        p.setBorder(cardBorder());
        return p;
    }

    static JScrollPane scroll(Component c) {
        JScrollPane sp = new JScrollPane(c);
        sp.getViewport().setBackground(PANEL);
        sp.setBackground(PANEL);
        return sp;
    }
}
