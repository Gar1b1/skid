# SkiDB UI — שלב ה: index מסוג TreeMap ושליפה לפי טווח

בשלב ה אפשר לבחור לכל index את **סוג מבנה הנתונים** שלו:

| סוג | מבנה נתונים | חיפוש ערך | מה הוא יודע לענות |
|---|---|---|---|
| `HASH` | `HashMap` | `O(1)` | רק `=` |
| `TREE` | `TreeMap` | `O(log n)` | `=` וגם **טווחים**: `>`, `>=`, `<`, `<=`, `between` |

לשלב הזה יש JAR חדש: **skidb-ui-5.0.jar**.

> **שלב ד:** אין בו UI חדש. בשלב ד המשיכו לעבוד עם `skidb-ui-3.0.jar` ועם [מדריך שלב ג](STUDENT_GUIDE_STAGE_C.md).

ההתקנה זהה לשלבים הקודמים. אם עוד לא קראתם, התחילו מ[מדריך החניך](STUDENT_GUIDE.md).

---

## 1. מעבר מ-3.0 ל-5.0

1. הורידו את הקבצים החדשים:
   - https://github.com/Gar1b1/skid/raw/main/dist/skidb-ui-5.0.jar
   - https://github.com/Gar1b1/skid/raw/main/dist/skidb-ui-5.0-sources.jar (רשות)
2. בתיקייה `lib/` החליפו את `skidb-ui-3.0.jar` ב-`skidb-ui-5.0.jar`, ומחקו את הישן.
3. עדכנו את הספרייה בסביבת הפיתוח. ב-Maven שנו את הגרסה ל-`5.0`.

**שימו לב:** שלוש מתודות בממשק `SkiDBAdapter` השתנו, ולכן ה-Adapter שלכם **לא יתקמפל** עד שתעדכנו אותן:

| שלב ג (3.0) | שלב ה (5.0) |
|---|---|
| `createIndex(collection, field)` | `createIndex(collection, field, IndexType type)` |
| `Collection<String> getIndexedFields(collection)` | `Map<String, IndexType> getIndexes(collection)` |
| `query(collection, Map<String, Object> conditions)` | `query(collection, List<Condition> conditions)` |

`getIndex` נשאר כמו שהוא, אבל ל-index מסוג `TREE` הוא חייב להחזיר `SortedMap`, כלומר `TreeMap`.

---

## 2. המחלקות החדשות ב-JAR

שתיהן נמצאות בחבילה `com.matmon.skidb.api`. אלה מחלקות **נתונים** בלבד: את הלוגיקה של השליפה אתם כותבים.

### `IndexType`

```java
public enum IndexType { HASH, TREE }
```

### `Condition`

תנאי אחד על שדה. שליפה מחזירה את הרשומות שמקיימות **את כל** התנאים.

```java
public record Condition(String field, Operator operator, Object value, Object to)
```

| מתודה | מה היא מחזירה |
|---|---|
| `field()` | שם השדה, למשל `"age"` |
| `operator()` | האופרטור, ראו בטבלה למטה |
| `value()` | הערך להשוואה. ב-`BETWEEN` זה הגבול התחתון |
| `to()` | רק ב-`BETWEEN`: הגבול העליון. בכל אופרטור אחר `null` |

| `Operator` | משמעות | דוגמה |
|---|---|---|
| `EQUALS` | `field = value` | `firstName = "Israel"` |
| `GREATER_THAN` | `field > value` | `age > 20` |
| `GREATER_OR_EQUAL` | `field >= value` | `age >= 18` |
| `LESS_THAN` | `field < value` | `age < 30` |
| `LESS_OR_EQUAL` | `field <= value` | `age <= 25` |
| `BETWEEN` | `value <= field <= to` (כולל שני הגבולות) | `age between 18 and 25` |

**כללי השוואה:**
- `Integer`: לפי ערך מספרי.
- `String`: לפי סדר אלפביתי (`String.compareTo`). למשל `"Dana" < "Israel"`.
- `Boolean`: רק `EQUALS`. אופרטור טווח על שדה Boolean צריך לזרוק חריגה.
- ערך מטיפוס אחר מזה של השדה, למשל `age > "20"` (String), לא מתאים לאף רשומה.

---

## 3. עדכון ה-Adapter

החליפו במחלקה שלכם את שלוש המתודות הישנות בחדשות. אפשר להתחיל עם `TODO`:

```java
import com.matmon.skidb.api.Condition;
import com.matmon.skidb.api.IndexType;
import java.util.Collection;

    @Override
    public void createIndex(String collection, String field, IndexType type) throws Exception {
        // HASH -> e.g. a HashMap, TREE -> a TreeMap; build it from the records already in the collection
        throw new UnsupportedOperationException("TODO createIndex");
    }

    @Override
    public Map<String, IndexType> getIndexes(String collection) throws Exception {
        // e.g. {"firstName" -> HASH, "age" -> TREE}
        throw new UnsupportedOperationException("TODO getIndexes");
    }

    @Override
    public Map<?, ? extends Collection<String>> getIndex(String collection, String field) throws Exception {
        // value -> keys; for a TREE index return the TreeMap itself (a SortedMap)
        throw new UnsupportedOperationException("TODO getIndex");
    }

    @Override
    public Map<String, Map<String, Object>> query(String collection, List<Condition> conditions) throws Exception {
        // e.g. [age between 18 and 25, firstName = "Israel"]: records that satisfy ALL conditions
        throw new UnsupportedOperationException("TODO query");
    }
```

