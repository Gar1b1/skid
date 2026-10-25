package com.matmon.skidb.demo;

import com.matmon.skidb.api.Condition;
import com.matmon.skidb.api.IndexType;
import com.matmon.skidb.api.SkiDBAdapter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Instructor-only reference adapter used to try out the UI.
 * Lives in test scope, so it is NOT packaged into the student JAR.
 */
public class InMemoryDemoAdapter implements SkiDBAdapter {

    private static final Map<String, Class<?>> TYPES = Map.of(
            "String", String.class, "Integer", Integer.class, "Boolean", Boolean.class, "DateTime", LocalDateTime.class);
    private static final DateTimeFormatter HOUR_ID = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH");

    /**
     * indexes: field -> (value -> keys); indexTypes: field -> HASH / TREE;
     * partitions: hour id -> keys (sorted, so ids in a time range are a subMap), when partitionField is set
     */
    private record Collection(Map<String, String> schema, Map<String, Map<String, Object>> records,
                              Map<String, Map<Object, Set<String>>> indexes, Map<String, IndexType> indexTypes,
                              String partitionField, TreeMap<String, Set<String>> partitions, List<String> lastScanned) {
    }

    private final Map<String, Collection> collections = new LinkedHashMap<>();

    @Override
    public void createCollection(String name, Map<String, String> schema) {
        create(name, schema, null);
    }

    @Override
    public void createPartitionedCollection(String name, Map<String, String> schema, String partitionField) {
        if (!"DateTime".equals(schema.get(partitionField))) {
            throw new IllegalArgumentException("Partition field \"" + partitionField + "\" must be a DateTime field of the schema");
        }
        create(name, schema, partitionField);
    }

    private void create(String name, Map<String, String> schema, String partitionField) {
        if (collections.containsKey(name)) {
            throw new IllegalArgumentException("Collection \"" + name + "\" already exists");
        }
        schema.forEach((field, type) -> {
            if (!TYPES.containsKey(type)) {
                throw new IllegalArgumentException("Unsupported type " + type + " for field " + field);
            }
        });
        collections.put(name, new Collection(new LinkedHashMap<>(schema), new LinkedHashMap<>(), new LinkedHashMap<>(),
                new LinkedHashMap<>(), partitionField, new TreeMap<>(), new ArrayList<>()));
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
        c.lastScanned().clear();
        if (c.partitionField() != null) {
            SortedMap<String, Set<String>> read = partitionsFor(c, conditions);
            c.lastScanned().addAll(read.keySet());
            Set<String> keys = new HashSet<>();
            read.values().forEach(keys::addAll);
            candidates = keys;
        }
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

    @Override
    public String getPartitionField(String collection) {
        return get(collection).partitionField();
    }

    @Override
    public Map<String, Set<String>> getPartitions(String collection) {
        return get(collection).partitions();
    }

    @Override
    public List<String> getScannedPartitions(String collection) {
        return new ArrayList<>(get(collection).lastScanned());
    }

    /** The partitions whose hour can satisfy every condition on the partition field: a subMap of the sorted ids. */
    private static SortedMap<String, Set<String>> partitionsFor(Collection c, List<Condition> conditions) {
        String low = null;
        String high = null;
        for (Condition cond : conditions) {
            if (!cond.field().equals(c.partitionField())) {
                continue;
            }
            if (!(cond.value() instanceof LocalDateTime v) || (cond.operator() == Condition.Operator.BETWEEN && !(cond.to() instanceof LocalDateTime))) {
                return new TreeMap<>(); // a value of the wrong type matches nothing
            }
            String from = null;
            String to = null;
            switch (cond.operator()) {
                case EQUALS -> from = to = hourId(v);
                case GREATER_THAN, GREATER_OR_EQUAL -> from = hourId(v);
                case LESS_THAN -> to = hourId(v.minusNanos(1));
                case LESS_OR_EQUAL -> to = hourId(v);
                case BETWEEN -> {
                    from = hourId(v);
                    to = hourId((LocalDateTime) cond.to());
                }
            }
            if (from != null && (low == null || from.compareTo(low) > 0)) {
                low = from;
            }
            if (to != null && (high == null || to.compareTo(high) < 0)) {
                high = to;
            }
        }
        if (low != null && high != null && low.compareTo(high) > 0) {
            return new TreeMap<>();
        }
        SortedMap<String, Set<String>> read = c.partitions();
        if (low != null) {
            read = read.tailMap(low);
        }
        if (high != null) {
            read = read.headMap(high + "\0"); // inclusive upper bound
        }
        return read;
    }

    private static String hourId(LocalDateTime t) {
        return t.truncatedTo(ChronoUnit.HOURS).format(HOUR_ID);
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
        if (c.partitionField() != null) {
            c.partitions().computeIfAbsent(hourId((LocalDateTime) value.get(c.partitionField())), p -> new HashSet<>()).add(key);
        }
        c.indexes().forEach((field, index) -> index.computeIfAbsent(value.get(field), v -> new HashSet<>()).add(key));
    }

    private static void unindex(Collection c, String key, Map<String, Object> value) {
        if (c.partitionField() != null) {
            String id = hourId((LocalDateTime) value.get(c.partitionField()));
            Set<String> keys = c.partitions().get(id);
            if (keys != null) {
                keys.remove(key);
                if (keys.isEmpty()) {
                    c.partitions().remove(id);
                }
            }
        }
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
