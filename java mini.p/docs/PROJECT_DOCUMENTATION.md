# LABSTOCK: Project Documentation

## 1. Project Title

**LABSTOCK — Electronics Lab Component Borrowing & Inventory System**

A Java Swing mini project, version 1.1.

---

## 2. Abstract

Electronics laboratories give out components and instruments to students every day: resistors, ICs, sensors, Arduino boards, multimeters, function generators. This is usually written in a register, which makes it hard to know what is in stock, who has an item, and what is overdue. LABSTOCK is a standalone Java desktop application that replaces the register.

The lab assistant can:

- maintain the inventory;
- lend equipment and reusable components;
- issue consumable parts that are used up;
- take items back;
- see the complete transaction history.

Available quantities update automatically on every borrow, issue and return. The application refuses invalid or impossible requests, such as borrowing more than is in stock, returning the same loan twice, or lending to a student who still has an overdue item. It flags items that fall below their minimum stock level and loans that are past their return date.

All data is stored in plain text files that are saved safely after every change, so nothing is lost when the program closes. The project uses object-oriented programming (including three item classes with different behaviour), collections, exception handling, file handling, a Swing GUI and automated testing.

---

## 3. Problem Statement

In most college labs, borrowing is recorded by hand. This causes several problems:

- Nobody knows the exact available quantity without counting the shelf.
- Items are issued even when stock is too low for the next practical.
- It is hard to find who borrowed an item, or whether it is overdue.
- Students who haven't returned items keep borrowing more.
- Cheap parts that are used up (resistors, LEDs, wires) are mixed up with items that must come back.
- Entries are illegible, incomplete or lost.

A simple computer system is needed that keeps an accurate count, checks every request against the stock and the lab's rules, and keeps a permanent record. It must not need a database or a network.

---

## 4. Objectives

1. Maintain an inventory of lab equipment, components and consumables with total and available quantities.
2. Lend items only when enough stock is available, and never to a student with an overdue loan.
3. Issue consumables as used up, so they reduce the stock permanently.
4. Accept returns and restore the stock, never processing the same return twice.
5. Keep a searchable history of all transactions: borrowed, overdue, returned and issued.
6. Warn when items fall below a configurable minimum stock level.
7. Save all data safely in files so it is available every time the program starts.
8. Demonstrate core Java concepts in a clean, working and tested desktop application.

---

## 5. Scope

**Included:** a single lab, used by the lab assistant on one computer. It manages items (add, edit, delete, search, filter), borrowing, consumable issues, returning, history, low-stock alerts and overdue detection.

**Not included:** multiple labs, user logins, fines, barcode scanning, partial returns, network/multi-user access and databases. These are listed under Future Scope.

---

## 6. Features

* **Dashboard**:
  * four compact figures: total items, available units, borrowed units, low-stock items;
  * a recent-activity list and a low-stock table;
  * a greeting that depends on the time of day, and a live date/time in the top bar.
* **Inventory**:
  * a table with ID, item, category, type, available, total, minimum stock and status (READY / LOW STOCK / OUT OF STOCK);
  * live search by ID or name, and a category filter;
  * Add, Edit and Delete, with a confirmation before deleting.
* **Three kinds of item**:

  | Kind | Examples | Rules |
  |---|---|---|
  | Equipment | Arduino, multimeter, oscilloscope | 7-day loan, at most 2 per borrow |
  | Component | ICs, sensors, modules | 14-day loan |
  | Consumable | resistors, capacitors, LEDs, wires | issued and used up, never returned |

* **Borrow / Issue**:
  * a **search box** for the component: typing "555", "sensor" or "E00" lists matching items with their stock, picked with the mouse or the arrow keys;
  * typing a **roll number** fills in a known student's name and shows what they already have out. An **overdue loan blocks new borrowing**;
  * a live availability line, the quantity with − / + buttons, and a default return date (none for consumables);
  * a success message and a receipt with the transaction ID.
* **Returns**:
  * search by transaction ID or roll number; open loans are listed by default;
  * a details card and a confirmation before returning;
  * already-returned loans and consumable issues cannot be returned.
* **History**: every transaction (newest first), with text search and a status filter (ALL / BORROWED / OVERDUE / RETURNED / ISSUED).
* **Rules enforced**:
  * no zero, negative or excess quantities, and no more than 2 units of equipment;
  * no past return dates or impossible dates such as 31/02;
  * no duplicate item IDs;
  * no deleting an item while units are on loan;
  * no new loans for a student with an overdue item.
* **Persistence**: three readable text files, saved through a temporary file so a crash can't corrupt them, and reloaded at start-up. Sample data is created on the first run.
* **Sequential transaction IDs**: TXN-1001, TXN-1002, … The numbering continues correctly after a restart.
* **Automated tests**: `run-tests.bat` runs 55 checks covering every rule and the save/reload cycle.

