package com.matmon.skidb.demo;

import com.matmon.skidb.api.Condition;
import com.matmon.skidb.api.IndexType;
import com.matmon.skidb.api.SkiDBAdapter;

import java.util.*;

/**
 * Instructor-only reference adapter used to try out the UI.
 * Lives in test scope, so it is NOT packaged into the student JAR.
 */
public class InMemoryDemoAdapter implements SkiDBAdapter {

    private static final Map<String, Class<?>> TYPES = Map.of(
            "String", String.class, "Integer", Integer.class, "Boolean", Boolean.class);

    /** indexes: field -> (value -> keys); indexTypes: field -> HASH / TREE */
    private record Collection(Map<String, String> schema, Map<String, Map<String, Object>> records,
                              Map<String, Map<Object, Set<String>>> indexes, Map<String, IndexType> indexTypes) {
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
        collections.put(name, new Collection(new LinkedHashMap<>(schema), new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>()));
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
    public void createIndex(String collection, String field, IndexType type) {
        Collection c = get(collection);
        if (!c.schema().containsKey(field)) {
            throw new IllegalArgumentException("Field \"" + field + "\" is not in the schema");
        }
        if (c.indexes().containsKey(field)) {
            throw new IllegalArgumentException("Field \"" + field + "\" is already indexed");
        }
        Map<Object, Set<String>> index = type == IndexType.TREE ? new TreeMap<>() : new HashMap<>();
        c.records().forEach((key, value) -> index.computeIfAbsent(value.get(field), v -> new HashSet<>()).add(key));
        c.indexes().put(field, index);
        c.indexTypes().put(field, type);
    }

    @Override
    public Map<String, IndexType> getIndexes(String collection) {
        return new LinkedHashMap<>(get(collection).indexTypes());
    }

    @Override
    public Map<Object, Set<String>> getIndex(String collection, String field) {
        Map<Object, Set<String>> index = get(collection).indexes().get(field);
        if (index == null) {
            throw new NoSuchElementException("Field \"" + field + "\" is not indexed");
        }
        return index;
    }

    /**
     * Starts from the smallest candidate key set any index can give (HASH: "=" only, TREE: "=" and
     * ranges), then checks every condition on those records; scans only when no index helps.
     */
    @Override
    public Map<String, Map<String, Object>> query(String collection, List<Condition> conditions) {
        Collection c = get(collection);
        for (Condition cond : conditions) {
            String type = c.schema().get(cond.field());
            if (type == null) {
                throw new IllegalArgumentException("Field \"" + cond.field() + "\" is not in the schema");
            }
            if (type.equals("Boolean") && cond.operator() != Condition.Operator.EQUALS) {
                throw new IllegalArgumentException("Range operator " + cond.operator() + " on Boolean field \"" + cond.field() + "\"");
            }
        }
        java.util.Collection<String> candidates = c.records().keySet();
        for (Condition cond : conditions) {
            Set<String> keys = candidates(c, cond);
            if (keys != null && keys.size() < candidates.size()) {
                candidates = keys;
            }
        }
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (String key : candidates) {
            Map<String, Object> value = c.records().get(key);
            if (conditions.stream().allMatch(cond -> matches(cond, value.get(cond.field())))) {
                result.put(key, new HashMap<>(value));
            }
        }
        return result;
    }

    /** The keys an index gives for a condition, or null if no index on the field can answer it. */
    private static Set<String> candidates(Collection c, Condition cond) {
        Map<Object, Set<String>> index = c.indexes().get(cond.field());
        Class<?> type = TYPES.get(c.schema().get(cond.field()));
        if (index == null) {
            return null;
        }
        if (!type.isInstance(cond.value()) || (cond.operator() == Condition.Operator.BETWEEN && !type.isInstance(cond.to()))) {
            return Set.of(); // a value of the wrong type matches nothing
        }
        if (cond.operator() == Condition.Operator.EQUALS) {
            return index.getOrDefault(cond.value(), Set.of());
        }
        if (!(index instanceof NavigableMap<Object, Set<String>> tree)) {
            return null; // a HASH index cannot answer a range
        }
        Map<Object, Set<String>> range = switch (cond.operator()) {
            case GREATER_THAN -> tree.tailMap(cond.value(), false);
            case GREATER_OR_EQUAL -> tree.tailMap(cond.value(), true);
            case LESS_THAN -> tree.headMap(cond.value(), false);
            case LESS_OR_EQUAL -> tree.headMap(cond.value(), true);
            case BETWEEN -> compare(cond.value(), cond.to()) > 0 ? Map.of() : tree.subMap(cond.value(), true, cond.to(), true);
            case EQUALS -> throw new IllegalStateException();
        };
        Set<String> keys = new HashSet<>();
        range.values().forEach(keys::addAll);
        return keys;
    }

    private static boolean matches(Condition cond, Object v) {
        if (v == null) {
            return false;
        }
        if (cond.operator() == Condition.Operator.EQUALS) {
            return v.equals(cond.value());
        }
        if (cond.value() == null || v.getClass() != cond.value().getClass()) {
            return false;
        }
        int cmp = compare(v, cond.value());
        return switch (cond.operator()) {
            case GREATER_THAN -> cmp > 0;
            case GREATER_OR_EQUAL -> cmp >= 0;
            case LESS_THAN -> cmp < 0;
            case LESS_OR_EQUAL -> cmp <= 0;
            case BETWEEN -> cmp >= 0 && cond.to() != null && v.getClass() == cond.to().getClass() && compare(v, cond.to()) <= 0;
            case EQUALS -> true;
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int compare(Object a, Object b) {
        return ((Comparable) a).compareTo(b);
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
