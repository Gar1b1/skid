# SkiDB UI

ממשק גרפי (Swing) לתרגיל **SkiDB** (שלבים א–ג). אתם בונים את ה-DBMS, וה-UI הזה מאפשר ליצור אוספים, להוסיף / לקרוא / לעדכן / למחוק רשומות ולראות את כל מסד הנתונים בזמן העבודה.

ה-UI לא נוגע במימוש הפנימי שלכם. הוא מדבר עם ה-DBMS רק דרך ממשק (interface) אחד, `SkiDBAdapter`, כך שהתכנון (Design) נשאר שלכם.

> **חניכים: התחילו מ[מדריך החניך](docs/STUDENT_GUIDE.md).** יש בו הוראות התקנה שלב אחר שלב ל-IntelliJ, VS Code, Maven, Gradle ושורת הפקודה, תבנית Adapter, רשימת בדיקה ופתרון בעיות.
>
> **שלב ב (Integer ו-Boolean):** ראו את [מדריך שלב ב](docs/STUDENT_GUIDE_STAGE_B.md) והשתמשו ב-[skidb-ui-2.0.jar](dist/skidb-ui-2.0.jar).
>
> **שלב ג (שליפה לפי value ו-index):** ראו את [מדריך שלב ג](docs/STUDENT_GUIDE_STAGE_C.md) והשתמשו ב-[skidb-ui-3.0.jar](dist/skidb-ui-3.0.jar).

| שלב | JAR | מה נתמך |
|---|---|---|
| א | [skidb-ui-1.0.jar](dist/skidb-ui-1.0.jar) | `String` |
| ב | [skidb-ui-2.0.jar](dist/skidb-ui-2.0.jar) | `String`, `Integer`, `Boolean` |
| ג | [skidb-ui-3.0.jar](dist/skidb-ui-3.0.jar) | `String`, `Integer`, `Boolean` + שליפה לפי value ו-index |

## בקצרה

1. דרישות: **Java 21** ומעלה.
2. הורידו את **[skidb-ui-1.0.jar](dist/skidb-ui-1.0.jar)** מתיקיית [`dist/`](dist/). הוא מכיל רק את ה-UI ואת הממשק `SkiDBAdapter`, בלי קוד פתרון. אפשר להוריד גם את [ה-JAR של קוד המקור](dist/skidb-ui-1.0-sources.jar), שמציג את התיעוד בסביבת הפיתוח.
3. הוסיפו את ה-JAR כספרייה לפרויקט שלכם.
4. כתבו מחלקה שמממשת את `com.matmon.skidb.api.SkiDBAdapter` וקוראת לקוד שלכם.
5. הפעילו מתוך `main`:

```java
import com.matmon.skidb.ui.SkiDBUI;

public class Main {
    public static void main(String[] args) {
        SkiDBUI.launch(new MyAdapter(new MyDatabase()));
    }
}
```

---

## למדריך: הרצת הדמו

בתיקייה `src/test/java/com/matmon/skidb/demo` יש Adapter לדוגמה ו-`DemoLauncher`. הדמו פותח את ה-UI עם מסד נתונים ריק. הקוד הזה נמצא רק ב-test scope ו**לא** נכלל ב-JAR.

```bash
mvn test-compile exec:java -Dexec.mainClass=com.matmon.skidb.demo.DemoLauncher -Dexec.classpathScope=test
```

או: פתחו את הפרויקט ב-IntelliJ והריצו את `DemoLauncher.main`.

לבנייה מחדש של ה-JAR אחרי שינוי בקוד: `mvn package`, ואז העתיקו את `target/skidb-ui-1.0.jar` ואת `target/skidb-ui-1.0-sources.jar` אל `dist/`.
