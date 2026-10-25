# SkiDB UI — שלב ו: שליפה לפי partitions (חלוקה לפי זמן)

בשלב ו אוסף יכול להיות **מחולק ל-partitions לפי זמן**: כל הרשומות שהזמן שלהן נופל באותה **שעה** נשמרות באותו partition.
שליפה עם תנאי על הזמן צריכה לקרוא **רק את ה-partitions שיכולים להכיל תוצאות**, ולדלג על כל השאר (partition pruning).

לשלב הזה יש JAR חדש: **skidb-ui-6.0.jar**.

ההתקנה זהה לשלבים הקודמים. אם עוד לא קראתם, התחילו מ[מדריך החניך](STUDENT_GUIDE.md) ומ[מדריך שלב ה](STUDENT_GUIDE_STAGE_E.md).

---

## 1. מעבר מ-5.0 ל-6.0

1. הורידו את הקבצים החדשים:
   - https://github.com/Gar1b1/skid/raw/main/dist/skidb-ui-6.0.jar
   - https://github.com/Gar1b1/skid/raw/main/dist/skidb-ui-6.0-sources.jar (רשות)
2. בתיקייה `lib/` החליפו את `skidb-ui-5.0.jar` ב-`skidb-ui-6.0.jar`, ומחקו את הישן.
3. עדכנו את הספרייה בסביבת הפיתוח. ב-Maven שנו את הגרסה ל-`6.0`.

**שימו לב:** המתודות של שלב ה לא השתנו, אבל נוספו **4 מתודות חדשות** (סעיף 3). ה-Adapter שלכם לא יתקמפל עד שתוסיפו אותן.

---

## 2. טיפוס חדש: `DateTime`

| טיפוס בסכמה | המחלקה ב-Java | דוגמה ב-UI |
|---|---|---|
| `"DateTime"` | `java.time.LocalDateTime` | `2026-10-25 14:30` |

- ב-`addRecord` וב-`updateRecord` ערך של שדה DateTime מגיע כ-`LocalDateTime`.
- אפשר לשלוף עליו עם כל האופרטורים של שלב ה: `=`, `>`, `>=`, `<`, `<=`, `between`. `LocalDateTime` מושווה לפי זמן.
- אפשר לבנות עליו index מסוג HASH או TREE, כמו על כל שדה.

---

## 3. partitions לפי שעה

### מזהה (id) של partition

כל partition מייצג **שעה אחת**. המזהה שלו הוא השעה בפורמט `yyyy-MM-dd'T'HH`:

| זמן הרשומה | ה-partition שלה |
|---|---|
| `2026-10-25 14:00` | `"2026-10-25T14"` |
| `2026-10-25 14:37` | `"2026-10-25T14"` |
| `2026-10-25 14:59:59` | `"2026-10-25T14"` |
| `2026-10-25 15:00` | `"2026-10-25T15"` |

ה-UI משווה את ה-partitions שלכם למזהים האלה, ולכן **השתמשו בדיוק בפורמט הזה**. כדאי לבדוק מה עושות `truncatedTo` ו-`DateTimeFormatter` של `java.time`.

### המתודות החדשות

```java
    @Override
    public void createPartitionedCollection(String name, Map<String, String> schema, String partitionField) throws Exception {
        // like createCollection, but the records are split by the hour of partitionField (a DateTime field)
        throw new UnsupportedOperationException("TODO createPartitionedCollection");
    }

    @Override
    public String getPartitionField(String collection) throws Exception {
        // the DateTime field the collection is partitioned by, or null if it is not partitioned
        throw new UnsupportedOperationException("TODO getPartitionField");
    }

    @Override
    public Map<String, ? extends Collection<String>> getPartitions(String collection) throws Exception {
        // partition id -> keys, e.g. {"2026-10-25T14" -> {"1", "7"}, "2026-10-25T15" -> {"3"}}
        throw new UnsupportedOperationException("TODO getPartitions");
    }

    @Override
    public Collection<String> getScannedPartitions(String collection) throws Exception {
        // the ids of the partitions the LAST query on this collection read, e.g. ["2026-10-25T14"]
        throw new UnsupportedOperationException("TODO getScannedPartitions");
    }
```

| מתודה | צריכה לעשות | צריכה לזרוק חריגה כאשר |
|---|---|---|
| `createPartitionedCollection` | ליצור אוסף ריק שמחולק לפי השעה של `partitionField` | השם כבר קיים, הסכמה לא תקינה, או ש-`partitionField` אינו שדה DateTime בסכמה |
| `getPartitionField` | להחזיר את שם השדה שלפיו האוסף מחולק, או **`null`** אם הוא לא מחולק | האוסף לא קיים |
| `getPartitions` | להחזיר את ה-partitions: id → keys של הרשומות שבו. ריק אם האוסף לא מחולק | האוסף לא קיים |
| `getScannedPartitions` | להחזיר את ה-ids של ה-partitions ש**השליפה האחרונה** על האוסף קראה. ריק אם האוסף לא מחולק | האוסף לא קיים |

`createCollection` הרגיל ממשיך ליצור אוסף **בלי** partitions.

---

## 4. דברים שחייבים לזכור

- **תחזוקה:** `addRecord` מכניס את הרשומה ל-partition של השעה שלה. `deleteRecord` מוציא אותה. `updateRecord` ששינה את הזמן צריך **להעביר** את הרשומה ל-partition החדש.
- **Pruning:** כשיש בשליפה תנאי על שדה ה-partition, קראו **רק** את ה-partitions שהשעה שלהם יכולה לקיים את התנאי:
  - `at between 14:20 and 16:10` → רק `T14`, `T15`, `T16`
  - `at > 14:20` → מ-`T14` והלאה (גם ב-`T14` יש זמנים אחרי 14:20)
  - `at < 15:00` → עד `T14` (ב-`T15` אין אף זמן לפני 15:00)
