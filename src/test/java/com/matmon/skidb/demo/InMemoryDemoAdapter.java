package com.matmon.skidb.demo;

import com.matmon.skidb.api.SkiDBAdapter;

import java.util.*;

/**
 * Instructor-only reference adapter used to try out the UI.
 * Lives in test scope, so it is NOT packaged into the student JAR.
 */
public class InMemoryDemoAdapter implements SkiDBAdapter {

    private static final Map<String, Class<?>> TYPES = Map.of(
            "String", String.class, "Integer", Integer.class, "Boolean", Boolean.class);

    /** indexes: field -> (value -> keys) */
    private record Collection(Map<String, String> schema, Map<String, Map<String, Object>> records,
                              Map<String, Map<Object, Set<String>>> indexes) {
    }

    private final Map<String, Collection> collections = new LinkedHashMap<>();

    @Override
    public void createCollection(String name, Map<String, String> schema) {
        if (collections.containsKey(name)) {
            throw new IllegalArgumentException("Collection \"" + name + "\" already exists");
        }
        schema.forEach((field, type) -> {
            if (!TYPES.containsKey(type)) {
                throw new IllegalArgumentException("Unsupported type " + type + " for field " + field);
            }
        });
        collections.put(name, new Collection(new LinkedHashMap<>(schema), new LinkedHashMap<>(), new LinkedHashMap<>()));
    }

    @Override
    public void deleteCollection(String name) {
        if (collections.remove(name) == null) {
            throw new NoSuchElementException("Collection \"" + name + "\" does not exist");
        }
    }

    @Override
    public List<String> getCollectionNames() {
        return new ArrayList<>(collections.keySet());
    }

    @Override
    public Map<String, String> getSchema(String collection) {
        return new LinkedHashMap<>(get(collection).schema());
    }

    @Override
    public void addRecord(String collection, String key, Map<String, Object> value) {
        Collection c = get(collection);
        if (c.records().containsKey(key)) {
            throw new IllegalArgumentException("Key " + key + " already exists");
        }
        validate(c.schema(), value);
        Map<String, Object> stored = new HashMap<>(value);
        c.records().put(key, stored);
        index(c, key, stored);
    }

    @Override
    public Map<String, Object> getRecord(String collection, String key) {
        return new HashMap<>(record(get(collection), key));
    }

    @Override
    public void updateRecord(String collection, String key, Map<String, Object> fields) {
        Collection c = get(collection);
        Map<String, Object> old = record(c, key);
        Map<String, Object> updated = new HashMap<>(old);
        updated.putAll(fields);
        validate(c.schema(), updated);
        unindex(c, key, old);
        c.records().put(key, updated);
        index(c, key, updated);
    }

    @Override
    public void deleteRecord(String collection, String key) {
        Collection c = get(collection);
        unindex(c, key, record(c, key));
        c.records().remove(key);
    }

    @Override
    public Map<String, Map<String, Object>> getAllRecords(String collection) {
        return new LinkedHashMap<>(get(collection).records());
    }

    @Override
    public void createIndex(String collection, String field) {
        Collection c = get(collection);
        if (!c.schema().containsKey(field)) {
            throw new IllegalArgumentException("Field \"" + field + "\" is not in the schema");
        }
        if (c.indexes().containsKey(field)) {
            throw new IllegalArgumentException("Field \"" + field + "\" is already indexed");
        }
        Map<Object, Set<String>> index = new HashMap<>();
        c.records().forEach((key, value) -> index.computeIfAbsent(value.get(field), v -> new HashSet<>()).add(key));
        c.indexes().put(field, index);
    }

    @Override
    public List<String> getIndexedFields(String collection) {
        return new ArrayList<>(get(collection).indexes().keySet());
    }

    @Override
    public Map<Object, Set<String>> getIndex(String collection, String field) {
        Map<Object, Set<String>> index = get(collection).indexes().get(field);
        if (index == null) {
            throw new NoSuchElementException("Field \"" + field + "\" is not indexed");
        }
        return index;
    }

    /** Starts from the smallest index candidate set, then checks the remaining conditions; scans only without an index. */
    @Override
    public Map<String, Map<String, Object>> query(String collection, Map<String, Object> conditions) {
        Collection c = get(collection);
        for (String field : conditions.keySet()) {
            if (!c.schema().containsKey(field)) {
                throw new IllegalArgumentException("Field \"" + field + "\" is not in the schema");
            }
        }
        java.util.Collection<String> candidates = c.records().keySet();
        for (Map.Entry<String, Object> e : conditions.entrySet()) {
            Map<Object, Set<String>> index = c.indexes().get(e.getKey());
            if (index != null) {
                Set<String> keys = index.getOrDefault(e.getValue(), Set.of());
                if (keys.size() < candidates.size()) {
                    candidates = keys;
                }
            }
        }
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (String key : candidates) {
            Map<String, Object> value = c.records().get(key);
            if (conditions.entrySet().stream().allMatch(e -> Objects.equals(value.get(e.getKey()), e.getValue()))) {
                result.put(key, new HashMap<>(value));
            }
        }
        return result;
    }

    private Collection get(String name) {
        Collection c = collections.get(name);
        if (c == null) {
            throw new NoSuchElementException("Collection \"" + name + "\" does not exist");
        }
        return c;
    }

    private static Map<String, Object> record(Collection c, String key) {
        Map<String, Object> value = c.records().get(key);
        if (value == null) {
            throw new NoSuchElementException("Key " + key + " does not exist");
        }
        return value;
    }

    private static void index(Collection c, String key, Map<String, Object> value) {
        c.indexes().forEach((field, index) -> index.computeIfAbsent(value.get(field), v -> new HashSet<>()).add(key));
    }

    private static void unindex(Collection c, String key, Map<String, Object> value) {
        c.indexes().forEach((field, index) -> {
            Set<String> keys = index.get(value.get(field));
            if (keys != null) {
                keys.remove(key);
                if (keys.isEmpty()) {
                    index.remove(value.get(field));
                }
            }
        });
    }

    /** Strict schema: exactly the schema's fields, each of its schema type. */
    private static void validate(Map<String, String> schema, Map<String, Object> value) {
        for (String field : value.keySet()) {
            if (!schema.containsKey(field)) {
                throw new IllegalArgumentException("Field \"" + field + "\" is not in the schema");
            }
        }
        schema.forEach((field, type) -> {
            Object v = value.get(field);
            if (v == null) {
                throw new IllegalArgumentException("Field \"" + field + "\" is missing");
            }
            if (!TYPES.get(type).isInstance(v)) {
                throw new IllegalArgumentException("Field \"" + field + "\" must be " + type
                        + " but got " + v.getClass().getSimpleName());
            }
        });
    }
}
