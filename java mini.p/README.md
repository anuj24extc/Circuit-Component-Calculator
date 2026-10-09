# LABSTOCK — Electronics Lab Component Borrowing & Inventory System

A Java Swing desktop application for a college electronics lab. It keeps the inventory, lends equipment and components to students, issues consumables (resistors, LEDs, wires) that are used up, takes items back, records every transaction and points out items that are running low. It won't lend to a student with an overdue item. Data is saved safely in plain text files.

Standard Java + Swing only. There are no external libraries and no database, and it runs on **Java 8 or newer**.

![Dashboard](docs/screenshots/01_dashboard.png)

## Run it

**Quickest:** double-click `LabStock.jar`, or run:

```
java -jar LabStock.jar
```

**From source (Windows):** double-click `run.bat`, which compiles into `out/` and starts the app.

**From source (any OS):**

```
javac -encoding UTF-8 -d out -sourcepath src src/labstock/Main.java
java -cp out labstock.Main
```

**IntelliJ IDEA:** File → Open → select this folder. If it asks for a JDK, choose any installed JDK (8+). Open `src/labstock/Main.java`, then click the green ▶ next to `main`.

**Eclipse:** File → Import → General → *Existing Projects into Workspace* → select this folder. Right-click `Main.java` → Run As → Java Application.

**VS Code:** install the *Extension Pack for Java*, open this folder, open `Main.java` and click **Run** above `main`.

On the first launch a `data/` folder is created next to where the app is started. It is filled with 68 sample items and 12 sample transactions. To see the overdue rule, type roll number **EX-2131** on the Borrow page. To start again with fresh sample data, close the app and delete the `data/` folder.

**Tests:** double-click `run-tests.bat`. It runs 55 automatic checks on a temporary copy of the data. In an IDE, run `test/labstock/LabStockTest.java` like any `main`.

## Project structure

```
LabStock/
├── src/labstock/
│   ├── Main.java                    entry point
│   ├── model/                       the data
│   │   ├── Item.java                abstract: id, name, category, quantities, min stock
│   │   ├── Equipment.java           Item subclass: 7-day loan, max 2 per borrow
│   │   ├── ElectronicComponent.java Item subclass: 14-day loan, no per-borrow limit
│   │   ├── Consumable.java          Item subclass: issued and used up, never returned
│   │   ├── Student.java             name + roll number
│   │   ├── BorrowTransaction.java   one loan or issue (has a Student)
│   │   ├── TransactionStatus.java   enum BORROWED / OVERDUE / RETURNED / ISSUED
│   │   └── Activity.java            one line of the activity log
│   ├── service/                     the rules and the files
│   │   ├── InventoryManager.java    borrow/issue, return, add/edit/delete, queries
│   │   ├── FileManager.java         reads/writes data/*.txt (safe save via .tmp)
│   │   ├── InputValidator.java      empty boxes, whole numbers
│   │   ├── DateUtil.java            DD/MM/YYYY parsing and formatting
│   │   └── LabException.java        custom checked exception
│   └── ui/                          the Swing screens
│       ├── MainFrame.java           window: sidebar, top bar, pages, status bar
│       ├── Sidebar.java
│       ├── BasePage.java            abstract base of the 5 pages
│       ├── DashboardPanel.java
│       ├── InventoryPanel.java
│       ├── ItemDialog.java          add / edit item form
│       ├── BorrowPanel.java
│       ├── ItemPicker.java          search box with a list of matching items
│       ├── ReturnPanel.java
│       ├── HistoryPanel.java
│       ├── MessageDialog.java       error / success / confirm dialogs
│       ├── Theme.java               colours, fonts, small helpers
│       └── components/              custom-painted Swing widgets
│           FlatButton, FlatTextField, FlatComboBox, SegmentedControl,
│           RoundedPanel, StyledTable, ThinScrollBarUI
├── test/labstock/LabStockTest.java  55 automatic checks (plain Java, no JUnit)
├── data/                            created on first run (inventory, transactions, activity)
├── docs/
│   ├── PROJECT_DOCUMENTATION.md     explanations and viva Q&A
│   └── screenshots/
├── LabStock.jar                     ready-to-run build
├── run.bat / run.sh / run-tests.bat
└── .idea/ .project .classpath .vscode/   IDE project files
```

See **[docs/PROJECT_DOCUMENTATION.md](docs/PROJECT_DOCUMENTATION.md)** for the full documentation and viva preparation.
