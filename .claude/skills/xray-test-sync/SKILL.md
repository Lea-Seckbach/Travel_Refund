---
name: xray-test-sync
description: Syncs the JUnit tests to Jira Xray through an Xray MCP server - creates/updates one Xray Test per test method (title, preconditions, steps, expected results taken from the code), puts each test class in its own Test Repository folder, writes the Jira key and link into the code, and after a run sets each test's status to Run (passed) or Failed. Use when asked to "sync tests to Jira/Xray", "write the tests into Jira", or "update Jira with the test results". מסנכרן את הבדיקות ל-Jira Xray ומעדכן סטטוס אחרי הרצה.
---

# סנכרון בדיקות ל-Jira Xray

הסקיל כותב כל מקרה בדיקה מהקוד ל-Jira Xray, מוסיף לקוד את מזהה ה-Jira והקישור,
ואחרי הרצה מעדכן את הסטטוס של כל בדיקה לפי התוצאה.

כל ההגדרות (כתובת Jira, מפתח פרויקט, שמות סטטוסים, שפה) נמצאות בקובץ
`xray-config.json` שליד הקובץ הזה. קרא אותו לפני כל פעולה.

## כללים

1. **רק דרך MCP של Xray.** כל פעולה מול Jira/Xray מתבצעת בכלים של שרת MCP של
   Xray. לפני שמתחילים, חפש את הכלים (ToolSearch עם "xray", ואם אין - "jira").
   אם אין שרת MCP של Xray זמין - עצור, הסבר למשתמש שצריך להתקין ולהגדיר אותו,
   ואל תנסה לעקוף דרך curl או REST ישיר.
2. **כל מתודת בדיקה = Test נפרד ב-Jira.** (issue type `Test`, מסוג
   `testType` מההגדרות).
3. **כל מחלקת בדיקות = תיקייה ב-Test Repository.** שם התיקייה = שם המחלקה,
   מתחת ל-`repositoryRootFolder`. אם התיקייה לא קיימת - צור אותה.
4. **התוכן נלקח רק ממה שהקוד באמת מריץ.** אל תמציא שלבים, נתונים או תוצאות
   שלא קיימים בקוד.
5. **לא יוצרים כפילויות.** מתודה שכבר יש מעליה מזהה Jira - מעדכנים את ה-Test
   הקיים, לא יוצרים חדש.
6. **לפני יצירה או עדכון ב-Jira - מציגים תצוגה מקדימה ומחכים לאישור.** פעולות
   ב-Jira גלויות לכל הצוות.
7. כתוב את תוכן ה-Jira בשפה שמוגדרת ב-`language` (`he` = עברית). שמות מחלקות,
   מתודות, שדות JSON וערכים מהקוד נשארים באנגלית.

## פעולה 1 - סנכרון הבדיקות ל-Xray ("sync")

### שלב 1: איסוף מקרי הבדיקה מהקוד
עבור כל מחלקה ב-`testSourceDir` שיש בה מתודות `@Test`:

| שדה ב-Jira | מאיפה לוקחים אותו |
|------------|-------------------|
| **Title (Summary)** | `@DisplayName` של המתודה. אם אין - שם המתודה בפירוק למילים. מוסיפים קידומת של `@DisplayName` של המחלקה, בקיצור. |
| **Pre-conditions** | מה שחייב להתקיים לפני שהבדיקה רצה: ה-`@BeforeAll`/`@BeforeEach` (למשל בחירת מערכת אמיתית/סימולטור, הגנה על נתוני הבדיקה), נתוני הבדיקה מ-`TestConfig` (עובד, חודש, ימים, מרחק, אחוז משרה), ו**בדיקות קודמות לפי `@Order`** שהבדיקה תלויה בהן (למשל "שלב 3 - החישוב הורץ בהצלחה" כשהמתודה קוראת ל-`requireCalculated()`). |
| **Test Steps** | כל קריאה למערכת (`api.xxx(...)`) או חישוב בתוך המתודה = שלב. מתארים מה עושים ועם אילו נתונים אמיתיים (מזהה עובד, חודש). |
| **Expected Result** | לכל שלב - מה ה-`assert` שאחריו בודק, כולל הערכים הצפויים בפועל. ערכים מחושבים (למשל הסכום הצפוי מ-`ReferenceCalculator`) - לחשב לפי `TestConfig` ולכתוב גם את הנוסחה וגם את המספר (למשל "22.60 × 100% × 15 = 339.00 ₪"). |

בנוסף לכל Test: `labels` מההגדרות, ובתיאור (Description) - שם המחלקה והמתודה
המלאים, כדי שאפשר יהיה למצוא את הקוד.

