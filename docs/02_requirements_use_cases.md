# Requirements and Use Cases - HabitEngine

## 1. Functional Requirements

### 1.1 Habit Loop Lifecycle Management
- **Create Habit Loop:** I must be able to create a new habit by specifying a **Life Domain** (Health, Professional, Personal, Family), a **Cadence** (Daily, Weekdays, Weekends, Monthly), a **Cue (Trigger)**, a **Routine (Action)**, and a **Reward**.
- **Edit Habit Loop:** I must be able to modify the Cue, Reward, and Personal Notes of any existing habit. However, to maintain core identity and prevent cognitive cheating, the core Routine text must be immutable after creation.
- **Delete Habit:** I must be able to remove a habit. Deleting a habit should automatically cascade to delete all completion history logs associated with that habit ID to maintain database integrity.

### 1.2 Completion Tracking & History
- **Interactive Calendar Logging:** I must be able to view a calendar row of past days and click on any day to see active habits applicable on that specific date.
- **Toggle completions:** Clicking on a habit card on the dashboard must toggle its completion status for the selected date. Completion status must be persisted instantly in local SQLite storage.
- **Dynamic Cadence Filter:** The dashboard must dynamically filter the list of visible habits based on the selected calendar date. For example, Weekday habits must not appear on weekends, and Monthly habits should only be selectable on the 1st of each month.

### 1.3 Cognitive Logger (Behavioral Terminal)
- **Log Activity:** I must be able to enter a quick raw text description, associate it with an **Activity Category** (Important, Time Waster, Neutral), and input a duration in minutes.
- **Real-Time Display:** Activity logs must be displayed on the dashboard in a terminal-like chronological list, with color-coded categories indicating focus vs distractions.
- **Delete Activity Log:** I must be able to delete any raw log entry.

### 1.4 Backup and Data Portability
- **JSON Export:** I must be able to trigger a backup export that packages all my habits, logs, and activity logs into a single structured, indented JSON file and copy it to device storage.
- **JSON Import:** I must be able to import a JSON backup file. The application must parse it, wipe the old local SQLite tables completely, and restore all habits and logs transactional-safe.

---

## 2. Non-Functional Requirements

- **Zero Network Dependency (Offline-First):** The application must not perform network calls, contain remote database connections, or use tracking analytics. All storage, analytics, and business logic calculations must run locally on the device CPU.
- **Visual Micro-Reinforcement (Dopamine Sparkles):** Completing a habit loop must trigger an instant interactive particle system on the canvas, reinforcing the psychological reward stage.
- **Sub-10ms Local Latency:** The local state must load reactively. Database queries must utilize Room Flows to stream updates to the UI, guaranteeing that user clicks reflect on the UI with sub-10ms response times.
- **Adaptive Monospace Aesthetics:** The UI must adhere to a premium developer aesthetic utilizing modern typography (e.g. Outfit and Roboto), clean dark-mode colors, and glassmorphic panels. It must support multiple visual theme presets (Cyberpunk Focus / Sunset Calm).

---

## 3. Core Use Cases & User Flows

### Use Case 1: Formulating and Tracking a Habit Loop
1. **Flow:** User clicks "Create My First Loop" or "Add New Habit".
2. **Action:** User selects Professional domain, sets cadence to Weekdays, types Cue ("When I log out of Slack..."), Routine ("I will solve 1 Leetcode problem..."), and Reward ("to enjoy a 5-minute coffee").
3. **Action:** User submits. The new habit is created and appears on the Dashboard.
4. **Tracking:** On a Tuesday, the user logs out of Slack, completes the Leetcode problem, and taps the habit card. A dopamine particle sparkle animation is rendered at the click coordinate, and the completion is stored for the date.

### Use Case 2: Auditing Distractions via Cognitive Logger
1. **Flow:** User opens the Cognitive Logger console on the dashboard.
2. **Action:** After getting distracted, the user types: "Scrolled social media feeds", selects Category: "Time Waster", inputs Duration: "25 minutes", and submits.
3. **Display:** The console prints the entry in red terminal text, adding 25 minutes to the daily distraction metrics. The user gains an immediate reality check of their daily focus balance.

### Use Case 3: Offline Data Portability (JSON Backup)
1. **Flow:** User enters Settings & FAQ.
2. **Action:** User clicks "Export Backup". The app queries all tables via the Room adapter, compiles them into a structured JSON string, and exports the backup to local file storage.
3. **Restore:** Upon installing the app on a new device, the user imports the JSON file. The app validates the JSON format, wipes the local SQLite tables, inserts the backup data in a single SQL transaction, and refreshes the Dashboard reactively.
