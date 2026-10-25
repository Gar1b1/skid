# SkiDB UI — מדריך לחניך

המדריך מסביר איך לחבר את ה-DBMS **שלכם** (SkiDB – שלב א) לממשק הגרפי (UI) המוכן.

אתם **לא** כותבים קוד UI. אתם כותבים מחלקה אחת קטנה, ה-*Adapter*, שמעבירה את הבקשות מה-UI אל ה-DBMS שלכם.

```
 ┌──────────────┐   calls    ┌──────────────┐   calls    ┌──────────────────┐
 │   SkiDB UI   │ ─────────▶ │  MyAdapter   │ ─────────▶ │  your DBMS code  │
 │  (the JAR)   │            │ (you write)  │            │   (your design)  │
 └──────────────┘            └──────────────┘            └──────────────────┘
```

---

## 0. דרישות

- **Java 21** ומעלה. בדקו עם `java -version`.
- סביבת פיתוח כלשהי: IntelliJ IDEA, VS Code עם תוספי Java, או Eclipse.

---

## 1. הורדת ה-JAR

הורידו את שני הקבצים האלה מתיקיית `dist/` שבריפו:

| קובץ | מה זה |
|---|---|
| [`skidb-ui-1.0.jar`](../dist/skidb-ui-1.0.jar) | ספריית ה-UI. **חובה.** |
| [`skidb-ui-1.0-sources.jar`](../dist/skidb-ui-1.0-sources.jar) | קוד המקור. רשות; בזכותו סביבת הפיתוח מציגה את התיעוד של כל מתודה. |

קישורים ישירים:
- https://github.com/Gar1b1/skid/raw/main/dist/skidb-ui-1.0.jar
- https://github.com/Gar1b1/skid/raw/main/dist/skidb-ui-1.0-sources.jar

צרו בפרויקט שלכם תיקייה בשם `lib/` ושימו בה את קובצי ה-JAR:

```
my-skidb-project/
├── lib/
│   ├── skidb-ui-1.0.jar
│   └── skidb-ui-1.0-sources.jar
└── src/
    └── ...your code...
```

---

## 2. הוספת ה-JAR לפרויקט

בחרו את הסעיף שמתאים לאופן שבו הפרויקט שלכם בנוי.

### IntelliJ IDEA (פרויקט רגיל, בלי Maven/Gradle)

1. **File → Project Structure…** (`Ctrl+Alt+Shift+S` ב-Windows, `⌘;` ב-Mac)
2. **Libraries → `+` → Java**
3. בחרו את `lib/skidb-ui-1.0.jar` ולחצו **OK**. כשתישאלו, הוסיפו אותו למודול שלכם.
4. רשות: באותה ספרייה לחצו **`+`** והוסיפו את `lib/skidb-ui-1.0-sources.jar`, כדי שתוכלו לקרוא את התיעוד.
5. לחצו **Apply → OK**.

### VS Code (עם תוספי Java)

1. פתחו את חלונית **Explorer** ופתחו את **JAVA PROJECTS** בתחתית.
2. ליד **Referenced Libraries** לחצו **`+`**.
3. בחרו את `lib/skidb-ui-1.0.jar`.

בפרויקט VS Code בלי כלי בנייה, קבצים בתיקייה `lib/` מתווספים אוטומטית, כך שייתכן שהשלב הזה כבר בוצע.

### פרויקט Maven

התקינו את ה-JAR פעם אחת ב-Maven המקומי (הריצו בתיקיית הפרויקט):

```bash
mvn install:install-file -Dfile=lib/skidb-ui-1.0.jar -Dsources=lib/skidb-ui-1.0-sources.jar \
    -DgroupId=com.matmon -DartifactId=skidb-ui -Dversion=1.0 -Dpackaging=jar
```

ואז הוסיפו ל-`pom.xml`, בתוך `<dependencies>`:

```xml
<dependency>
    <groupId>com.matmon</groupId>
    <artifactId>skidb-ui</artifactId>
    <version>1.0</version>
</dependency>
```

### פרויקט Gradle

ב-`build.gradle`:

```groovy
dependencies {
    implementation files('lib/skidb-ui-1.0.jar')
}
```

### שורת פקודה (בלי סביבת פיתוח)

```bash
# compile
javac -cp lib/skidb-ui-1.0.jar -d out $(find src -name "*.java")
# run   (on Windows use ; instead of : )
java -cp lib/skidb-ui-1.0.jar:out Main
```

---

## 3. כתיבת ה-Adapter

צרו מחלקה שמממשת את `com.matmon.skidb.api.SkiDBAdapter`. כל מתודה צריכה **לקרוא לקוד ה-DBMS שלכם**. אל תכתבו את הלוגיקה של מסד הנתונים בתוך ה-Adapter עצמו.

העתיקו את התבנית הזאת והשלימו כל `TODO`:

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

אפשר להריץ את ה-UI כבר עם התבנית הזאת. כל כפתור יציג "TODO ..." עד שתממשו את המתודה שלו, כך שאפשר לעבוד על מתודה אחת בכל פעם.

### מה כל מתודה צריכה לעשות

