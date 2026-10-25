package com.matmon.skidb.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The bridge between the SkiDB UI and your DBMS.
 * <p>
 * Implement this interface in ONE class that delegates to your own database design,
 * then start the UI with {@code SkiDBUI.launch(new MyAdapter(...))}.
 * <p>
 * Error handling: whenever an operation is invalid (missing collection, duplicate key,
 * value that does not match the schema, ...) throw an exception with a clear message.
 * The UI catches every exception and shows its class name and message to the user,
 * so feel free to throw your own exception types.
 * <p>
 * Stage B field types, and the Java class of their values:
 * <ul>
 *   <li>{@code "String"}  - {@link String}</li>
 *   <li>{@code "Integer"} - {@link Integer}</li>
 *   <li>{@code "Boolean"} - {@link Boolean}</li>
 *   <li>{@code "DateTime"} - {@link java.time.LocalDateTime} (stage F)</li>
 * </ul>
 * The UI converts what the user types to these classes before calling you. It can also send a
 * value of the wrong class on purpose, so your DBMS must check every value against the schema.
 * <p>
 * Queries and indexes: an index on a field maps each value of that field to the keys of the
 * records that hold it, for example on firstName: "Israel" -> {1, 4, 8}. Every add, update and
 * delete must keep the collection's indexes up to date. Stage E adds index types (HASH / TREE)
 * and range conditions ({@link Condition}).
 * <p>
 * Stage F adds partitions by time: a partitioned collection splits its records by the hour of
 * one DateTime field. The partition id is the hour written as {@code yyyy-MM-dd'T'HH}, for example
 * a record with {@code 2026-10-25T14:37} belongs to partition {@code "2026-10-25T14"}. A query with
 * a condition on that field should read only the partitions that can hold matching records.
 */
public interface SkiDBAdapter {

    /**
     * Creates a new, empty collection.
     *
     * @param name   unique collection name
     * @param schema field name -> type name ("String", "Integer", "Boolean" or "DateTime"), in display order
     * @throws Exception if a collection with this name already exists, or the schema is invalid
     */
    void createCollection(String name, Map<String, String> schema) throws Exception;

    /**
     * Deletes a collection and all of its records.
     *
     * @throws Exception if the collection does not exist
     */
    void deleteCollection(String name) throws Exception;

    /**
     * @return the names of all existing collections (empty list if there are none, never null)
     */
    List<String> getCollectionNames();

    /**
     * @return the schema of the collection: field name -> type name
     * @throws Exception if the collection does not exist
     */
    Map<String, String> getSchema(String collection) throws Exception;

    /**
     * Adds a new record to a collection.
     *
     * @param key   the record's unique identifier
     * @param value the record content: field name -> value (a String, Integer, Boolean or LocalDateTime)
     * @throws Exception if the collection does not exist, the key already exists,
     *                   or the value does not match the collection's schema (missing field,
     *                   field not in the schema, or a value of the wrong type)
     */
    void addRecord(String collection, String key, Map<String, Object> value) throws Exception;

    /**
     * Reads a single record by its key.
     *
     * @return the record's value: field name -> value
     * @throws Exception if the collection or the key does not exist
     */
    Map<String, Object> getRecord(String collection, String key) throws Exception;

    /**
     * Partially updates a record: only the fields present in {@code fields} change,
     * every other field keeps its current value.
     *
     * @param fields the fields to update: field name -> new value
     * @throws Exception if the collection or the key does not exist,
     *                   or the updated record would not match the schema
     */
    void updateRecord(String collection, String key, Map<String, Object> fields) throws Exception;

    /**
     * Deletes a record by its key.
     *
     * @throws Exception if the collection or the key does not exist
     */
    void deleteRecord(String collection, String key) throws Exception;

    /**
     * Returns every record of a collection - used by the UI to display the collection's content.
     *
     * @return key -> record value
     * @throws Exception if the collection does not exist
     */
    Map<String, Map<String, Object>> getAllRecords(String collection) throws Exception;

    // ---------------------------------------------------------------- indexes and queries

    /**
     * Creates an index on a field of an existing collection, built from the records already in it.
     * The UI also calls this right after {@link #createCollection} for fields the user chose to
     * index at creation time.
     *
     * @param type {@link IndexType#HASH} (for example a HashMap) or {@link IndexType#TREE} (for example a TreeMap)
     * @throws Exception if the collection does not exist, the field is not in the schema,
     *                   or the field is already indexed
     */
    void createIndex(String collection, String field, IndexType type) throws Exception;

    /**
     * @return every indexed field of the collection and its index type (empty if none, never null)
     * @throws Exception if the collection does not exist
     */
    Map<String, IndexType> getIndexes(String collection) throws Exception;

    /**
     * Returns the content of an index, so the UI can show it and check that it is up to date.
     * For a {@link IndexType#TREE} index this must be a {@link java.util.SortedMap} (for example a TreeMap).
     *
     * @return field value -> keys of the records that hold this value,
     *         for example "Israel" -> {"1", "4", "8"}
     * @throws Exception if the collection does not exist or the field is not indexed
     */
    Map<?, ? extends Collection<String>> getIndex(String collection, String field) throws Exception;

    /**
     * Returns every record that satisfies ALL the conditions, for example
     * [firstName = "Israel", age between 18 and 25].
     * Use the indexes of the conditions' fields so the query does not scan the whole collection:
     * any index helps an "=" condition, and a TREE index also helps range conditions.
     *
     * @return key -> record value of each matching record (empty if none match)
     * @throws Exception if the collection does not exist, a condition's field is not in the schema,
     *                   or a range operator is used on a Boolean field
     */
    Map<String, Map<String, Object>> query(String collection, List<Condition> conditions) throws Exception;

    // ---------------------------------------------------------------- stage F: partitions by time

    /**
     * Creates a new, empty collection whose records are partitioned by the hour of a DateTime field.
     *
     * @param schema         field name -> type name, as in {@link #createCollection}
     * @param partitionField the DateTime field to partition by
     * @throws Exception if a collection with this name already exists, the schema is invalid,
     *                   or the partition field is not a DateTime field of the schema
     */
    void createPartitionedCollection(String name, Map<String, String> schema, String partitionField) throws Exception;

    /**
     * @return the field the collection is partitioned by, or null if it is not partitioned
     * @throws Exception if the collection does not exist
     */
    String getPartitionField(String collection) throws Exception;

    /**
     * Returns the collection's partitions, so the UI can show them and check that every record is
     * in the right one.
     *
     * @return partition id (for example "2026-10-25T14") -> keys of the records in it;
     *         empty if the collection is not partitioned
     * @throws Exception if the collection does not exist
     */
    Map<String, ? extends Collection<String>> getPartitions(String collection) throws Exception;

    /**
     * Returns the partitions that the most recent {@link #query} on this collection read, so the UI
     * can check that the query skipped the partitions that cannot match.
     *
     * @return the ids of the partitions the last query read; empty if the collection is not partitioned
     * @throws Exception if the collection does not exist
     */
    Collection<String> getScannedPartitions(String collection) throws Exception;
}
