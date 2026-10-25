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
 * </ul>
 * The UI converts what the user types to these classes before calling you. It can also send a
 * value of the wrong class on purpose, so your DBMS must check every value against the schema.
 * <p>
 * Stage C adds queries by value and indexes. An index on a field maps each value of that field
 * to the keys of the records that hold it, for example on firstName: "Israel" -> {1, 4, 8}.
 * Every add, update and delete must keep the collection's indexes up to date.
 */
public interface SkiDBAdapter {

    /**
     * Creates a new, empty collection.
     *
     * @param name   unique collection name
     * @param schema field name -> type name ("String", "Integer" or "Boolean"), in display order
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
     * @param value the record content: field name -> value (a String, Integer or Boolean)
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

    // ---------------------------------------------------------------- stage C: indexes and queries

    /**
     * Creates an index on a field of an existing collection, built from the records already in it.
     * The UI also calls this right after {@link #createCollection} for fields the user chose to
     * index at creation time.
     *
     * @throws Exception if the collection does not exist, the field is not in the schema,
     *                   or the field is already indexed
     */
    void createIndex(String collection, String field) throws Exception;

    /**
     * @return the names of the indexed fields of the collection (empty if none, never null)
     * @throws Exception if the collection does not exist
     */
    Collection<String> getIndexedFields(String collection) throws Exception;

    /**
     * Returns the content of an index, so the UI can show it and check that it is up to date.
     *
     * @return field value -> keys of the records that hold this value,
     *         for example "Israel" -> {"1", "4", "8"}
     * @throws Exception if the collection does not exist or the field is not indexed
     */
    Map<?, ? extends Collection<String>> getIndex(String collection, String field) throws Exception;

    /**
     * Returns every record whose fields equal ALL the given conditions.
     * For example {"firstName" -> "Israel"} returns all records whose firstName is "Israel".
     * Use the indexes of the conditions' fields, so the query does not scan the whole collection.
     *
     * @param conditions field name -> required value (a String, Integer or Boolean)
     * @return key -> record value of each matching record (empty if none match)
     * @throws Exception if the collection does not exist or a condition's field is not in the schema
     */
    Map<String, Map<String, Object>> query(String collection, Map<String, Object> conditions) throws Exception;
}