---

## 7. Modules

| Module | What it does | Main classes |
|---|---|---|
| Inventory management | Stores items; add, edit, delete, search, filter | `Item`, `Equipment`, `ElectronicComponent`, `Consumable`, `InventoryManager`, `InventoryPanel`, `ItemDialog` |
| Borrowing & issuing | Validates and records loans and consumable issues, reduces stock | `InventoryManager.borrowItem()`, `BorrowPanel`, `ItemPicker`, `BorrowTransaction`, `Student` |
| Returns | Finds loans, marks them returned, restores stock | `InventoryManager.returnItem()`, `ReturnPanel` |
| History & tracking | Lists all transactions; overdue detection | `HistoryPanel`, `TransactionStatus` |
| Dashboard & alerts | Summary figures, recent activity, low stock | `DashboardPanel`, `Activity` |
| File storage | Saves (safely) and loads the three data files | `FileManager` |
| Validation | Checks every input and rule | `InputValidator`, `DateUtil`, `LabException` |
| Testing | Automatic checks of all rules | `LabStockTest` |
| User interface | Window, navigation, styled widgets | `MainFrame`, `Sidebar`, `BasePage`, `Theme`, `components/*` |

---

## 8. Hardware / Software Requirements

| | Minimum |
|---|---|
| Processor | Any dual-core PC |
| RAM | 2 GB (the app itself uses well under 200 MB) |
| Disk | 5 MB for the program and data |
| Display | 1100 × 700 or larger |
| Operating system | Windows 10/11, Linux or macOS |
| Java | JDK 8 or newer (tested on JDK 24, compiled with `--release 8`) |
| IDE (optional) | IntelliJ IDEA, Eclipse or VS Code with the Java extension |
| Libraries | None. Only the standard Java library (Swing, java.io, java.nio.file, java.time) |

---

## 9. Java Concepts Used

| Concept | Where it is used |
|---|---|
| Classes & objects | Every item, student, transaction and activity is an object; each screen is an object created in `MainFrame`. |
| Constructors | Every model class; `super(...)` in `Equipment`, `ElectronicComponent`, `Consumable` and all pages. |
| Encapsulation | All fields are `private`, with getters. Stock can only change through `issue()` / `receive()`. `getItems()` returns an unmodifiable list. |
| Inheritance | `Item → Equipment / ElectronicComponent / Consumable`; `BasePage → 5 pages`; `ItemPicker → FlatTextField`; UI classes extend Swing classes. |
| Method overriding & polymorphism | `getLoanDays()`, `getMaxPerBorrow()` and `isReturnable()` differ by class. `Consumable` overrides `issue()` to also reduce the total. `BasePage.refresh()` is called the same way for every page. |
| Abstract classes | `Item` and `BasePage` cannot be instantiated. |
| `protected` access | `Item.useUp()` can be called by subclasses (`Consumable`) but not by other classes. |
| Composition | A `BorrowTransaction` *has a* `Student`. |
| Enum | `TransactionStatus { BORROWED, OVERDUE, RETURNED, ISSUED }`. |
| ArrayList / collections | Items, transactions, activities, search matches, and the rows shown in each table. |
| Exception handling | Custom checked `LabException`; `NumberFormatException`, `DateTimeParseException`, `IOException` and `AtomicMoveNotSupportedException` are caught; multi-catch in `FileManager`; `try-finally` in `saveChanges()`. |
| File handling | `BufferedReader` / `BufferedWriter` with try-with-resources, and `Files.move` for safe saving, in `FileManager`. |
| Date & time API | `LocalDate` and `LocalDateTime`; strict parsing of DD/MM/YYYY. |
| Swing GUI | `JFrame`, `JPanel`, `CardLayout`, `GridBagLayout`, `JTable`, `JList`, `JPopupMenu`, `JComboBox`, `JDialog`, custom painting. |
| Event handling | `ActionListener`, `DocumentListener` (live search), `ListSelectionListener`, mouse and key bindings, lambdas. |
| Testing | `LabStockTest`: a plain-Java test program with 55 checks. |

---

## 10. Class Description

### model package: the data