| מתודה | צריכה לעשות | צריכה לזרוק חריגה כאשר |
|---|---|---|
| `createIndex` | ליצור index מהסוג שנבחר, כולל הרשומות שכבר באוסף | האוסף לא קיים, השדה לא בסכמה, או שכבר יש עליו index |
| `getIndexes` | להחזיר כל שדה מאונדקס ואת סוג ה-index שלו | האוסף לא קיים |
| `getIndex` | להחזיר את תוכן ה-index: ערך → keys. ב-`TREE` חייב להיות `SortedMap` | האוסף לא קיים, או שאין index על השדה |
| `query` | להחזיר את כל הרשומות שמקיימות **את כל** התנאים | האוסף לא קיים, שדה בתנאי לא בסכמה, או אופרטור טווח על שדה Boolean |

---

## 4. דברים שחייבים לזכור

- **השתמשו ב-index הנכון.** תנאי `=` יכול להשתמש בכל index. תנאי טווח יכול להשתמש רק ב-index מסוג `TREE`. חפשו ב-`TreeMap` מתודות שמחזירות חלק מהמפה לפי טווח של מפתחות.
- **בלי index מתאים** (אין index על השדה, או index מסוג HASH עם תנאי טווח), השליפה עדיין חייבת להחזיר תוצאה נכונה. היא פשוט תהיה איטית יותר.
- **כמה תנאים:** חשבו מאיזה תנאי כדאי להתחיל כדי לבדוק כמה שפחות רשומות.
- **תחזוקה:** בדיוק כמו בשלב ג, כל `addRecord`, `updateRecord` ו-`deleteRecord` חייבים לעדכן את כל ה-indexes, משני הסוגים.

---

## 5. מה חדש ב-UI

- **יצירת אוסף:** ליד כל שדה יש בחירה של `No index` / `HASH` / `TREE`.
- **לשונית Indexes:** יוצרים index עם בחירת סוג (**of type HASH / TREE**). כל index מוצג עם הסוג שלו. index מסוג TREE שאינו `SortedMap` מסומן באדום: `wrong data structure`.
- **לשונית Query:** לכל תנאי יש בחירת אופרטור (`=`, `>`, `>=`, `<`, `<=`, `between`). ב-`between` מופיעה תיבה שנייה לגבול העליון. בשדה Boolean אפשר לבחור רק `=`. מתחת לכל תנאי מוצג באיזה index הוא יכול להשתמש: `TREE index`, `HASH index — can't help a range`, או `not indexed`.
- **בדיקת התוצאה:** כמו בשלב ג, כל תוצאה מושווית לסריקה מלאה של האוסף. רשומות חסרות או מיותרות מסומנות באדום.

---

## 6. בדקו את העבודה שלכם

- [ ] יצירת אוסף עם `firstName: String` (HASH), `age: Integer` (TREE), `isCommander: Boolean` (בלי index) → בשורת הסכמה: `(HASH index)`, `(TREE index)`
- [ ] **Generate data…** עם 1,000 רשומות → בלשונית Indexes שני ה-indexes ✔ in sync
- [ ] Query: `age between 20 and 30` → ✔ Same records as a full scan, ומתחת לתנאי `TREE index`
- [ ] Query: `age > 60`, `age >= 60`, `age < 20`, `age <= 20` → כולם ✔ Same records as a full scan
- [ ] Query: `age between 30 and 20` (גבולות הפוכים) → 0 תוצאות, בלי קריסה
- [ ] Query: `firstName between "A" and "D"` → רק שמות שמתחילים ב-A עד C (ו-`"D"` עצמו, אם קיים)
- [ ] Query עם שני תנאים: `age >= 25` וגם `firstName = Israel` → ✔ Same records as a full scan
- [ ] Query: `firstName > "M"` כשעל firstName יש index מסוג HASH → תוצאה נכונה (מתחת לתנאי: `HASH index — can't help a range`)
- [ ] Query: `age > 20` כשהטיפוס הוא `String` (כלומר `"20"`) → 0 תוצאות
- [ ] Update ו-Delete לכמה רשומות → שני ה-indexes עדיין ✔ in sync
- [ ] יצירת index מסוג TREE על `isCommander` (Boolean) → עובד, ו-✔ in sync
- [ ] **Generate data…** עם 100,000 רשומות: `age between 30 and 31` עם TREE index על age מהיר בהרבה מאותה שליפה בלי index

---

## 7. פתרון בעיות

| בעיה | פתרון |
|---|---|
| ה-Adapter לא מתקמפל: `does not override ... createIndex(String,String,IndexType)` | עדכנו את שלוש המתודות שבסעיף 1. |
| `cannot find symbol: class Condition` / `IndexType` | הוסיפו `import com.matmon.skidb.api.Condition;` ו-`import com.matmon.skidb.api.IndexType;`. |
| ב-Indexes: `wrong data structure — a TREE index must be a SortedMap such as TreeMap` | ה-index מסוג TREE ממומש עם `HashMap`. השתמשו ב-`TreeMap`, והחזירו אותו מ-`getIndex`. |
| `ClassCastException` בשליפת טווח | השוויתם ערכים מטיפוסים שונים, למשל `Integer` מול `String`. בדקו קודם שהטיפוס של `value()` מתאים לטיפוס השדה. |
| `IllegalArgumentException: fromKey > toKey` | ב-`between` הגבול התחתון גדול מהעליון. במקרה כזה החזירו תוצאה ריקה. |
| `missing keys` / `unexpected keys` | בדקו אם הגבולות כלולים: `>` לעומת `>=`, ו-`between` כולל את שני הגבולות. |
| הכותרת בחלון היא `Stage C` | אתם עדיין מריצים JAR ישן. ודאו שרק `skidb-ui-5.0.jar` נמצא בספריות של הפרויקט. |