- **בלי תנאי על הזמן** אי אפשר לדלג על partitions, וצריך לקרוא את כולם.
- **כמה תנאים על הזמן**, למשל `at >= ...` וגם `at < ...`: קראו רק את ה-partitions שמתאימים **לכולם**.
- **שילוב עם indexes:** בתוך ה-partitions שנבחרו אפשר עדיין להשתמש ב-index כדי לסנן מהר יותר.
- **רמז:** אם המזהים שמורים במבנה ממוין, אפשר לקבל את כל ה-partitions שבטווח בפעולה אחת, בלי לעבור על כולם.
- `getScannedPartitions` צריך לדווח **בדיוק** מה השליפה האחרונה קראה, כדי שה-UI יוכל לבדוק את ה-pruning שלכם.

---

## 5. מה חדש ב-UI

- **יצירת אוסף:** טיפוס חדש בסכמה, `DateTime`, ובתחתית החלון **Partition by hour of**: בחרו שדה DateTime, או `None` לאוסף בלי partitions.
- **שורת הסכמה** (Records): ליד שדה ה-partition כתוב `(partitions by hour)`.
- **לשונית Partitions (חדשה):** כל partition וה-keys שבו, מול מה שהוא **צריך** להכיל לפי הזמנים של הרשומות. רשומה ב-partition הלא נכון מסומנת באדום, וליד הלשונית מופיע ⚠.
- **בדיקת pruning:** אחרי כל שליפה על אוסף מחולק, ה-UI קורא ל-`getScannedPartitions` ומציג למשל:
  - `✔ Partitions: read 3 of 72, needed 3` → מצוין
  - `✖ Partitions: read 72 of 72, needed 3 · could have skipped [...]` → השליפה קראה partitions מיותרים
  - `✖ ... did not read needed [...]` → השליפה דילגה על partition שהיה צריך לקרוא
- **Generate data…** ממלא שדות DateTime בזמנים אקראיים מ-3 הימים האחרונים, כלומר בערך 72 partitions.

---

## 6. בדקו את העבודה שלכם

- [ ] יצירת אוסף `events` עם `name: String`, `at: DateTime`, ו-**Partition by hour of** = `at` → בשורת הסכמה `(partitions by hour)`
- [ ] הוספת רשומות עם `at` = `2026-10-25 14:05`, `2026-10-25 14:50`, `2026-10-25 15:10` → בלשונית Partitions: `2026-10-25T14` עם 2 keys, `2026-10-25T15` עם key אחד, ✔ in sync
- [ ] Update לרשומה, שינוי `at` ל-`2026-10-25 18:00` → היא עוברת ל-`2026-10-25T18`, ✔ in sync
- [ ] Delete לרשומה → היא נעלמת מה-partition שלה, ✔ in sync
- [ ] **Generate data…** עם 10,000 רשומות
- [ ] Query: `at between <שעה מסוימת>:00 and <אותה שעה>:59` → ✔ Same records as a full scan, ו-`✔ Partitions: read 1 of …, needed 1`
- [ ] Query: `at > <זמן כלשהו>` → ✔ ב-pruning (נקראים רק partitions מאותה שעה והלאה)
- [ ] Query: `at < <זמן עגול, למשל 15:00>` → ✔ ב-pruning (ה-partition של 15:00 **לא** נקרא)
- [ ] Query עם שני תנאים: `at >= ...` וגם `at < ...` → ✔ ב-pruning
- [ ] Query על `name = Israel` בלי תנאי על הזמן → `read N of N, needed N` (אין על מה לדלג)
- [ ] Query: `at > "2026"` כשהטיפוס הוא `String` → 0 תוצאות, ו-0 partitions נקראו
- [ ] אוסף **בלי** partitions (Partition by hour of = None) → הלשונית Partitions מציינת שהאוסף לא מחולק, והכול עובד כמו בשלב ה

---

## 7. פתרון בעיות

| בעיה | פתרון |
|---|---|
| ה-Adapter לא מתקמפל: `does not override ... createPartitionedCollection` | הוסיפו את 4 המתודות מסעיף 3. |
| ⚠ ליד Partitions: `"2026-10-25T14" has [1, 3], expected [1]` | רשומה 3 נמצאת ב-partition הלא נכון, כנראה אחרי Update שלא העביר אותה. |
| כל ה-partitions מסומנים אדום, ו-ids כמו `2026-10-25T14:00` | פורמט ה-id לא נכון. צריך `yyyy-MM-dd'T'HH`, למשל `2026-10-25T14`. |
| `could have skipped [...]` | השליפה קראה partitions שהשעה שלהם לא יכולה לקיים את התנאי. בדקו את חישוב הטווח. |
| `did not read needed [...]` | השליפה דילגה על partition נחוץ. בדרך כלל זו טעות בגבול: `>` לעומת `>=`, או שעה שבה התנאי מתחיל באמצע. |
| `getScannedPartitions` מחזיר partitions של שליפה קודמת | עדכנו את רשימת ה-partitions שנקראו **בכל** קריאה ל-`query`, ונקו אותה בתחילת כל שליפה. |
| `ClassCastException: String cannot be cast to LocalDateTime` | השוויתם ערך מטיפוס אחר לשדה DateTime. בדקו את הטיפוס של `value()` לפני ההשוואה. |
| הכותרת בחלון היא `Stage E` ואין לשונית Partitions | אתם עדיין מריצים JAR ישן. ודאו שרק `skidb-ui-6.0.jar` נמצא בספריות של הפרויקט. |