| Class | Description |
|---|---|
| `Item` (abstract) | Any stock item: `id`, `name`, `category`, `totalQuantity`, `availableQuantity`, `minimumStock`. `issue(qty)` removes units and `receive(qty)` puts them back. `isLowStock()` and `getStockStatus()` report its state. It declares the abstract methods `getType()`, `getLoanDays()` and `getMaxPerBorrow()`, plus `isReturnable()` (true by default). |
| `Equipment` | Boards, instruments and tools. A 7-day loan and at most 2 units per borrow. |
| `ElectronicComponent` | Reusable parts: ICs, sensors, modules, breadboards. A 14-day loan, limited only by stock. (It isn't named `Component` because that would clash with `java.awt.Component`.) |
| `Consumable` | Parts that are used up: resistors, capacitors, LEDs, wires, batteries. `isReturnable()` returns false, and `issue()` is overridden so issued units leave the total as well as the shelf. |
| `Student` | Name and roll number of the borrower. |
| `BorrowTransaction` | One transaction: ID, student, item ID and name, quantity, borrow date, expected return date (null for consumables), and return date (null until returned). `getStatus()` works out BORROWED, OVERDUE, RETURNED or ISSUED. |
| `TransactionStatus` | Enum with the four statuses. |
| `Activity` | Time and message for the dashboard's recent-activity list. |

### service package: the rules and storage

| Class | Description |
|---|---|
| `InventoryManager` | The core class. It holds the three `ArrayList`s and every rule: `borrowItem()` (loans and consumable issues), `returnItem()`, `addItem()`, `updateItem()`, `deleteItem()`. It provides the queries for the screens (low stock, open loans, a student's loans, overdue check, search, totals), generates transaction IDs, and creates the sample data. After each change it saves the files and notifies the screens. |
| `FileManager` | The only class that knows the file format. It converts objects to and from text lines, and reads/writes `inventory.txt`, `transactions.txt` and `activity.txt` through a temporary file (safe saving). |
| `InputValidator` | Rejects empty boxes, numbers that aren't whole numbers, and the `\|` character. |
| `DateUtil` | Parses and formats DD/MM/YYYY strictly, and produces "Today 21:42"-style times. |
| `LabException` | Custom checked exception. Its message is shown to the user as-is. |

### ui package: the screens

| Class | Description |
|---|---|
| `MainFrame` | The window. It builds the sidebar, top bar (breadcrumb and clock), the pages inside a `CardLayout`, and the status bar. It refreshes every page when the data changes. |
| `Sidebar` | Navigation, with the blue marker on the selected page and counts (items, open loans). |
| `BasePage` (abstract) | Common padding and heading for all pages. It forces each page to implement `refresh()`. |
| `DashboardPanel` | Statistic boxes, recent activity and the low-stock table. |
| `InventoryPanel` | Item table, search, category filter, Add/Edit/Delete. |
| `ItemDialog` | Add/Edit form with the COMPONENT / CONSUMABLE / EQUIPMENT type. Errors appear in red inside the dialog. |
| `ItemPicker` | Search box with a popup list of matching items (a `JPopupMenu` holding a `JList`), used on the Borrow page. |
| `BorrowPanel` | The borrowing form, the student and availability lines, item details and the receipt. |
| `ReturnPanel` | Search, open-loans table, details card, RETURN button. |
| `HistoryPanel` | Full transaction table with search and status filter. |
| `MessageDialog` | Error, success and confirmation dialogs in the dark style. |
| `Theme` | All colours and fonts, plus helpers (status colours, icons, text-change listener). |
| `components/*` | Small custom-painted widgets: `FlatButton`, `FlatTextField`, `FlatComboBox`, `SegmentedControl`, `RoundedPanel`, `StyledTable` (table with status pills) and `ThinScrollBarUI`. |

### test folder

| Class | Description |
|---|---|
| `LabStockTest` | 55 automatic checks of every rule, run on a temporary copy of the data. It prints ok or FAIL for each check. |

---

## 11. System Workflow

```
            ┌──────────── start ────────────┐
            │ Main → InventoryManager.load() │
            │  data/ exists? ── no ──► create sample data, save files
            │       │ yes                     │
            │       ▼                         │
            │  FileManager reads 3 files      │
            └───────────────┬─────────────────┘
                            ▼
                 MainFrame shows Dashboard
                            │
   ┌───────────────┬────────┴──────┬────────────────┬──────────────┐
   ▼               ▼               ▼                ▼              ▼
 Inventory       Borrow         Returns          History       Dashboard
 add/edit/      search item →  search → pick →  search /       figures,
 delete item    check student  confirm →        filter         activity,
   │            → validate →   receive stock →                 low stock
   │            loan (due date) mark RETURNED
   │            or consumable
   │            issue (used up)
   └───────────────┴───────┬───────┘
                           ▼
          InventoryManager.saveChanges()
          → FileManager writes .tmp files, then swaps them in
          → every screen refresh()es
```

---

## 12. How the project works (viva explanations)

### 12.1 What each class does
See section 10. In one line:

- the **model** classes hold data;
- **InventoryManager** applies the rules;
- **FileManager** saves and loads;
- the **ui** classes display everything and pass the user's actions to InventoryManager.

### 12.2 How borrowing works
1. The lab assistant types the student's name and roll number.
   - For a roll number seen before, the name is filled in automatically.
   - A line underneath shows what the student already has out. An overdue loan is shown in red.
2. They type part of the component's name in the search box and pick it from the list. The availability line and the details card update.
3. They set the quantity and the return date (pre-filled: 7 days for equipment, 14 for components, none for consumables), then click **BORROW COMPONENT** (or **ISSUE COMPONENT** for a consumable).
4. `BorrowPanel` converts the text. `InputValidator` rejects empty boxes and non-numbers, and `DateUtil` rejects badly written dates.
5. It calls `manager.borrowItem(name, roll, itemId, quantity, returnDate)`, which checks, in order:
   - the name contains only letters and the roll number is valid;
   - **the student has no overdue loan**;
   - the item exists;
   - the quantity is at least 1, the item is not out of stock, the quantity is not more than available, and equipment is not more than 2;
   - for a returnable item, the return date is given and is not in the past.
6. If any check fails, a `LabException` is thrown and the screen shows its message. Nothing is changed.
7. If every check passes:
   - `item.issue(quantity)` reduces the stock;
   - a new `BorrowTransaction` is created with the next ID. Its expected return date is null for a consumable;
   - an activity line is logged and all files are saved.
8. The screen shows "Component issued successfully" with the transaction ID and updates the receipt card. The dashboard and the other pages refresh automatically.

### 12.3 How returning works
1. On the Returns page the user types a transaction ID or roll number. The table filters as they type, and a single match is selected automatically.
2. They check the details and press **RETURN COMPONENT**, then confirm.
3. `manager.returnItem(id)`:
   - checks that the transaction exists;
   - checks that it is a loan. A consumable issue is refused: "TXN-1005 was a consumable issue. Consumables are used up and are not returned.";
   - checks that it is **not already returned**: "TXN-1002 was already returned on 01/10/2026.";
   - calls `item.receive(quantity)`, sets the return date to today, logs the activity and saves.
4. The details card then shows RETURNED and the button changes to ALREADY RETURNED. A second return is therefore blocked twice over: by the disabled button and by the check inside `returnItem()`.

### 12.4 How the inventory quantity is updated
Each `Item` stores `totalQuantity` (what the lab owns) and `availableQuantity` (what is on the shelf). The stock only changes in these places:

- `issue(qty)`: `available -= qty` when borrowing. The method refuses to go below zero.
- `Consumable.issue(qty)` **overrides** this. It calls `super.issue(qty)` and then `useUp(qty)`, which also does `total -= qty`, because a used-up resistor no longer belongs to the lab.
- `receive(qty)`: `available += qty` when returning. It never goes above the total.
- `setTotalQuantity(newTotal)` (when an item is edited) keeps the borrowed units on loan: `available = newTotal − borrowed`. The total can't be set below the number currently borrowed.

Borrowed units are never stored; they are calculated as `total − available`. For a consumable they are always 0.

### 12.5 How file handling works
`FileManager` uses three text files in the `data/` folder. There is one record per line, fields are separated by `|`, and the first line (starting with `#`) names the columns. A `-` means "no date":

```
inventory.txt
# TYPE|ID|NAME|CATEGORY|TOTAL|AVAILABLE|MIN_STOCK
EQUIPMENT|E001|Arduino UNO|Development Board|5|4|2
COMPONENT|C036|IR Sensor Module|Sensor|6|6|3
CONSUMABLE|C001|1kΩ Resistor|Resistor|40|40|15

transactions.txt
# TXN_ID|STUDENT|ROLL_NO|ITEM_ID|ITEM_NAME|QTY|BORROWED|EXPECTED_RETURN|RETURNED|STATUS
TXN-1001|Sneha Kulkarni|EX-2131|E004|Function Generator|2|24/09/2026|01/10/2026|-|BORROWED
TXN-1002|Priya Patil|EX-2117|E003|Digital Multimeter|1|25/09/2026|02/10/2026|01/10/2026|RETURNED
TXN-1005|Amit Shah|EX-2109|C001|1kΩ Resistor|10|28/09/2026|-|-|ISSUED

activity.txt
# DATE_TIME|MESSAGE
05/10/2026 16:40|Rohan Deshmukh returned 4 × IC 555 Timer
```

* **Saving:** after every change, each file is rewritten completely from its `ArrayList` using a `BufferedWriter` (try-with-resources, so it is always closed).
* **Safe saving:** the new contents first go into `inventory.txt.tmp`. Only when that file is complete is it renamed over `inventory.txt` with `Files.move(..., ATOMIC_MOVE)`. If the program crashes or the power fails half-way through, the old file is still intact.
* **Loading:** at start-up each file is read line by line with a `BufferedReader`. Every line is split on `|` and turned back into an object. The TYPE column decides whether an `Equipment`, `ElectronicComponent` or `Consumable` is created. A damaged line is skipped with a warning instead of crashing the program.
* **Encoding:** the files are written in UTF-8 explicitly, so Ω and µ survive. A plain `FileWriter` on Java 8 would use the Windows code page and corrupt them.
* **The `|` character:** it is not allowed in names, so it can never break a line.
* **First run:** if `inventory.txt` doesn't exist, sample data is created and saved.

### 12.6 Where OOP is used
* **Encapsulation:** private fields everywhere. Quantities can only change through `issue()`, `receive()` and `setTotalQuantity()`, so the rules can't be bypassed. `useUp()` is `protected`, so only subclasses can call it.
* **Inheritance:**
  * `Equipment`, `ElectronicComponent` and `Consumable` extend the abstract `Item`;
  * the five pages extend the abstract `BasePage`;
  * `ItemPicker` extends `FlatTextField`, which extends `JTextField`.
* **Polymorphism and overriding:**
  * `borrowItem()` calls `item.getMaxPerBorrow()`, `item.isReturnable()` and `item.issue(qty)` without knowing the item's class.
  * Equipment answers 2 / true; a component answers no limit / true; a consumable answers no limit / false, and its `issue()` also lowers the total.
  * `MainFrame` calls `refresh()` on every page through the `BasePage` type.
* **Abstraction:** you cannot create a plain `Item` or `BasePage`, only their concrete subclasses.
* **Composition:** a `BorrowTransaction` contains a `Student` object.

### 12.7 Where exception handling is used
| Exception | Where | Why |
|---|---|---|
| `LabException` (custom, checked) | thrown by `InventoryManager`, `InputValidator`, `DateUtil`; caught in every page | every rule violation, with a readable message |
| `NumberFormatException` | `InputValidator.parseWholeNumber()`, `FileManager` | "abc" or "2.5" typed as a quantity; damaged numbers in a file |
| `DateTimeParseException` | `DateUtil.parse()`, `FileManager` | invalid dates such as 31/02/2026 or 2026-10-12 |
| `IOException` | `FileManager`, `InventoryManager.load()`, `Main` | the files cannot be read or written; the user is told, and the program keeps running |
| `AtomicMoveNotSupportedException` | `FileManager.writeFile()` | a disk that can't rename in one step; it falls back to a normal replace |
| `IllegalArgumentException` | `Item.issue()`, `FileManager` | a safety check that should never happen in normal use; wrong number of columns in a file |

Other examples are multi-catch (`catch (IllegalArgumentException | DateTimeParseException e)`), try-with-resources, and `try … finally` (the screens refresh even if saving fails).

### 12.8 How the GUI works
* `Main` starts everything on the Swing Event Dispatch Thread with `SwingUtilities.invokeLater`.
* `MainFrame` is the `JFrame`. The sidebar is on the left. On the right there is a top bar (page name and a clock updated by a `javax.swing.Timer`), a `JPanel` with a **CardLayout** holding the five pages, and a status bar.
* Clicking a sidebar item calls `cards.show(pageArea, key)`. Each page is created once and refreshed when shown.
* **Refreshing:** `InventoryManager` keeps a list of listeners (`Runnable`s). After every change it calls them. `MainFrame` registers one that calls `refresh()` on every page, so the dashboard updates the moment an item is borrowed.
* **The search picker:** `ItemPicker` listens to every keystroke with a `DocumentListener`, filters the item list, and shows the matches in a `JPopupMenu`. The popup is made non-focusable, so typing continues in the text box. The arrow keys and ENTER are added with key bindings.
* Forms use `GridBagLayout`. Tables are `JTable`s with custom cell renderers: the alternating rows and the coloured status pills.
* The dark look comes from `Theme` (all colours and fonts in one place) and a few custom-painted components (`paintComponent` overridden). No external look-and-feel library is used.

### 12.9 How transaction IDs are generated
`InventoryManager` keeps an integer `nextTransactionNumber`, which starts at 1001. Each new loan or issue gets `"TXN-" + nextTransactionNumber++`, giving TXN-1001, TXN-1002, …

On start-up, `load()` reads all saved transactions and sets the counter to (highest number found + 1). Numbering therefore continues after a restart, and an ID is never reused.

### 12.10 How low-stock detection works
Every item has a `minimumStock` value, which is set when the item is added and can be changed with Edit, so the threshold is configurable for each item.

- `isLowStock()` returns `available < minimumStock`.
- `getStockStatus()` returns OUT OF STOCK when nothing is available, LOW STOCK when below the minimum, and READY otherwise.

`getLowStockItems()` collects the low items for the dashboard's LOW STOCK box and table. The Inventory status column shows the same status as an amber or red pill. Consumables make this especially useful: every issue lowers their stock for good, so they are the items that most often need restocking.

Example: Function Generator, total 3, minimum 2. When 2 are borrowed, available is 1, which is less than 2, so it shows as LOW STOCK.

### 12.11 How the overdue rule works
Before anything is lent, `borrowItem()` calls `findOverdueLoan(roll)`. This looks through the student's open loans (`getOpenLoansFor(roll)`) for one whose status is OVERDUE, which happens when today is after its expected return date. If one is found, the request is refused with the transaction ID, the item and the due date.

The Borrow page uses the same two methods while the roll number is typed, so the assistant sees the problem before pressing the button.

---

## 13. Validation rules (with the exact messages)

| Situation | Message shown |
|---|---|
| Empty name / roll number / quantity | *Please enter the student name.* (etc.) |
| No component picked | *Choose a component: type part of its name and pick it from the list.* |
| Quantity is text or a decimal | *Quantity must be a whole number, for example 5.* |
| Quantity 0 or negative | *Quantity must be at least 1.* |
| More than available | *Only 2 units are currently available.* |
| Item has 0 available | *Digital Oscilloscope is out of stock.* |
| Equipment, quantity > 2 | *Equipment can be borrowed at most 2 units at a time.* |
| Item ID does not exist | *Item X999 was not found in the inventory.* |
| Student has an overdue loan | *EX-2131 has an overdue loan: TXN-1001 (Function Generator, due 01/10/2026). It must be returned before anything new is issued.* |
| Bad date (31/02/2026, 2026-10-12) | *Enter the expected return date as DD/MM/YYYY, for example 13/10/2026.* |
| Return date before today | *Expected return date cannot be in the past.* |
| Digits in student name | *Student name can only contain letters, spaces and dots.* |
| Duplicate item ID | *An item with ID C001 already exists.* |
| Total < 1 / negative minimum | *Total quantity must be at least 1.* / *Minimum stock cannot be negative.* |
| Edit total below borrowed | *2 units are currently borrowed, so the total cannot be less than 2.* |
| Delete an item that is on loan | *2 units of Function Generator are still with students. It can be deleted after they are returned.* |
| Return an already-returned loan | *TXN-1002 was already returned on 01/10/2026.* |
| Return a consumable issue | *TXN-1005 was a consumable issue. Consumables are used up and are not returned.* |
| Unknown transaction | *Transaction TXN-9999 was not found.* |

Every rule above is checked by `test/labstock/LabStockTest.java` (run `run-tests.bat`), together with the save/reload cycle and safe saving. All 55 checks pass.

---

## 14. Sample data (created on first run)

68 items in total: 15 pieces of equipment, 25 reusable components and 28 consumables, across 15 categories. Quantities are in brackets, and **(c)** marks a consumable.

| Category | Items |
|---|---|
| Development Board (E) | E001 Arduino UNO (5), E002 Arduino Nano (4), E007 ESP32 Dev Board (4), E008 NodeMCU ESP8266 (5), E009 Raspberry Pi 4 Model B (2), E010 8051 Microcontroller Kit (3) |
| Test Equipment (E) | E003 Digital Multimeter (4), E004 Function Generator (3), E005 Digital Oscilloscope (2), E006 DC Power Supply (3), E011 Digital IC Trainer Kit (4), E012 LCR Meter (1), E013 Logic Analyzer (2) |
| Tool (E) | E014 Soldering Station (4), E015 Wire Stripper (5) |
| Resistor | C001 1kΩ (50, c), C002 10kΩ (40, c), C003 220Ω (50, c), C004 330Ω (40, c), C005 4.7kΩ (40, c), C006 100kΩ (30, c), C007 10kΩ Potentiometer (12) |
| Capacitor | C008 100nF Ceramic (30, c), C009 22pF Ceramic (20, c), C010 10µF Electrolytic (20, c), C011 100µF Electrolytic (15, c), C012 1000µF Electrolytic (8, c) |
| IC | C013 IC 555 Timer (15), C014 LM358 Op-Amp (4), C015 LM741 Op-Amp (10), C016 7805 Voltage Regulator (12), C017 7408 Quad AND (10), C018 7400 Quad NAND (10), C019 7474 Dual D Flip-Flop (8), C020 74HC595 Shift Register (6), C021 CD4017 Decade Counter (6), C022 L293D Motor Driver (5) |
| Transistor | C023 BC547 NPN (30, c), C024 BC557 PNP (25, c), C025 2N2222 NPN (20, c), C026 IRF540N MOSFET (3, c) |
| Diode | C027 1N4007 Rectifier (25, c), C028 1N4148 Signal (30, c), C029 5.1V Zener (15, c), C030 Bridge Rectifier W10M (6, c) |
| LED / Display | C031 LED 5 mm Red (100, c), C032 LED 5 mm Green (80, c), C033 RGB LED (15, c), C034 7-Segment Display (10), C035 16x2 LCD Display (6) |
| Sensor | C036 IR Sensor Module (6), C037 LM35 Temperature (8), C038 HC-SR04 Ultrasonic (5), C039 DHT11 Humidity (5), C040 LDR (20), C041 PIR Motion (4), C042 MQ-2 Gas (3) |
| Module | C043 5V Relay Module (6), C044 HC-05 Bluetooth (4), C045 SG90 Servo Motor (6), C046 Piezo Buzzer (15), C047 Tactile Push Button (40, c) |
| Prototyping | C048 Breadboard 830 points (10), C049 Jumper Wires M-M (12, c), C050 Jumper Wires M-F (10, c), C051 Zero PCB (15, c) |
| Power | C052 9V Battery (10, c), C053 9V Battery Clip (15, c) |

* **Sample transactions:** twelve, TXN-1001 to TXN-1012, one each by twelve students:
  * six open loans, two of them overdue (TXN-1001 Sneha Kulkarni, EX-2131 and TXN-1004 Aditi Nair, EX-2101);
  * four returned loans;
  * two consumable issues (TXN-1005: 10 × 1kΩ resistors; TXN-1012: 6 × red LEDs).
* **Try the overdue rule:** enter roll number EX-2131 on the Borrow page.
* **Sample low stock:** three items start below their minimum: E004 Function Generator, C014 LM358 Op-Amp and C026 IRF540N MOSFET.

Items are added with three small helper methods in `InventoryManager`, `addEquipment(...)`, `addComponent(...)` and `addConsumable(...)`, so the list is easy to extend.

---

## 15. Future Scope

* A login for the lab assistant, and separate accounts for multiple labs.
* Barcode or QR scanning of items and student ID cards.
* Fines for overdue items, and automatic e-mail reminders before the due date.
* Partial returns (returning 1 of 2 multimeters).
* A one-click restock button for low-stock items.
* A per-student report of current and past loans, and CSV/PDF export of the history.
* A database (e.g. SQLite) once many labs share the system.
* A request system, where students reserve items before a practical.

---

## 16. Conclusion

LABSTOCK replaces the paper borrowing register of an electronics lab with a small, reliable desktop application. Stock levels are always accurate because every borrow, issue and return passes through one class that checks the rules first. Problems are caught before they happen: over-borrowing, double returns, impossible dates, deleting items that are on loan, and lending to students who haven't returned overdue items.

Separating equipment, reusable components and consumables reflects how a real lab works, and shows clearly how inheritance and method overriding give each kind of item its own behaviour. Low stock and overdue loans are highlighted automatically. Safely written text files keep the data intact between sessions with no database to install, and an automated test program confirms that every rule works.

The project shows how classes, inheritance, collections, exceptions, file handling and Swing work together in a complete program. The separation into model, service and ui packages keeps the code easy to follow and to extend.

---

## 17. Viva Questions and Answers

**Q1. What is your project about?**
LABSTOCK is a Java Swing desktop application for an electronics lab. It keeps the inventory, lends equipment and components, issues consumables, takes items back, records every transaction, and warns about low stock and overdue loans. Data is saved in text files.

**Q2. Why did you not use a database?**
The project is for one lab on one computer, and the data is small (tens of items, hundreds of transactions). Text files are enough, need no installation, and can be opened in Notepad to check. The requirement was also to use Java file handling.

**Q3. Explain the package structure.**
`model` holds the data classes. `service` holds the rules (`InventoryManager`) and the storage (`FileManager`). `ui` holds the Swing screens. The UI never reads files or changes quantities directly; it always goes through `InventoryManager`.

**Q4. Why is `Item` an abstract class?**
Every real item is equipment, a component or a consumable, so a plain "Item" should never exist. `Item` holds what they share (ID, name, quantities) and declares abstract methods (`getLoanDays()`, `getMaxPerBorrow()`) that each subclass must implement in its own way.

**Q5. Give an example of polymorphism in your project.**
`borrowItem()` calls `item.issue(quantity)` without knowing the item's class. For equipment or a component it only lowers the available stock. For a `Consumable`, the overridden `issue()` also lowers the total, because the units are used up. Similarly, `getMaxPerBorrow()` returns 2 for equipment and "no limit" for the others.

**Q6. What is the difference between overloading and overriding? Where do you override?**
Overloading means the same method name with different parameters in one class. Overriding means a subclass replaces a method it inherited, keeping the same signature. `Consumable` overrides `issue()`, `isReturnable()`, `getLoanDays()` and `getMaxPerBorrow()`, and every page overrides `refresh()`.

**Q7. Why do consumables reduce the total quantity?**
A resistor given to a student is soldered into their circuit and never comes back. If only the available count went down, the lab would appear to have 10 resistors "on loan" forever. Lowering the total keeps "borrowed = total − available" true, and makes low-stock alerts show when consumables need re-ordering.

**Q8. What does `protected` mean, and why is `useUp()` protected?**
A `protected` method can be used by subclasses (and classes in the same package) but not by unrelated classes. `useUp()` should only be called by `Consumable.issue()`, so the UI can never lower a total by mistake.

**Q9. Why did you name the class `ElectronicComponent` and not `Component`?**
`java.awt.Component` is the base class of every Swing widget. A class with the same name would cause confusion and import clashes in the UI code.

**Q10. How is encapsulation used?**
All fields are private. Stock can only change through `issue()`, `receive()` and `setTotalQuantity()`, which enforce the rules. For example, `issue()` refuses to make the available quantity negative. `getItems()` returns an unmodifiable list, so other classes can't add or remove items behind the manager's back.

**Q11. What happens when a student tries to borrow 9 Arduino boards when 4 are available?**
The availability line on the Borrow page turns amber while typing ("Only 4 units are currently available."). If BORROW is pressed anyway, `borrowItem()` throws a `LabException` with the same message, and a dialog shows it. The stock is not changed.

**Q12. How does the overdue rule work?**
`borrowItem()` calls `findOverdueLoan(roll)` before anything else is checked about the item. If the student has an open loan past its due date, the request is refused with the transaction ID and due date. The Borrow page shows the same warning in red as soon as the roll number is typed.

**Q13. Why a custom exception (`LabException`)?**
It separates rule violations (expected, shown to the user) from programming errors. Because it is checked, the compiler forces every UI action that calls the manager to handle it, and its message is written for the user, not the developer.

**Q14. How do you prevent a transaction from being returned twice?**
In two ways. The RETURN button is disabled for returned loans, and `returnItem()` itself checks `transaction.isReturned()` and throws "TXN-… was already returned on …". The same method also refuses consumable issues, which are not loans.

**Q15. How is OVERDUE decided? Is it stored in the file?**
It is not stored, because it changes with time. `getStatus()` returns ISSUED for consumables; otherwise RETURNED if there is a return date; otherwise OVERDUE if today is after the expected return date; otherwise BORROWED. The file only records BORROWED, RETURNED or ISSUED.

**Q16. How are transaction IDs kept sequential after restarting?**
On loading, the manager finds the highest number among saved IDs and sets the counter to one more. With the sample data (TXN-1001 to TXN-1012), the next transaction is TXN-1013.

**Q17. Explain your file format.**
One record per line, with fields separated by `|`. The first line is a `#` comment naming the columns. Example: `EQUIPMENT|E001|Arduino UNO|Development Board|5|4|2`. When loading, each line is split with `split("\\|")`. The backslashes are needed because `split()` takes a regular expression, and `|` means "or" in a regex.

**Q18. What is "safe saving"?**
Each file is first written completely to a temporary `.tmp` file, which is then renamed over the real file in one operation (`Files.move` with `ATOMIC_MOVE`). If the program crashes while writing, only the temporary file is incomplete; the real data file still holds the last good version.

**Q19. What if a line in the file is damaged?**
Parsing it throws `NumberFormatException`, `DateTimeParseException` or `IllegalArgumentException`. `FileManager` catches that, prints a warning, skips the line and loads the rest.

**Q20. What is try-with-resources?**
`try (BufferedWriter w = …) { … }` closes the file automatically when the block ends, even if an exception occurs, so files are never left open or half-written.

**Q21. How does the component search box work?**
`ItemPicker` is a text field that listens to every change with a `DocumentListener`. Each time, it keeps the items whose name, ID or category contains the typed text and shows them in a `JList` inside a `JPopupMenu`. The popup is non-focusable, so typing continues normally. The arrow keys, ENTER and ESC are added with key bindings.

**Q22. How does the dashboard update immediately after a borrow?**
`InventoryManager` keeps a list of listeners. After every change it runs them. `MainFrame` registered a listener that calls `refresh()` on every page. This is the Observer pattern.

**Q23. What is CardLayout and why did you use it?**
A layout that stacks panels like cards and shows one at a time. All five pages are added once, and the sidebar calls `cards.show(…)` to switch between them without creating new windows.

**Q24. How did you test the project?**
`LabStockTest` is a plain-Java program (no JUnit needed). It creates a temporary data folder and runs 55 checks: every validation message, borrowing, returning, consumables, the overdue rule, safe saving and reloading. It prints ok or FAIL for each and exits with an error code if anything failed. Run it with `run-tests.bat`.

**Q25. What is the Event Dispatch Thread?**
Swing is not thread-safe, so all GUI work must happen on one thread, the EDT. `Main` uses `SwingUtilities.invokeLater()` to build the window there. The clock uses `javax.swing.Timer`, which also runs on the EDT.

**Q26. How did you make the Swing interface look modern without a library?**
All colours and fonts are kept in one `Theme` class. A few components (buttons, text fields, drop-down, tables) override `paintComponent()` or use custom renderers to draw flat shapes with thin borders. Standard Swing layouts handle the positioning.

**Q27. What are the limitations of your project?**
It is single-user and single-lab, with no login. It doesn't support partial returns. Text files are not suited to very large data or several computers at once. These are covered in Future Scope.
