# Project Overview - HabitEngine

## Background & Motivation
As a developer, I spent years looking for a habit tracker that aligned with my lifestyle. Every tool I tried was cluttered with intrusive advertisements, locked behind monthly subscriptions, or compromised my personal privacy by forcing cloud synchronization. Most apps lacked the precise, distraction-free environment that high-performance builders and developers need to track their execution and discipline.

To solve this, I built **HabitEngine**—a distraction-free, offline-first, and privacy-focused habit loop tracker and productivity logger. It is designed specifically for developers, scientists, and high-performance individuals who want to stop setting vague resolutions and start tracking progress with quantitative, computer-science precision.

---

## Core Value Proposition

1. **Psychologically Backed Habit Loop:**
   Instead of using simple checklists, the app forces me to define habits using the *Cue ➔ Routine ➔ Reward* neurological blueprint. By specifying an explicit environmental trigger (the Cue) and a concrete micro-action (the Routine), I make it easy for my brain to wire new positive habits.
2. **Four-Dimensional Life Balance:**
   HabitEngine categorizes habits into four essential life domains: **Health**, **Professional**, **Personal**, and **Social (Family)**. This prevents me from burning out in one area while losing ground in another, reminding me to keep a balanced daily focus.
3. **Interactive Cognitive Logger (Activity Terminal):**
   I wanted a quick way to log my daily workflow, ambient states, focus levels, and distractions. The Cognitive Logger acts as a real-time behavioral terminal on my device, helping me maintain an exact, offline data catalog of my focus.
4. **Absolute Privacy by Design:**
   HabitEngine is 100% offline. It runs no remote telemetry, includes no third-party tracking, and requests zero cloud sync. All data is kept securely on my device in a local database, with manual JSON/CSV backup and restore controls.

---

## Technology Stack & Architectural Decisions

When designing the application, I chose a modern, robust, and clean Android stack:

- **Kotlin:** The primary language, allowing me to write concise, expressive, and type-safe code.
- **Jetpack Compose:** For building a fully reactive, modern Material 3 UI. Its declarative model allowed me to implement custom canvas particle systems, smooth layout transitions, and themes without dealing with heavy XML code.
- **SQLite with Jetpack Room:** Room provides an elegant, object-mapping layer over raw SQLite. It allows me to model composite primary keys and run safe database transactions with compile-time query validation.
- **Kotlin Symbol Processing (KSP):** Replaced KAPT for compiling Room database code and Moshi serialization code, significantly reducing compile times.
- **Hexagonal Architecture (Ports & Adapters):** I isolated my core domain models and business logic from the Android framework and database adapters. By defining abstract port interfaces, I can swap my persistence mechanisms or UI layouts without changing core business rules.

By combining these technologies, I built a high-performance, responsive app that loads instantly, operates entirely offline, and serves as my ultimate dashboard of execution.
