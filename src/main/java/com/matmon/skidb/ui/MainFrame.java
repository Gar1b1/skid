package com.matmon.skidb.ui;

import com.matmon.skidb.api.Condition;
import com.matmon.skidb.api.IndexType;
import com.matmon.skidb.api.SkiDBAdapter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
    private static final String TAB_PARTITIONS = "Partitions";

    private final SkiDBAdapter adapter;
    private final LogPanel log = new LogPanel();
    private final CollectionsPanel collections = new CollectionsPanel(this);
    private final RecordsPanel records = new RecordsPanel(this);
    private final QueryPanel query = new QueryPanel(this);
    private final IndexesPanel indexes = new IndexesPanel(this);
    private final PartitionsPanel partitions = new PartitionsPanel();
    private final RecordFormPanel form = new RecordFormPanel(this);
    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards);
    private final Map<String, JButton> tabs = new LinkedHashMap<>();

    private String currentCollection;
    private Map<String, String> currentSchema = Map.of();
    private Map<String, Map<String, Object>> currentRecords = Map.of();
    private List<IndexCheck> currentChecks = List.of();
    private String currentPartitionField;
    private IndexCheck currentPartitionCheck;

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
        cardPanel.add(partitions, TAB_PARTITIONS);
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

        JLabel title = new JLabel("SkiDB — Stage F", SwingConstants.CENTER);
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
        for (String name : List.of(TAB_RECORDS, TAB_QUERY, TAB_INDEXES, TAB_PARTITIONS)) {
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
            currentPartitionField = null;
            currentPartitionCheck = null;
            records.clear();
            indexes.clear();
            partitions.clear();
            query.setCollection(null, Map.of(), Map.of());
            form.setCollection(null, Map.of());
            updateIndexesTab();
            return;
        }
        currentSchema = call("getSchema(\"" + name + "\")", () -> adapter.getSchema(name), false).orElse(Map.of());
        currentRecords = call("getAllRecords(\"" + name + "\")", () -> adapter.getAllRecords(name), false).orElse(Map.of());
        Map<String, IndexType> indexed = call("getIndexes(\"" + name + "\")", () -> adapter.getIndexes(name), false)
                .<Map<String, IndexType>>map(LinkedHashMap::new).orElse(new LinkedHashMap<>());
        currentChecks = indexed.entrySet().stream().map(e -> checkIndex(name, e.getKey(), e.getValue())).toList();
        loadPartitions(name);

        records.show(name, currentSchema, indexed, currentPartitionField, currentRecords);
        query.setCollection(name, currentSchema, indexed);
        indexes.show(currentSchema, indexed.keySet(), currentChecks);
        partitions.show(currentPartitionField, currentPartitionCheck);
        form.setCollection(name, currentSchema);
        updateIndexesTab();
    }

    private IndexCheck checkIndex(String collection, String field, IndexType indexType) {
        String type = currentSchema.getOrDefault(field, "?");
        try {
            Map<?, ? extends Collection<?>> index = adapter.getIndex(collection, field);
            if (index == null) {
                return IndexCheck.failed(field, type, indexType, "getIndex returned null");
            }
            return IndexCheck.of(field, type, indexType, index, currentRecords);
        } catch (Throwable t) {
            return IndexCheck.failed(field, type, indexType, describe(t));
        }
    }

    /** Reads the partition field (null when not partitioned) and checks the partitions against the records. */
    private void loadPartitions(String name) {
        currentPartitionField = null;
        currentPartitionCheck = null;
        try {
            currentPartitionField = adapter.getPartitionField(name);
        } catch (Throwable t) {
            log.error("getPartitionField(\"" + name + "\") failed — " + describe(t));
            return;
        }
        if (currentPartitionField == null) {
            return;
        }
        try {
            Map<String, ? extends Collection<String>> p = adapter.getPartitions(name);
            currentPartitionCheck = p == null
                    ? IndexCheck.failed(currentPartitionField, FieldTypes.DATETIME, null, "getPartitions returned null")
                    : IndexCheck.partitions(currentPartitionField, p, currentRecords);
        } catch (Throwable t) {
            currentPartitionCheck = IndexCheck.failed(currentPartitionField, FieldTypes.DATETIME, null, describe(t));
        }
    }

    private void updateIndexesTab() {
        boolean bad = currentChecks.stream().anyMatch(c -> !c.ok());
        tabs.get(TAB_INDEXES).setText(bad ? TAB_INDEXES + " ⚠" : TAB_INDEXES);
        boolean badPartitions = currentPartitionCheck != null && !currentPartitionCheck.ok();
        tabs.get(TAB_PARTITIONS).setText(badPartitions ? TAB_PARTITIONS + " ⚠" : TAB_PARTITIONS);
    }

    /** Logs every index that no longer matches the records - called after each change. */
    private void reportIndexProblems() {
        for (IndexCheck check : currentChecks) {
            if (!check.ok()) {
                log.error("Index on \"" + check.field + "\" is out of sync — " + check.problem());
            }
        }
        if (currentPartitionCheck != null && !currentPartitionCheck.ok()) {
            log.error("Partitions of \"" + currentPartitionCheck.field + "\" are out of sync — " + currentPartitionCheck.problem());
        }
    }

    // ---------------------------------------------------------------- operations

    void createCollection(String name, Map<String, String> schema, Map<String, IndexType> indexedFields, String partitionField) {
        boolean created = partitionField == null
                ? run("Create collection \"" + name + "\" " + format(schema), () -> adapter.createCollection(name, schema))
                : run("Create collection \"" + name + "\" " + format(schema) + " partitioned by the hour of \"" + partitionField + "\"",
                () -> adapter.createPartitionedCollection(name, schema, partitionField));
        if (created) {
            indexedFields.forEach((field, type) -> run("Create " + type + " index on \"" + field + "\" in \"" + name + "\"",
                    () -> adapter.createIndex(name, field, type)));
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

    void createIndex(String field, IndexType type) {
        String c = currentCollection;
        if (run("Create " + type + " index on \"" + field + "\" in \"" + c + "\"", () -> adapter.createIndex(c, field, type))) {
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
    void runQuery(List<Condition> conditions) {
        String c = currentCollection;
        long[] nanos = new long[1];
        String where = conditions.stream().map(Condition::toString).collect(Collectors.joining(" AND "));
        Optional<Map<String, Map<String, Object>>> result = call("Query \"" + c + "\" where " + where, () -> {
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
            if (ConditionCheck.matchesAll(value, conditions)) {
                expected.add(key);
            }
        });
        Set<String> missing = new HashSet<>(expected);
        missing.removeAll(found.keySet());
        Set<String> unexpected = new HashSet<>(found.keySet());
        unexpected.removeAll(expected);
        boolean ok = missing.isEmpty() && unexpected.isEmpty();

        Map<String, IndexType> indexed = new HashMap<>();
        currentChecks.forEach(ch -> indexed.put(ch.field, ch.indexType));
        List<String> used = new ArrayList<>();
        List<String> notUsed = new ArrayList<>();
        for (Condition cond : conditions) {
            IndexType t = indexed.get(cond.field());
            boolean range = cond.operator() != Condition.Operator.EQUALS;
            if (cond.field().equals(currentPartitionField)) {
                used.add(cond.field() + " " + cond.operator().symbol() + " (partitions" + (t == null ? "" : ", " + t) + ")");
            } else if (t == IndexType.TREE || (t == IndexType.HASH && !range)) {
                used.add(cond.field() + " " + cond.operator().symbol() + " (" + t + ")");
            } else {
                notUsed.add(cond.field() + " " + cond.operator().symbol() + (t == IndexType.HASH ? " (HASH can't help a range)" : ""));
            }
        }
        String time = String.format("%.3f ms", nanos[0] / 1_000_000.0);
        String summary = found.size() + (found.size() == 1 ? " result" : " results") + " in " + time
                + " · " + currentRecords.size() + " records in the collection";
        String indexLine = (used.isEmpty() ? "" : "can narrow the search: " + String.join(", ", used))
                + (used.isEmpty() || notUsed.isEmpty() ? "" : " · ")
                + (notUsed.isEmpty() ? "" : "no usable index: " + String.join(", ", notUsed));
        String check = ok ? "✔ Same records as a full scan of the collection"
                : "✖ Different from a full scan of the collection"
                + (missing.isEmpty() ? "" : " · missing keys " + RecordTable.sortedKeys(missing))
                + (unexpected.isEmpty() ? "" : " · unexpected keys " + RecordTable.sortedKeys(unexpected));

        String partitionLine = currentPartitionField == null ? null : checkPruning(c, conditions);
        boolean pruned = partitionLine == null || partitionLine.startsWith("✔");
        query.showResult("<html>" + escape(summary) + "<br>" + escape(indexLine) + "<br>" + escape(check)
                + (partitionLine == null ? "" : "<br>" + escape(partitionLine)) + "</html>", ok && pruned, found);
        if (ok) {
            log.info(summary + " · " + indexLine);
        } else {
            log.error("Query result is wrong — " + check.substring(2));
        }
        if (partitionLine != null) {
            if (pruned) {
                log.info(partitionLine.substring(2));
            } else {
                log.error(partitionLine.substring(2));
            }
            partitions.showLastQuery("<html>Last query: " + escape(where) + "<br>" + escape(partitionLine) + "</html>", pruned);
        }
    }

    /**
     * Compares the partitions the student's last query read with the partitions that hold records
     * whose hour can satisfy the conditions on the partition field.
     */
    private String checkPruning(String c, List<Condition> conditions) {
        Optional<Collection<String>> scannedResult = call("getScannedPartitions(\"" + c + "\")", () -> adapter.getScannedPartitions(c), false);
        if (scannedResult.isEmpty()) {
            return "✖ Partitions: getScannedPartitions failed — see the log";
        }
        Set<String> scanned = new TreeSet<>();
        scannedResult.get().forEach(p -> scanned.add(String.valueOf(p)));
        Set<String> all = new TreeSet<>();
        currentRecords.values().forEach(v -> {
            if (v != null && v.get(currentPartitionField) instanceof LocalDateTime t) {
                all.add(FieldTypes.hourId(t));
            }
        });
        List<Condition> onTime = conditions.stream().filter(cond -> cond.field().equals(currentPartitionField)).toList();
        Set<String> needed = new TreeSet<>();
        for (String id : all) {
            LocalDateTime start = FieldTypes.hourStart(id);
            if (onTime.stream().allMatch(cond -> overlaps(cond, start))) {
                needed.add(id);
            }
        }
        Set<String> extra = new TreeSet<>(scanned);
        extra.removeAll(needed);
        Set<String> missing = new TreeSet<>(needed);
        missing.removeAll(scanned);
        String counts = "Partitions: read " + scanned.size() + " of " + all.size() + ", needed " + needed.size()
                + (onTime.isEmpty() ? " (no condition on \"" + currentPartitionField + "\", so every partition is needed)" : "");
        if (extra.isEmpty() && missing.isEmpty()) {
            return "✔ " + counts;
        }
        return "✖ " + counts
                + (extra.isEmpty() ? "" : " · could have skipped " + shortList(extra))
                + (missing.isEmpty() ? "" : " · did not read needed " + shortList(missing));
    }

    /** Whether some time inside the hour starting at {@code start} can satisfy the condition. */
    private static boolean overlaps(Condition cond, LocalDateTime start) {
        if (!(cond.value() instanceof LocalDateTime v)) {
            return false; // a value of the wrong type matches nothing
        }
        LocalDateTime last = start.plus(1, ChronoUnit.HOURS).minusNanos(1);
        return switch (cond.operator()) {
            case EQUALS -> !v.isBefore(start) && !v.isAfter(last);
            case GREATER_THAN -> last.isAfter(v);
            case GREATER_OR_EQUAL -> !last.isBefore(v);
            case LESS_THAN -> start.isBefore(v);
            case LESS_OR_EQUAL -> !start.isAfter(v);
            case BETWEEN -> cond.to() instanceof LocalDateTime to && !start.isAfter(to) && !last.isBefore(v);
        };
    }

    private static String shortList(Set<String> ids) {
        List<String> first = ids.stream().limit(3).toList();
        return first + (ids.size() > first.size() ? " and " + (ids.size() - first.size()) + " more" : "");
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
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
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
                    case FieldTypes.DATETIME -> now.minusMinutes(random.nextInt(3 * 24 * 60));
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
            Map<String, IndexType> indexed = call("getIndexes(\"" + name + "\")", () -> adapter.getIndexes(name), false).orElse(null);
            Map<String, Map<String, Object>> recs = call("getAllRecords(\"" + name + "\")", () -> adapter.getAllRecords(name), false).orElse(null);
            String partitionField;
            try {
                partitionField = adapter.getPartitionField(name);
            } catch (Throwable t) {
                partitionField = "?";
            }
            log.info("  " + name + "  schema " + (schema == null ? "?" : format(schema))
                    + "  indexes " + (indexed == null ? "?" : indexed)
                    + (partitionField == null ? "" : "  partitioned by the hour of " + partitionField));
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
