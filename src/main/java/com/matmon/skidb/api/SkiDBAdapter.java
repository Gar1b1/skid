package com.matmon.skidb.api;

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
 * Stage A: the only supported field type is {@code "String"}.
 */
public interface SkiDBAdapter {

    /**
     * Creates a new, empty collection.
     *
     * @param name   unique collection name
     * @param schema field name -> type name (stage A: always "String"), in display order
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
     * @param value the record content: field name -> value
     * @throws Exception if the collection does not exist, the key already exists,
     *                   or the value does not match the collection's schema
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
}
