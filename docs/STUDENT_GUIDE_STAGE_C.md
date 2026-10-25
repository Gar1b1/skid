# SkiDB UI — שלב ג: שליפה לפי value ו-index

בשלב ג ה-DBMS שלכם צריך לאפשר **שליפת רשומות לפי שדות שבתוך ה-value**, ולעשות זאת מהר בעזרת **index**.
לשלב הזה יש JAR חדש: **skidb-ui-3.0.jar**.

ההתקנה זהה לשלבים הקודמים. אם עוד לא קראתם, התחילו מ[מדריך החניך](STUDENT_GUIDE.md) ומ[מדריך שלב ב](STUDENT_GUIDE_STAGE_B.md).

---

## 1. מעבר מ-2.0 ל-3.0

1. הורידו את הקבצים החדשים:
   - https://github.com/Gar1b1/skid/raw/main/dist/skidb-ui-3.0.jar
   - https://github.com/Gar1b1/skid/raw/main/dist/skidb-ui-3.0-sources.jar (רשות)
2. בתיקייה `lib/` החליפו את `skidb-ui-2.0.jar` ב-`skidb-ui-3.0.jar`, ומחקו את הישן.
3. עדכנו את הספרייה בסביבת הפיתוח. ב-Maven שנו את הגרסה ל-`3.0`.

**שימו לב:** בממשק `SkiDBAdapter` נוספו 4 מתודות חדשות, ולכן ה-Adapter שלכם **לא יתקמפל** עד שתוסיפו אותן. זה בכוונה: הקומפיילר מראה לכם בדיוק מה חסר.

---

## 2. המתודות החדשות

הוסיפו את המתודות האלה ל-Adapter שלכם. אפשר להתחיל עם `TODO` ולממש אחת בכל פעם:

```java
import java.util.Collection;

    @Override
    public void createIndex(String collection, String field) throws Exception {
        // create an index on field, built from the records already in the collection
        throw new UnsupportedOperationException("TODO createIndex");
    }

    @Override
    public Collection<String> getIndexedFields(String collection) throws Exception {
        // e.g. ["firstName", "age"]
        throw new UnsupportedOperationException("TODO getIndexedFields");
    }

    @Override
    public Map<?, ? extends Collection<String>> getIndex(String collection, String field) throws Exception {
        // value -> keys, e.g. {"Israel" -> {"1", "4", "8"}, "Ploni" -> {"2"}}
        throw new UnsupportedOperationException("TODO getIndex");
    }

    @Override
    public Map<String, Map<String, Object>> query(String collection, Map<String, Object> conditions) throws Exception {
        // conditions e.g. {"firstName" -> "Israel"}: return every record whose firstName is "Israel"
        throw new UnsupportedOperationException("TODO query");
    }
```

| מתודה | צריכה לעשות | צריכה לזרוק חריגה כאשר |
|---|---|---|
| `createIndex` | ליצור index על שדה באוסף קיים, כולל הרשומות שכבר נמצאות בו | האוסף לא קיים, השדה לא בסכמה, או שכבר יש עליו index |
| `getIndexedFields` | להחזיר את שמות השדות שיש עליהם index | האוסף לא קיים |
| `getIndex` | להחזיר את תוכן ה-index: ערך → ה-keys של הרשומות שמכילות אותו | האוסף לא קיים, או שאין index על השדה |
| `query` | להחזיר את כל הרשומות (key → value) שכל השדות שלהן שווים **לכל** התנאים | האוסף לא קיים, או ששדה בתנאי לא נמצא בסכמה |

**גמישות בטיפוסים:** אפשר להחזיר ב-`getIndex` כל סוג של `Map` ו-`Collection`, למשל `HashMap<Object, HashSet<String>>` או `Map<String, List<String>>`. ה-UI משווה ערכים לפי הטקסט שלהם, כך ש-index שבו המפתח הוא `21` וגם index שבו המפתח הוא `"21"` נחשבים תקינים.

---

## 3. דברים שחייבים לזכור

- **לא מספיק ליצור index, צריך לתחזק אותו.** כל `addRecord`, `updateRecord` ו-`deleteRecord` חייבים לעדכן את כל ה-indexes של האוסף. חשבו מה קורה לרשומה שהשדה המאונדקס שלה השתנה.
- **אפשר להגדיר index בשתי דרכים:** בזמן יצירת האוסף, או מאוחר יותר על אוסף קיים. ב-UI, כשמסמנים **Index** בזמן יצירת אוסף, ה-UI קורא ל-`createCollection` ומיד אחריו ל-`createIndex` לכל שדה שסומן.
- **אפשר כמה indexes על אותו אוסף, ועל כל סוג שדה** (`String`, `Integer`, `Boolean`).
- **זמני ריצה:** שליפה על שדה מאונדקס צריכה לגשת ישר לרשומות המתאימות דרך ה-index (`O(1)` לחיפוש הערך), ולא לעבור על כל האוסף.
- **טיפוסים:** התנאי `{"age" -> 21}` (Integer) לא שווה לרשומה שבה `age` הוא `"21"` (String).

