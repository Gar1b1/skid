# SkiDB UI — Student Guide

This guide explains how to connect **your** SkiDB (Stage A) DBMS to the ready-made SkiDB UI.

You do **not** write any UI code. You write one small class, the *adapter*, that passes the UI's requests to your DBMS.

```
 ┌──────────────┐   calls    ┌──────────────┐   calls    ┌──────────────────┐
 │   SkiDB UI   │ ─────────▶ │  MyAdapter   │ ─────────▶ │  your DBMS code  │
 │  (the JAR)   │            │ (you write)  │            │   (your design)  │
 └──────────────┘            └──────────────┘            └──────────────────┘
```

---

## 0. Requirements

- **Java 21** or newer. Check with `java -version`.
- Any IDE: IntelliJ IDEA, VS Code with the Java extensions, or Eclipse.

---

## 1. Download the JAR

Download these two files from the repository's `dist/` folder:

| File | What it is |
|---|---|
| [`skidb-ui-1.0.jar`](../dist/skidb-ui-1.0.jar) | The UI library. **Required.** |
| [`skidb-ui-1.0-sources.jar`](../dist/skidb-ui-1.0-sources.jar) | The source code. Optional; it makes your IDE show the documentation of each method. |

Direct links:
- https://github.com/Gar1b1/skid/raw/main/dist/skidb-ui-1.0.jar
- https://github.com/Gar1b1/skid/raw/main/dist/skidb-ui-1.0-sources.jar

Inside your project, create a folder called `lib/` and put the JAR(s) in it:

```
my-skidb-project/
├── lib/
│   ├── skidb-ui-1.0.jar
│   └── skidb-ui-1.0-sources.jar
└── src/
    └── ...your code...
```

---

## 2. Add the JAR to your project

Pick the section that matches how your project is set up.

### IntelliJ IDEA (regular project, no Maven/Gradle)

1. **File → Project Structure…** (`Ctrl+Alt+Shift+S` on Windows, `⌘;` on Mac)
2. **Libraries → `+` → Java**
3. Select `lib/skidb-ui-1.0.jar` and press **OK**. When asked, add it to your module.
4. Optional: in the same library, press **`+`** and add `lib/skidb-ui-1.0-sources.jar` so you can read the documentation.
5. Press **Apply → OK**.

### VS Code (Java extensions)

1. Open the **Explorer** panel and expand **JAVA PROJECTS** at the bottom.
2. Next to **Referenced Libraries**, press **`+`**.
3. Select `lib/skidb-ui-1.0.jar`.

Unmanaged VS Code projects already include `lib/**/*.jar` automatically, so this step may already be done for you.

### Maven project

Install the JAR into your local Maven repository once (run this in the folder that holds the JAR):

```bash
mvn install:install-file -Dfile=lib/skidb-ui-1.0.jar -Dsources=lib/skidb-ui-1.0-sources.jar \
    -DgroupId=com.matmon -DartifactId=skidb-ui -Dversion=1.0 -Dpackaging=jar
```

Then add this to your `pom.xml` inside `<dependencies>`:

```xml
<dependency>
    <groupId>com.matmon</groupId>
    <artifactId>skidb-ui</artifactId>
    <version>1.0</version>
</dependency>
```

### Gradle project

In `build.gradle`:

```groovy
dependencies {
    implementation files('lib/skidb-ui-1.0.jar')
}
```

### Command line (no IDE)

```bash
# compile
javac -cp lib/skidb-ui-1.0.jar -d out $(find src -name "*.java")
# run   (on Windows use ; instead of : )
java -cp lib/skidb-ui-1.0.jar:out Main
```

---

## 3. Write your adapter

Create a class that implements `com.matmon.skidb.api.SkiDBAdapter`. Each method should **call your own DBMS code**; don't put the database logic in the adapter itself.

Copy this template and fill in each `TODO`:

```java
import com.matmon.skidb.api.SkiDBAdapter;

import java.util.List;
import java.util.Map;

public class MyAdapter implements SkiDBAdapter {

    // TODO: replace with your own database class
    private final MyDatabase db;

    public MyAdapter(MyDatabase db) {
        this.db = db;
    }

    @Override
    public void createCollection(String name, Map<String, String> schema) throws Exception {
        // schema example: {"firstName" -> "String", "lastName" -> "String"}
        throw new UnsupportedOperationException("TODO createCollection");
    }

    @Override
    public void deleteCollection(String name) throws Exception {
        throw new UnsupportedOperationException("TODO deleteCollection");
    }

    @Override
    public List<String> getCollectionNames() {
        throw new UnsupportedOperationException("TODO getCollectionNames");
    }

    @Override
    public Map<String, String> getSchema(String collection) throws Exception {
        throw new UnsupportedOperationException("TODO getSchema");
    }

    @Override
    public void addRecord(String collection, String key, Map<String, Object> value) throws Exception {
        // value example: {"firstName" -> "Israel", "lastName" -> "Israeli"}
        throw new UnsupportedOperationException("TODO addRecord");
    }

    @Override
    public Map<String, Object> getRecord(String collection, String key) throws Exception {
        throw new UnsupportedOperationException("TODO getRecord");
    }

    @Override
    public void updateRecord(String collection, String key, Map<String, Object> fields) throws Exception {
        // fields contains ONLY the fields to change, e.g. {"firstName" -> "Moshe"}
        throw new UnsupportedOperationException("TODO updateRecord");
    }

    @Override
    public void deleteRecord(String collection, String key) throws Exception {
        throw new UnsupportedOperationException("TODO deleteRecord");
    }

    @Override
    public Map<String, Map<String, Object>> getAllRecords(String collection) throws Exception {
        // key -> record value, e.g. {"1" -> {"firstName" -> "Israel", "lastName" -> "Israeli"}}
        throw new UnsupportedOperationException("TODO getAllRecords");
    }
}
```