| מתודה | צריכה לעשות | צריכה לזרוק חריגה כאשר |
|---|---|---|
| `createCollection` | ליצור אוסף ריק עם הסכמה הזאת | השם כבר קיים |
| `deleteCollection` | למחוק את האוסף ואת כל הרשומות שבו | האוסף לא קיים |
| `getCollectionNames` | להחזיר את שמות כל האוספים (אף פעם לא `null`) | — |
| `getSchema` | להחזיר שם שדה → טיפוס | האוסף לא קיים |
| `addRecord` | להוסיף רשומה חדשה | האוסף לא קיים, ה-key כבר קיים, או שה-value לא תואם לסכמה |
| `getRecord` | להחזיר את ה-value של הרשומה | האוסף או ה-key לא קיימים |
| `updateRecord` | לשנות **רק** את השדות שנשלחו, ולהשאיר את השאר ללא שינוי | האוסף או ה-key לא קיימים, או שהתוצאה לא תואמת לסכמה |
| `deleteRecord` | למחוק את הרשומה | האוסף או ה-key לא קיימים |
| `getAllRecords` | להחזיר את כל הרשומות: key → value | האוסף לא קיים |

### שגיאות

כשמשהו לא תקין, **זרקו חריגה (Exception) עם הודעה ברורה**, לדוגמה:

```java
throw new IllegalArgumentException("Field \"age\" is not in the schema");
```

גם מחלקות חריגה שלכם, כמו `SchemaMismatchException`, עובדות. ה-UI מציג את סוג החריגה, את ההודעה שלה ואת השורה בקוד שלכם שזרקה אותה.

---

## 4. הפעלת ה-UI מתוך `main`

```java
import com.matmon.skidb.ui.SkiDBUI;

public class Main {
    public static void main(String[] args) {
        MyDatabase db = new MyDatabase();      // your DBMS
        SkiDBUI.launch(new MyAdapter(db));     // opens the UI window
    }
}
```

הריצו את `Main`. חלון SkiDB נפתח, וסגירת החלון מסיימת את התוכנית.

---

## 5. שימוש ב-UI

| אזור | מה אפשר לעשות |
|---|---|
| **Collections** (שמאל) | **+ New** יוצר אוסף: הזינו שם ואת השדות שלו. **Delete** מוחק את האוסף המסומן. |
| **טבלת האוסף** (מרכז) | מציגה את הסכמה ואת כל הרשומות. **לחיצה על שורה** טוענת אותה לטופס. |
| **Record** (ימין) | הזינו **key** וערכים לשדות, ולחצו **Add / Get / Update / Delete**. |
| **+ Extra field** | שולח שדה שאינו בסכמה, כדי לבדוק שה-DBMS שלכם דוחה אותו. |
| **Operation log** (למטה) | כל פעולה והתוצאה שלה: ✔ הצלחה, ✖ כישלון. |
| **Print database** (למעלה) | כותב ללוג את כל מבנה מסד הנתונים. |

חשוב:
- **שדות ריקים לא נשלחים.** כדי לבדוק עדכון חלקי, מלאו רק את השדה שאתם רוצים לשנות ולחצו **Update**.
- עמודה בטבלה שמוצגת **באדום עם ⚠** היא שדה שאינו בסכמה. הבדיקה שלכם נתנה לו לעבור; תקנו אותה!

---

## 6. בדקו את העבודה שלכם

עברו על הרשימה. כל שורה צריכה להתנהג כפי שמתואר.

- [ ] יצירת אוסף `matmonDB` עם השדות `firstName`, `lastName` → הוא מופיע ברשימה
- [ ] יצירת `matmonDB` שוב → **שגיאה**
- [ ] הוספת key `1` עם `Israel` / `Israeli` → הרשומה מופיעה בטבלה
- [ ] הוספת key `1` שוב → **שגיאה** (key כפול)
- [ ] הוספת key `2` עם **שדה נוסף** `age` → **שגיאה** (לא בסכמה)
- [ ] הוספת key `3` כשרק `firstName` מלא → **שגיאה** (לא תואם לסכמה)
- [ ] Get ל-key `1` → הטופס מתמלא ב-`Israel` / `Israeli`
- [ ] Get ל-key `99` → **שגיאה** (ה-key לא קיים)
- [ ] Update ל-key `1` כשרק firstName = `Moshe` → בטבלה מופיע `Moshe` / `Israeli` (lastName לא השתנה)
- [ ] מחיקת key `1` → הרשומה נעלמת מהטבלה
- [ ] מחיקת האוסף `matmonDB` → הוא נעלם מהרשימה

---

## 7. פתרון בעיות

| בעיה | פתרון |
|---|---|
| `UnsupportedClassVersionError ... class file version 65.0` | גרסת ה-Java שלכם ישנה מ-21. התקינו JDK 21 ובחרו בו בסביבת הפיתוח (Project Structure → SDK). |
| `package com.matmon.skidb.api does not exist` / `ClassNotFoundException` | ה-JAR לא נמצא ב-classpath. חזרו על שלב 2. |
| כפתור מציג `UnsupportedOperationException: TODO ...` | עוד לא מימשתם את המתודה הזאת ב-Adapter. |
| כפתור מציג `NullPointerException` | יש באג בקוד שלכם. השגיאה מציגה את הקובץ והשורה שבהם הוא קרה. |
| ה-UI מציג `the adapter returned null` | מתודה החזירה `null`. החזירו רשימה/מפה ריקה, או זרקו חריגה. |
| הטבלה לא משתנה אחרי פעולה | בדקו ש-`getAllRecords` מחזירה את הנתונים העדכניים ולא עותק ישן. |