---

## 4. מה חדש ב-UI

במרכז החלון יש עכשיו שלוש לשוניות:

| לשונית | מה יש בה |
|---|---|
| **Records** | טבלת הרשומות, כמו קודם. בשורת הסכמה כתוב **(indexed)** ליד כל שדה שיש עליו index. **Generate data…** מוסיף הרבה רשומות אקראיות, כדי למדוד מהירות שליפה. |
| **Query** | בונים תנאים (`field = value`, וכולם חייבים להתקיים) ולוחצים **Run query**. מתחת מוצגים התוצאות, זמן הריצה, אילו שדות מאונדקסים, ו**בדיקה** מול סריקה מלאה של האוסף. |
| **Indexes** | יצירת index על שדה קיים, ותוכן כל index: ערך → keys. ה-UI משווה כל index למה שהוא **צריך** להכיל לפי הרשומות. |

**איך ה-UI בודק אתכם:**
- אחרי כל פעולה (add / update / delete / createIndex), ה-UI קורא ל-`getIndex` ומשווה לרשומות. index שלא מעודכן מסומן **באדום**, ליד הלשונית Indexes מופיע **⚠**, וההבדלים נכתבים ללוג.
- כל תוצאת query מושווית לסריקה מלאה. אם חסרות רשומות או שיש רשומות מיותרות, תופיע הודעה באדום, למשל `unexpected keys [8]`.

---

## 5. בדקו את העבודה שלכם

- [ ] יצירת אוסף עם `firstName: String` (מסומן **Index**), `age: Integer`, `isCommander: Boolean` → בשורת הסכמה כתוב `(indexed)` ליד firstName
- [ ] הוספת 3 רשומות עם `firstName = Israel` ורשומה אחת עם `Ploni` → בלשונית Indexes: `Israel → {..3 keys..}`, `Ploni → {..}`, ✔ in sync
- [ ] Query: `firstName = Israel` → 3 תוצאות, ✔ Same records as a full scan
- [ ] Query עם שני תנאים: `firstName = Israel` וגם `isCommander = true` → רק הרשומות שמתאימות לשניהם
- [ ] Query: `firstName = Nobody` → 0 תוצאות, בלי שגיאה
- [ ] Update לרשומה, שינוי `firstName` מ-`Israel` ל-`Dana` → ה-index עדיין ✔ in sync, ו-Query על `Israel` כבר לא מחזיר אותה
- [ ] Delete לרשומה → ה-index עדיין ✔ in sync
- [ ] יצירת index על `age` (Integer) ועל `isCommander` (Boolean) באוסף שכבר יש בו רשומות → שניהם ✔ in sync
- [ ] יצירת index על שדה שכבר מאונדקס → **שגיאה**
- [ ] Query על `age = 21` כשהטיפוס הוא `String` (כלומר `"21"`) → 0 תוצאות, או שגיאה; לא הרשומות עם `21`
- [ ] **Generate data…** עם 100,000 רשומות, ואז Query על שדה מאונדקס מול שדה לא מאונדקס → השליפה המאונדקסת מהירה יותר

---

## 6. פתרון בעיות

| בעיה | פתרון |
|---|---|
| ה-Adapter לא מתקמפל: `MyAdapter is not abstract and does not override ... createIndex` | הוסיפו את 4 המתודות מסעיף 2. |
| ⚠ ליד Indexes, ובלוג: `Index on "firstName" is out of sync — "Israel" has [1, 4, 8], expected [1, 4]` | ה-index לא עודכן. כאן key 8 נמחק או שונה, ולא הוסר מה-index. בדקו את `deleteRecord` ואת `updateRecord`. |
| `unexpected keys` / `missing keys` ב-Query | ה-query מחזיר רשומות לא נכונות. בדרך כלל זה index לא מעודכן, או בדיקה חסרה של שאר התנאים. |
| השליפה המאונדקסת לא מהירה יותר | כנראה שה-query עובר על כל הרשומות גם כשיש index. השתמשו ב-index כדי לקבל ישר את ה-keys המתאימים. |
| הכותרת בחלון היא `Stage B` ואין לשוניות | אתם עדיין מריצים JAR ישן. ודאו שרק `skidb-ui-3.0.jar` נמצא בספריות של הפרויקט. |
