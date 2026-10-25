package com.matmon.skidb.ui;

import com.matmon.skidb.api.SkiDBAdapter;

import javax.swing.*;
import java.util.Objects;

/**
 * Entry point of the SkiDB UI.
 *
 * <pre>
 * public static void main(String[] args) {
 *     SkiDBUI.launch(new MyAdapter());
 * }
 * </pre>
 */
public final class SkiDBUI {

    private SkiDBUI() {
    }

    /** Opens the UI window, connected to the given adapter. Closing the window exits the program. */
    public static void launch(SkiDBAdapter adapter) {
        Objects.requireNonNull(adapter, "adapter must not be null");
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            new MainFrame(adapter).setVisible(true);
        });
    }
}