You can run the UI right away with this template. Every button will show "TODO ..." until you implement that method, so you can work on one method at a time.

### What each method must do

| Method | Must do | Must throw an exception when |
|---|---|---|
| `createCollection` | Create an empty collection with this schema | The name already exists |
| `deleteCollection` | Remove the collection and all its records | The collection does not exist |
| `getCollectionNames` | Return all collection names (never `null`) | — |
| `getSchema` | Return field name → type | The collection does not exist |
| `addRecord` | Add a new record | The collection is missing, the key already exists, or the value does not match the schema |
| `getRecord` | Return the record's value | The collection or the key does not exist |
| `updateRecord` | Change **only** the given fields and keep the rest | The collection or the key does not exist, or the result does not match the schema |
| `deleteRecord` | Remove the record | The collection or the key does not exist |
| `getAllRecords` | Return every record: key → value | The collection does not exist |

### Errors

When something is invalid, **throw an exception with a clear message**, for example:

```java
throw new IllegalArgumentException("Field \"age\" is not in the schema");
```

Your own exception classes, such as `SchemaMismatchException`, work too. The UI shows the exception type, its message, and the line in your code that threw it.

---

## 4. Launch the UI from `main`

```java
import com.matmon.skidb.ui.SkiDBUI;

public class Main {
    public static void main(String[] args) {
        MyDatabase db = new MyDatabase();      // your DBMS
        SkiDBUI.launch(new MyAdapter(db));     // opens the UI window
    }
}
```

Run `Main`. The SkiDB window opens, and closing it ends the program.

---

## 5. Using the UI

| Area | What you can do |
|---|---|
| **Collections** (left) | **+ New** creates a collection: enter a name and its fields. **Delete** removes the selected one. |
| **Collection table** (center) | Shows the schema and all records. **Click a row** to load it into the form. |
| **Record form** (right) | Enter a **key** and field values, then press **Add / Get / Update / Delete**. |
| **+ Extra field** | Sends a field that is **not** in the schema, to check that your DBMS rejects it. |
| **Operation log** (bottom) | Every operation and its result: ✔ success, ✖ failure. |
| **Print database** (top) | Writes the whole database structure to the log. |

Important:
- **Empty fields are not sent.** To test a partial update, fill in only the field you want to change and press **Update**.
- A table column shown in **red with ⚠** is a field that is not in the schema. Your validation let it through; fix it!

---

## 6. Check your work

Run through this list. Every line should behave as described.

- [ ] Create collection `matmonDB` with fields `firstName`, `lastName` → it appears in the list
- [ ] Create `matmonDB` again → **error**
- [ ] Add key `1` with `Israel` / `Israeli` → it appears in the table
- [ ] Add key `1` again → **error** (duplicate key)
- [ ] Add key `2` with an **extra field** `age` → **error** (not in schema)
- [ ] Add key `3` with only `firstName` filled → **error** (does not match the schema)
- [ ] Get key `1` → the form fills with `Israel` / `Israeli`
- [ ] Get key `99` → **error** (key does not exist)
- [ ] Update key `1` with only firstName = `Moshe` → the table shows `Moshe` / `Israeli` (lastName unchanged)
- [ ] Delete key `1` → it disappears from the table
- [ ] Delete collection `matmonDB` → it disappears from the list

---

## 7. Troubleshooting

| Problem | Solution |
|---|---|
| `UnsupportedClassVersionError ... class file version 65.0` | Your Java is older than 21. Install JDK 21 and select it in your IDE (Project Structure → SDK). |
| `package com.matmon.skidb.api does not exist` / `ClassNotFoundException` | The JAR is not on your classpath. Repeat step 2. |
| A button shows `UnsupportedOperationException: TODO ...` | You haven't implemented that adapter method yet. |
| A button shows `NullPointerException` | Your code has a bug. The error shows the file and line where it happened. |
| The UI shows `the adapter returned null` | A method returned `null`. Return an empty list/map, or throw an exception. |
| The table doesn't change after an operation | Check that `getAllRecords` returns the current data, not an old copy. |