Pre-conditions: אם תנאי מקדים משותף לכמה בדיקות (למשל נתוני העובד), צור אותו
פעם אחת כ-issue מסוג `Pre-Condition` וקשר אותו לכל הבדיקות שצריכות אותו, במקום
לשכפל. אם ה-MCP לא תומך ב-Pre-Condition, כתוב את התנאים בשדה ה-Pre-conditions
או בראש ה-Description.

### שלב 2: תצוגה מקדימה ואישור
הצג למשתמש טבלה:

| מחלקה (תיקייה) | מתודה | פעולה (חדש / עדכון KEY) | כותרת | מס' שלבים |

ומתחתיה את התוכן המלא של בדיקה אחת לדוגמה. שאל אם לבצע. אל תמשיך בלי אישור.

### שלב 3: יצירה/עדכון ב-Xray
לכל מחלקה:
1. ודא שהתיקייה `repositoryRootFolder/<ClassName>` קיימת ב-Test Repository
   (צור אם לא).
2. לכל מתודה - צור Test חדש או עדכן את הקיים: כותרת, Pre-conditions, שלבים
   (Action / Data / Expected Result), labels.
3. שייך את ה-Test לתיקייה של המחלקה.

### שלב 4: כתיבת הקישור לקוד
מעל כל מתודת `@Test` שנוצר לה Test חדש, הוסף:

```java
// Xray: QA-123 - https://YOUR-COMPANY.atlassian.net/browse/QA-123
@Tag("QA-123")
@Test
```

- השורה עם `// Xray:` היא מקור האמת למיפוי מתודה ← Jira. כך הסקיל מזהה בפעם
  הבאה שה-Test כבר קיים (כלל 5).
- `@Tag` מאפשר להריץ בדיקה לפי מזהה: `mvn test -Dgroups=QA-123`.
  ודא שיש `import org.junit.jupiter.api.Tag;`.
- אם כבר יש שורת `// Xray:` - אל תשנה אותה.

### שלב 5: סיכום
דווח: כמה Tests נוצרו, כמה עודכנו, כמה תיקיות נוצרו, ורשימת המזהים עם קישורים.

## פעולה 2 - עדכון סטטוס אחרי הרצה ("report")

### שלב 1: קריאת התוצאות
1. אם המשתמש ביקש להריץ - הרץ `mvn test` (אם Maven לא מותקן, אמור זאת ועצור).
2. קרא את קובצי ה-XML ב-`surefireReportsDir` (`TEST-*.xml`). לכל `<testcase>`:
   - יש `<failure>` או `<error>` ← **נכשל**
   - יש `<skipped>` ← **דולג** (לא משנים סטטוס, רק מדווחים)
   - אחרת ← **עבר**
3. מצא את מזהה ה-Jira של כל testcase לפי שורת `// Xray:` שמעל המתודה בקוד
   (`classname` + `name` ב-XML ← המחלקה והמתודה). בדיקה בלי מזהה - דווח עליה
   כ"לא מסונכרנת" והצע להריץ קודם את פעולה 1.

### שלב 2: עדכון ב-Xray
1. אם `createTestExecution` הוא `true` - צור Test Execution אחד להרצה הזו
   (כותרת לפי `testExecutionSummary`) והוסף אליו את כל הבדיקות שרצו.
   קבע לכל Test Run את התוצאה: `xrayRunStatusOnPass` / `xrayRunStatusOnFail`.
   בבדיקה שנכשלה - הוסף לתגובה (comment) של ה-Test Run את הודעת הכשל מה-XML.
2. העבר כל Test לסטטוס workflow ב-Jira:
   - עבר ← `statusOnPass` (ברירת מחדל: **Run**)
   - נכשל ← `statusOnFail` (ברירת מחדל: **Failed**) - גם אם הוא היה קודם
     ב-Run.
   - אם המעבר לא אפשרי (הסטטוס לא קיים ב-workflow או אין מעבר ישיר) - אל
     תנחש סטטוס אחר; דווח למשתמש איזה Test ומה המעברים הזמינים.

### שלב 3: סיכום
טבלה: מזהה | בדיקה | תוצאה | סטטוס חדש ב-Jira | קישור. ובסוף - קישור
ל-Test Execution, ומספר עברו / נכשלו / דולגו.

## לפני השימוש הראשון
- ב-`xray-config.json`: עדכן `jiraBaseUrl`, `projectKey`, ושמות הסטטוסים לפי
  ה-workflow האמיתי של ה-Test ב-Jira.
- ודא ששרת ה-MCP של Xray מוגדר ב-Claude Code ומחובר עם הרשאות ליצירת Tests,
  תיקיות, Test Executions ומעברי סטטוס.
