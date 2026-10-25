package com.matmon.skidb.ui;

import com.matmon.skidb.api.IndexType;

import java.util.*;

/**
 * Compares the index a student's DBMS returns for a field with the index the UI rebuilds from
 * the records themselves, to show whether add / update / delete kept the index up to date.
 * Values are compared by their text, so an index keyed by "21" or by 21 both count as correct.
 * A value with an empty key set counts the same as a missing value. A TREE index must also be a
 * SortedMap (for example a TreeMap).
 */
final class IndexCheck {

    /** One index entry: a field value, the keys the DBMS returned and the keys it should hold. */
    record Row(String value, List<String> actual, List<String> expected) {
        boolean ok() {
            return actual.equals(expected);
        }
    }

    final String field;
    /** The field's schema type, e.g. Integer. */
    final String type;
    final IndexType indexType;
    /** Why the index could not be read, or null if it was read. */
    final String error;
    /** What is wrong with the index's data structure, or null. */
    final String structureProblem;
    final List<Row> rows;

    private IndexCheck(String field, String type, IndexType indexType, String error, String structureProblem, List<Row> rows) {
        this.field = field;
        this.type = type;
        this.indexType = indexType;
        this.error = error;
        this.structureProblem = structureProblem;
        this.rows = rows;
    }

    static IndexCheck failed(String field, String type, IndexType indexType, String error) {
        return new IndexCheck(field, type, indexType, error, null, List.of());
    }

    static IndexCheck of(String field, String type, IndexType indexType, Map<?, ? extends Collection<?>> index,
                         Map<String, Map<String, Object>> records) {
        String structureProblem = indexType == IndexType.TREE && !(index instanceof SortedMap)
                ? "a TREE index must be a SortedMap such as TreeMap, but it is a " + index.getClass().getSimpleName()
                : null;
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
        return new IndexCheck(field, type, indexType, null, structureProblem, rows);
    }

    boolean ok() {
        return error == null && structureProblem == null && rows.stream().allMatch(Row::ok);
    }

    /** A short description of what is wrong, for the log. */
    String problem() {
        if (error != null) {
            return "could not be read — " + error;
        }
        if (structureProblem != null && rows.stream().allMatch(Row::ok)) {
            return structureProblem;
        }
        List<String> bad = rows.stream().filter(r -> !r.ok())
                .map(r -> "\"" + r.value + "\" has " + r.actual + ", expected " + r.expected)
                .limit(3).toList();
        long count = rows.stream().filter(r -> !r.ok()).count();
        return (structureProblem != null ? structureProblem + "; " : "")
                + String.join("; ", bad) + (count > bad.size() ? "; and " + (count - bad.size()) + " more" : "");
    }
}
