# SkiDB UI

A Swing UI for the **SkiDB – Stage A** exercise. You build the DBMS; this UI lets you create collections, add / read / update / delete records, and see the whole database while you work.

The UI never touches your internals. It talks to your DBMS only through one interface, `SkiDBAdapter`, so your design stays your own.

## 1. Get the JAR

Requirements: Java 21+ and Maven.

```bash
git clone https://github.com/Gar1b1/skid.git
cd skid
mvn install        # builds target/skidb-ui-1.0.jar and installs it to your local Maven repo
```

## 2. Add it to your project

**Maven** (after `mvn install` above):

```xml
<dependency>
    <groupId>com.matmon</groupId>
    <artifactId>skidb-ui</artifactId>
    <version>1.0</version>
</dependency>
```

**IntelliJ without Maven:** File → Project Structure → Libraries → `+` → Java → pick `target/skidb-ui-1.0.jar`.

## 3. Implement the adapter

Write one class that implements `com.matmon.skidb.api.SkiDBAdapter` and delegates to your own classes:

```java
import com.matmon.skidb.api.SkiDBAdapter;

public class MyAdapter implements SkiDBAdapter {

    private final MyDatabase db;   // your design

    public MyAdapter(MyDatabase db) {
        this.db = db;
    }

    @Override
    public void createCollection(String name, Map<String, String> schema) throws Exception {
        // call your code here
    }

    // ... the other methods
}
```

| Method | What it should do |
|---|---|
| `createCollection(name, schema)` | Create an empty collection. `schema` is field name → type (stage A: always `"String"`). |
| `deleteCollection(name)` | Delete a collection and its records. |
| `getCollectionNames()` | Return the names of all collections. |
| `getSchema(collection)` | Return the collection's schema. |
| `addRecord(collection, key, value)` | Add a new record. Reject it if it does not match the schema. |
| `getRecord(collection, key)` | Return one record by key. |
| `updateRecord(collection, key, fields)` | Change **only** the given fields; all other fields keep their values. |
| `deleteRecord(collection, key)` | Delete one record by key. |
| `getAllRecords(collection)` | Return key → value for every record (used to display the collection). |

**Errors:** when an operation is invalid (missing collection, duplicate key, field not in schema, ...) **throw an exception with a clear message**. The UI shows the exception type and message, so your own exception classes work fine. If a method is not implemented yet, throw `UnsupportedOperationException`. The UI stays open and logs the failure.

## 4. Launch

```java
import com.matmon.skidb.ui.SkiDBUI;

public class Main {
    public static void main(String[] args) {
        SkiDBUI.launch(new MyAdapter(new MyDatabase()));
    }
}
```

## Using the UI

- **Collections** (left): **+ New** opens a dialog for the name and schema fields. **Delete** removes the selected collection.
- **Collection table** (center): shows the schema and every record. Click a row to load it into the form. A column marked **⚠ in red** is a field that is **not in the schema**, which means your validation let it through.
- **Record form** (right): enter a key and the field values, then press **Add / Get / Update / Delete**.
  - Empty fields are **not sent**. To test a partial update, fill in only the fields you want to change.
  - **+ Extra field** sends a field that is not in the schema. Your DBMS should reject it.
- **Operation log** (bottom): every call and its result. **Print database** writes the full database structure to the log.

---

## Instructor: run the demo

`src/test/java/com/matmon/skidb/demo` holds a reference adapter and `DemoLauncher`. It opens the UI pre-filled with the exam's example collection. It is test-scope only and is **not** packaged into the JAR.

```bash
mvn test-compile exec:java -Dexec.mainClass=com.matmon.skidb.demo.DemoLauncher -Dexec.classpathScope=test
```

Or open the project in IntelliJ and run `DemoLauncher.main`.
