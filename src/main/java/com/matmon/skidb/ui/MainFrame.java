package com.matmon.skidb.ui;

import com.matmon.skidb.api.SkiDBAdapter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The main window. Owns the student's adapter and is the only place that calls it:
 * every call goes through {@link #call} so a bug in student code never crashes the UI.
 */
final class MainFrame extends JFrame {

    interface AdapterCall<T> {
        T call() throws Exception;
    }

    interface AdapterAction {
        void run() throws Exception;
    }

    private final SkiDBAdapter adapter;
    private final LogPanel log = new LogPanel();
    private final CollectionsPanel collections = new CollectionsPanel(this);
    private final RecordsPanel records = new RecordsPanel(this);
    private final RecordFormPanel form = new RecordFormPanel(this);

    private String currentCollection;
    private Map<String, Map<String, Object>> currentRecords = Map.of();

    MainFrame(SkiDBAdapter adapter) {
        super("SkiDB — MatmonDB");
        this.adapter = adapter;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setBackground(Theme.BG);
        setLayout(new BorderLayout());

        add(header(), BorderLayout.NORTH);

        JPanel workspace = new JPanel(new BorderLayout(10, 0));
        workspace.setBackground(Theme.BG);
        collections.setPreferredSize(new Dimension(220, 0));
        form.setPreferredSize(new Dimension(400, 0));
        workspace.add(collections, BorderLayout.WEST);
        workspace.add(records, BorderLayout.CENTER);
        workspace.add(form, BorderLayout.EAST);

        log.setPreferredSize(new Dimension(0, 180));
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setBackground(Theme.BG);
        body.setBorder(new EmptyBorder(12, 14, 14, 14));
        body.add(workspace, BorderLayout.CENTER);
        body.add(log, BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);

        setSize(1260, 780);
        setMinimumSize(new Dimension(1000, 600));
        setLocationRelativeTo(null);

        log.info("Connected to adapter " + adapter.getClass().getName());
        refreshAll();
    }

    private JComponent header() {
        JLabel brand = new JLabel("Matmon");
        brand.setFont(Theme.BRAND);
        brand.setForeground(Theme.TEXT);

        JLabel title = new JLabel("SkiDB — Stage B", SwingConstants.CENTER);
        title.setFont(Theme.TITLE);
        title.setForeground(Theme.TEXT);

        JButton print = Theme.button("Print database");
        print.setToolTipText("Write the full database structure to the log");
        print.addActionListener(e -> printDatabase());
        JButton refresh = Theme.button("Refresh");
        refresh.addActionListener(e -> {
            refreshAll();
            log.info("Refreshed");
        });
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(print);
        actions.add(refresh);

        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.BG);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, Theme.ACCENT),
                new EmptyBorder(12, 16, 12, 16)));
        p.add(brand, BorderLayout.WEST);
        p.add(title, BorderLayout.CENTER);
        p.add(actions, BorderLayout.EAST);
        return p;
    }

    // ---------------------------------------------------------------- adapter calls

    /**
     * Runs a call against the student's adapter. Failures (any Throwable, or a null result)
     * are logged; when {@code loud} they are also shown in a dialog and successes are logged.
     */
    private <T> Optional<T> call(String description, AdapterCall<T> c, boolean loud) {
        try {
            T result = c.call();
            if (result == null) {
                fail(description, "the adapter returned null", loud);
                return Optional.empty();
            }
            if (loud) {
                log.success(description);
            }
            return Optional.of(result);
        } catch (Throwable t) {
            String message = t.getClass().getSimpleName() + (t.getMessage() != null ? ": " + t.getMessage() : "");
            fail(description, message + origin(t), loud);
            return Optional.empty();
        }
    }

    private boolean run(String description, AdapterAction a) {
        return call(description, () -> {
            a.run();
            return Boolean.TRUE;
        }, true).isPresent();
    }

    private void fail(String description, String message, boolean loud) {
        log.error(description + " failed — " + message);
        if (loud) {
            JOptionPane.showMessageDialog(this, "<html><b>" + escape(description) + "</b><br><br>"
                    + escape(message) + "</html>", "Operation failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** The first stack frame outside this UI, so students see where their code threw. */
    private static String origin(Throwable t) {
        for (StackTraceElement e : t.getStackTrace()) {
            if (!e.getClassName().startsWith("com.matmon.skidb.ui.")) {
                return "  (at " + e.getClassName() + "." + e.getMethodName() + ":" + e.getLineNumber() + ")";
            }
        }
        return "";
    }

    // ---------------------------------------------------------------- state

    /** Reloads the collection list and the selected collection from the adapter. */
    void refreshAll() {
        List<String> names = call("getCollectionNames()", adapter::getCollectionNames, false).orElse(List.of());
        if (currentCollection != null && !names.contains(currentCollection)) {
            currentCollection = null;
        }
        collections.setNames(names, currentCollection);
        loadCollection();
    }

    void onCollectionSelected(String name) {
        if (!Objects.equals(name, currentCollection)) {
            currentCollection = name;
            loadCollection();
        }
    }

    void onRecordSelected(String key) {
        Map<String, Object> value = currentRecords.get(key);
        if (value != null) {
            form.fill(key, value);
        }
    }

    private void loadCollection() {
        String name = currentCollection;
        if (name == null) {
            currentRecords = Map.of();
            records.clear();
            form.setCollection(null, Map.of());
            return;
        }
        Map<String, String> schema = call("getSchema(\"" + name + "\")", () -> adapter.getSchema(name), false)
                .orElse(Map.of());
        currentRecords = call("getAllRecords(\"" + name + "\")", () -> adapter.getAllRecords(name), false)
                .orElse(Map.of());
        records.show(name, schema, currentRecords);
        form.setCollection(name, schema);
    }

    // ---------------------------------------------------------------- operations

    void createCollection(String name, Map<String, String> schema) {
        if (run("Create collection \"" + name + "\" " + format(schema), () -> adapter.createCollection(name, schema))) {
            currentCollection = name;
            refreshAll();
        }
    }

    void deleteCollection(String name) {
        if (run("Delete collection \"" + name + "\"", () -> adapter.deleteCollection(name))) {
            refreshAll();
        }
    }

    boolean addRecord(String key, Map<String, Object> value) {
        String c = currentCollection;
        boolean ok = run("Add record " + key + " " + format(value) + " to \"" + c + "\"", () -> adapter.addRecord(c, key, value));
        if (ok) {
            refreshAll();
            records.select(key);
        }
        return ok;
    }

    void getRecord(String key) {
        String c = currentCollection;
        call("getRecord(\"" + c + "\", " + key + ")", () -> adapter.getRecord(c, key), true).ifPresent(value -> {
            log.info(key + " → " + format(value));
            form.fill(key, value);
            records.select(key);
        });
    }

    void updateRecord(String key, Map<String, Object> fields) {
        String c = currentCollection;
        if (run("Update record " + key + " with " + format(fields) + " in \"" + c + "\"", () -> adapter.updateRecord(c, key, fields))) {
            refreshAll();
            records.select(key);
        }
    }

    void deleteRecord(String key) {
        String c = currentCollection;
        if (run("Delete record " + key + " from \"" + c + "\"", () -> adapter.deleteRecord(c, key))) {
            refreshAll();
            form.clear();
        }
    }

    private void printDatabase() {
        List<String> names = call("getCollectionNames()", adapter::getCollectionNames, true).orElse(null);
        if (names == null) {
            return;
        }
        log.info("Database structure — " + names.size() + " collection(s)");
        for (String name : names) {
            Map<String, String> schema = call("getSchema(\"" + name + "\")", () -> adapter.getSchema(name), false).orElse(null);
            Map<String, Map<String, Object>> recs = call("getAllRecords(\"" + name + "\")", () -> adapter.getAllRecords(name), false).orElse(null);
            log.info("  " + name + "  schema " + (schema == null ? "?" : format(schema)));
            if (recs != null) {
                RecordsPanel.sortedKeys(recs.keySet()).forEach(k -> log.info("      " + k + " → " + format(recs.get(k))));
            }
        }
    }

    // ---------------------------------------------------------------- helpers

    /** Formats a map JSON-style: {"firstName":"Israel", "lastName":"Israeli"}. */
    static String format(Map<String, ?> map) {
        if (map == null) {
            return "null";
        }
        return map.entrySet().stream()
                .map(e -> quote(e.getKey()) + ":" + (e.getValue() instanceof String s ? quote(s) : String.valueOf(e.getValue())))
                .collect(Collectors.joining(", ", "{", "}"));
    }

    private static String quote(String s) {
        return "\"" + s + "\"";
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
