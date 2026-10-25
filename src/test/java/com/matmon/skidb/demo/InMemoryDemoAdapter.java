package com.matmon.skidb.demo;

import com.matmon.skidb.api.SkiDBAdapter;

import java.util.*;

/**
 * Instructor-only reference adapter used to try out the UI.
 * Lives in test scope, so it is NOT packaged into the student JAR.
 */
public class InMemoryDemoAdapter implements SkiDBAdapter {

    private record Collection(Map<String, String> schema, Map<String, Map<String, Object>> records) {
    }

    private final Map<String, Collection> collections = new LinkedHashMap<>();

    @Override
    public void createCollection(String name, Map<String, String> schema) {
        if (collections.containsKey(name)) {
            throw new IllegalArgumentException("Collection \"" + name + "\" already exists");
        }
        schema.forEach((field, type) -> {
            if (!"String".equals(type)) {
                throw new IllegalArgumentException("Unsupported type " + type + " for field " + field);
            }
        });
        collections.put(name, new Collection(new LinkedHashMap<>(schema), new LinkedHashMap<>()));
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
        c.records().put(key, new HashMap<>(value));
    }

    @Override
    public Map<String, Object> getRecord(String collection, String key) {
        return new HashMap<>(record(get(collection), key));
    }

    @Override
    public void updateRecord(String collection, String key, Map<String, Object> fields) {
        Collection c = get(collection);
        Map<String, Object> updated = new HashMap<>(record(c, key));
        updated.putAll(fields);
        validate(c.schema(), updated);
        c.records().put(key, updated);
    }

    @Override
    public void deleteRecord(String collection, String key) {
        Collection c = get(collection);
        record(c, key);
        c.records().remove(key);
    }

    @Override
    public Map<String, Map<String, Object>> getAllRecords(String collection) {
        return new LinkedHashMap<>(get(collection).records());
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

    /** Strict schema: exactly the schema's fields, each a String. */
    private static void validate(Map<String, String> schema, Map<String, Object> value) {
        for (String field : value.keySet()) {
            if (!schema.containsKey(field)) {
                throw new IllegalArgumentException("Field \"" + field + "\" is not in the schema");
            }
        }
        for (String field : schema.keySet()) {
            if (!(value.get(field) instanceof String)) {
                throw new IllegalArgumentException("Field \"" + field + "\" is missing or not a String");
            }
        }
    }
}
