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

    private static final String TAB_RECORDS = "Records";
    private static final String TAB_QUERY = "Query";
    private static final String TAB_INDEXES = "Indexes";

    private final SkiDBAdapter adapter;
    private final LogPanel log = new LogPanel();
    private final CollectionsPanel collections = new CollectionsPanel(this);
    private final RecordsPanel records = new RecordsPanel(this);
    private final QueryPanel query = new QueryPanel(this);
    private final IndexesPanel indexes = new IndexesPanel(this);
    private final RecordFormPanel form = new RecordFormPanel(this);
    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards);
    private final Map<String, JButton> tabs = new LinkedHashMap<>();

    private String currentCollection;
    private Map<String, String> currentSchema = Map.of();
    private Map<String, Map<String, Object>> currentRecords = Map.of();
    private List<IndexCheck> currentChecks = List.of();

    MainFrame(SkiDBAdapter adapter) {
        super("SkiDB — MatmonDB");
        this.adapter = adapter;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setBackground(Theme.BG);
        setLayout(new BorderLayout());

        add(header(), BorderLayout.NORTH);

        cardPanel.setBackground(Theme.BG);
        cardPanel.add(records, TAB_RECORDS);
        cardPanel.add(query, TAB_QUERY);
        cardPanel.add(indexes, TAB_INDEXES);
        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(Theme.BG);
        center.add(tabBar(), BorderLayout.NORTH);
        center.add(cardPanel, BorderLayout.CENTER);

        JPanel workspace = new JPanel(new BorderLayout(10, 0));
        workspace.setBackground(Theme.BG);
        collections.setPreferredSize(new Dimension(220, 0));
        form.setPreferredSize(new Dimension(400, 0));
        workspace.add(collections, BorderLayout.WEST);
        workspace.add(center, BorderLayout.CENTER);
        workspace.add(form, BorderLayout.EAST);

        log.setPreferredSize(new Dimension(0, 180));
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setBackground(Theme.BG);
        body.setBorder(new EmptyBorder(12, 14, 14, 14));
        body.add(workspace, BorderLayout.CENTER);
        body.add(log, BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);

        setSize(1360, 820);
        setMinimumSize(new Dimension(1100, 640));
        setLocationRelativeTo(null);

        log.info("Connected to adapter " + adapter.getClass().getName());
        refreshAll();
    }

    private JComponent header() {
        JLabel brand = new JLabel("Matmon");
        brand.setFont(Theme.BRAND);
        brand.setForeground(Theme.TEXT);

        JLabel title = new JLabel("SkiDB — Stage C", SwingConstants.CENTER);
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

    private JComponent tabBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bar.setBackground(Theme.BG);
        for (String name : List.of(TAB_RECORDS, TAB_QUERY, TAB_INDEXES)) {
            JButton tab = Theme.button(name);
            tab.addActionListener(e -> selectTab(name));
            tabs.put(name, tab);
            bar.add(tab);
        }
        selectTab(TAB_RECORDS);
        return bar;
    }

    private void selectTab(String name) {
        cards.show(cardPanel, name);
        tabs.forEach((n, tab) -> {
            boolean selected = n.equals(name);
            tab.setForeground(selected ? Theme.ACCENT : Theme.MUTED);
            tab.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 1, selected ? 0 : 1, 1, Theme.BORDER),
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(2, 0, 0, 0, selected ? Theme.ACCENT : Theme.FIELD),
                            new EmptyBorder(6, 18, 6, 18))));
        });
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
            fail(description, describe(t), loud);
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

    private static String describe(Throwable t) {
        return t.getClass().getSimpleName() + (t.getMessage() != null ? ": " + t.getMessage() : "") + origin(t);
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
            currentSchema = Map.of();
            currentRecords = Map.of();
            currentChecks = List.of();
            records.clear();
            indexes.clear();
            query.setCollection(null, Map.of(), List.of());
            form.setCollection(null, Map.of());
            updateIndexesTab();
            return;
        }
        currentSchema = call("getSchema(\"" + name + "\")", () -> adapter.getSchema(name), false).orElse(Map.of());
        currentRecords = call("getAllRecords(\"" + name + "\")", () -> adapter.getAllRecords(name), false).orElse(Map.of());
        List<String> indexed = call("getIndexedFields(\"" + name + "\")", () -> adapter.getIndexedFields(name), false)
                .<List<String>>map(ArrayList::new).orElse(new ArrayList<>());
        currentChecks = indexed.stream().map(field -> checkIndex(name, field)).toList();

        records.show(name, currentSchema, indexed, currentRecords);
        query.setCollection(name, currentSchema, indexed);
        indexes.show(currentSchema, indexed, currentChecks);
        form.setCollection(name, currentSchema);
        updateIndexesTab();
    }

    private IndexCheck checkIndex(String collection, String field) {
        String type = currentSchema.getOrDefault(field, "?");
        try {
            Map<?, ? extends Collection<?>> index = adapter.getIndex(collection, field);
            if (index == null) {
                return IndexCheck.failed(field, type, "getIndex returned null");
            }
            return IndexCheck.of(field, type, index, currentRecords);
        } catch (Throwable t) {
            return IndexCheck.failed(field, type, describe(t));
        }
    }

    private void updateIndexesTab() {
        boolean bad = currentChecks.stream().anyMatch(c -> !c.ok());
        tabs.get(TAB_INDEXES).setText(bad ? TAB_INDEXES + " ⚠" : TAB_INDEXES);
    }

    /** Logs every index that no longer matches the records - called after each change. */
    private void reportIndexProblems() {
        for (IndexCheck check : currentChecks) {
            if (!check.ok()) {
                log.error("Index on \"" + check.field + "\" is out of sync — " + check.problem());
            }
        }
    }

    // ---------------------------------------------------------------- operations

    void createCollection(String name, Map<String, String> schema, List<String> indexedFields) {
        if (run("Create collection \"" + name + "\" " + format(schema), () -> adapter.createCollection(name, schema))) {
            for (String field : indexedFields) {
                run("Create index on \"" + field + "\" in \"" + name + "\"", () -> adapter.createIndex(name, field));
            }
            currentCollection = name;
            refreshAll();
            reportIndexProblems();
        }
    }

    void deleteCollection(String name) {
        if (run("Delete collection \"" + name + "\"", () -> adapter.deleteCollection(name))) {
            refreshAll();
        }
    }

    void createIndex(String field) {
        String c = currentCollection;
        if (run("Create index on \"" + field + "\" in \"" + c + "\"", () -> adapter.createIndex(c, field))) {
            refreshAll();
            reportIndexProblems();
        }
    }

    boolean addRecord(String key, Map<String, Object> value) {
        String c = currentCollection;
        boolean ok = run("Add record " + key + " " + format(value) + " to \"" + c + "\"", () -> adapter.addRecord(c, key, value));
        if (ok) {
            refreshAll();
            records.select(key);
            reportIndexProblems();
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
            reportIndexProblems();
        }
    }

    void deleteRecord(String key) {
        String c = currentCollection;
        if (run("Delete record " + key + " from \"" + c + "\"", () -> adapter.deleteRecord(c, key))) {
            refreshAll();
            form.clear();
            reportIndexProblems();
        }
    }

    /**
     * Runs a query, times it, and compares its result with a full scan of the records,
     * so a wrong or incomplete index shows up as missing or unexpected keys.
     */
    void runQuery(Map<String, Object> conditions) {
        String c = currentCollection;
        long[] nanos = new long[1];
        Optional<Map<String, Map<String, Object>>> result = call("Query \"" + c + "\" where " + format(conditions), () -> {
            long start = System.nanoTime();
            Map<String, Map<String, Object>> r = adapter.query(c, conditions);
            nanos[0] = System.nanoTime() - start;
            return r;
        }, true);
        if (result.isEmpty()) {
            query.showFailure("The query failed — see the log.");
            return;
        }
        Map<String, Map<String, Object>> found = result.get();

        Set<String> expected = new HashSet<>();
        currentRecords.forEach((key, value) -> {
            if (value != null && conditions.entrySet().stream().allMatch(e -> Objects.equals(value.get(e.getKey()), e.getValue()))) {
                expected.add(key);
            }
        });
        Set<String> missing = new HashSet<>(expected);
        missing.removeAll(found.keySet());
        Set<String> unexpected = new HashSet<>(found.keySet());
        unexpected.removeAll(expected);
        boolean ok = missing.isEmpty() && unexpected.isEmpty();

        List<String> indexed = currentChecks.stream().map(ch -> ch.field).toList();
        List<String> used = conditions.keySet().stream().filter(indexed::contains).toList();
        List<String> notIndexed = conditions.keySet().stream().filter(f -> !indexed.contains(f)).toList();
        String time = String.format("%.3f ms", nanos[0] / 1_000_000.0);
        String summary = found.size() + (found.size() == 1 ? " result" : " results") + " in " + time
                + " · " + currentRecords.size() + " records in the collection";
        String indexLine = (used.isEmpty() ? "" : "indexed: " + String.join(", ", used))
                + (used.isEmpty() || notIndexed.isEmpty() ? "" : " · ")
                + (notIndexed.isEmpty() ? "" : "not indexed: " + String.join(", ", notIndexed) + " (may scan every record)");
        String check = ok ? "✔ Same records as a full scan of the collection"
                : "✖ Different from a full scan of the collection"
                + (missing.isEmpty() ? "" : " · missing keys " + RecordTable.sortedKeys(missing))
                + (unexpected.isEmpty() ? "" : " · unexpected keys " + RecordTable.sortedKeys(unexpected));

        query.showResult("<html>" + escape(summary) + "<br>" + escape(indexLine) + "<br>" + escape(check) + "</html>", ok, found);
        if (ok) {
            log.info(summary + " · " + indexLine);
        } else {
            log.error("Query result is wrong — " + check.substring(2));
        }
    }

    /** Adds many random records, to measure query speed with and without an index. */
    void generateRecords() {
        String c = currentCollection;
        String input = JOptionPane.showInputDialog(this,
                "How many random records to add to \"" + c + "\"?", "Generate data", JOptionPane.QUESTION_MESSAGE);
        if (input == null) {
            return;
        }
        int count;
        try {
            count = Integer.parseInt(input.trim());
            if (count < 1 || count > 1_000_000) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Enter a number between 1 and 1,000,000.", "Generate data", JOptionPane.WARNING_MESSAGE);
            return;
        }

        long next = currentRecords.keySet().stream().mapToLong(k -> {
            try {
                return Long.parseLong(k.trim());
            } catch (NumberFormatException e) {
                return 0;
            }
        }).max().orElse(0) + 1;
        String[] names = {"Israel", "Ploni", "John", "Dana", "Noa", "Yossi", "Maya", "Avi", "Tamar", "David",
                "Shira", "Omer", "Yael", "Itai", "Roni", "Gal", "Lior", "Neta", "Eden", "Amit"};
        Random random = new Random();
        Map<String, String> schema = currentSchema;
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        long start = System.nanoTime();
        int added = 0;
        try {
            for (; added < count; added++) {
                String key = String.valueOf(next + added);
                Map<String, Object> value = new LinkedHashMap<>();
                schema.forEach((field, type) -> value.put(field, switch (type) {
                    case FieldTypes.INTEGER -> 18 + random.nextInt(50);
                    case FieldTypes.BOOLEAN -> random.nextBoolean();
                    default -> names[random.nextInt(names.length)];
                }));
                if (call("Add record " + key + " " + format(value), () -> {
                    adapter.addRecord(c, key, value);
                    return Boolean.TRUE;
                }, false).isEmpty()) {
                    break;
                }
            }
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
        String time = String.format("%.1f ms", (System.nanoTime() - start) / 1_000_000.0);
        if (added == count) {
            log.success("Generated " + added + " records in \"" + c + "\" in " + time);
        } else {
            log.error("Generated " + added + " of " + count + " records — stopped at the first failure (see above)");
        }
        refreshAll();
        reportIndexProblems();
    }

    private void printDatabase() {
        List<String> names = call("getCollectionNames()", adapter::getCollectionNames, true).orElse(null);
        if (names == null) {
            return;
        }
        log.info("Database structure — " + names.size() + " collection(s)");
        for (String name : names) {
            Map<String, String> schema = call("getSchema(\"" + name + "\")", () -> adapter.getSchema(name), false).orElse(null);
            Collection<String> indexed = call("getIndexedFields(\"" + name + "\")", () -> adapter.getIndexedFields(name), false).orElse(null);
            Map<String, Map<String, Object>> recs = call("getAllRecords(\"" + name + "\")", () -> adapter.getAllRecords(name), false).orElse(null);
            log.info("  " + name + "  schema " + (schema == null ? "?" : format(schema))
                    + "  indexes " + (indexed == null ? "?" : indexed));
            if (recs != null) {
                RecordTable.sortedKeys(recs.keySet()).forEach(k -> log.info("      " + k + " → " + format(recs.get(k))));
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
