package com.matmon.skidb.ui;

import java.util.*;

/**
 * Compares the index a student's DBMS returns for a field with the index the UI rebuilds from
 * the records themselves, to show whether add / update / delete kept the index up to date.
 * Values are compared by their text, so an index keyed by "21" or by 21 both count as correct.
 * A value with an empty key set counts the same as a missing value.
 */
final class IndexCheck {

    /** One index entry: a field value, the keys the DBMS returned and the keys it should hold. */
    record Row(String value, List<String> actual, List<String> expected) {
        boolean ok() {
            return actual.equals(expected);
        }
    }

    final String field;
    final String type;
    /** Why the index could not be read, or null if it was read. */
    final String error;
    final List<Row> rows;

    private IndexCheck(String field, String type, String error, List<Row> rows) {
        this.field = field;
        this.type = type;
        this.error = error;
        this.rows = rows;
    }

    static IndexCheck failed(String field, String type, String error) {
        return new IndexCheck(field, type, error, List.of());
    }

    static IndexCheck of(String field, String type, Map<?, ? extends Collection<?>> index,
                         Map<String, Map<String, Object>> records) {
        Map<String, Set<String>> actual = new HashMap<>();
        index.forEach((value, keys) -> {
            if (keys != null && !keys.isEmpty()) {
                Set<String> set = actual.computeIfAbsent(String.valueOf(value), v -> new HashSet<>());
                keys.forEach(k -> set.add(String.valueOf(k)));
            }
        });
        Map<String, Set<String>> expected = new HashMap<>();
        records.forEach((key, value) -> {
            Object v = value == null ? null : value.get(field);
            if (v != null) {
                expected.computeIfAbsent(String.valueOf(v), x -> new HashSet<>()).add(key);
            }
        });

        Set<String> values = new HashSet<>(actual.keySet());
        values.addAll(expected.keySet());
        List<Row> rows = new ArrayList<>();
        for (String value : RecordTable.sortedKeys(values)) {
            rows.add(new Row(value,
                    RecordTable.sortedKeys(actual.getOrDefault(value, Set.of())),
                    RecordTable.sortedKeys(expected.getOrDefault(value, Set.of()))));
        }
        return new IndexCheck(field, type, null, rows);
    }

    boolean ok() {
        return error == null && rows.stream().allMatch(Row::ok);
    }

    /** A short description of what is wrong, for the log. */
    String problem() {
        if (error != null) {
            return "could not be read — " + error;
        }
        List<String> bad = rows.stream().filter(r -> !r.ok())
                .map(r -> "\"" + r.value + "\" has " + r.actual + ", expected " + r.expected)
                .limit(3).toList();
        long count = rows.stream().filter(r -> !r.ok()).count();
        return String.join("; ", bad) + (count > bad.size() ? "; and " + (count - bad.size()) + " more" : "");
    }
}
