---
name: play-store
description: Production release auditor for Google Play Console. Enforces developer policies, closed testing access questionnaires, Android Vitals thresholds, and App Store Optimization (ASO) rules. Triggered by /play-store.
---

# Goal
Audit HabitEngine updates, metadata changes, feature plans, and Google Play Console production access applications to ensure total compliance with modern Google Play developer distribution policies, preventing production blocks or unexpected store reviews.

# Core Compliance Pillars
1. **Target API Mandates:** Ensure build profiles meet current platform floors. For 2026 releases, submissions must target Android 15 (API 35) minimum, transitioning strictly to Android 16 (API 36) by August 31, 2026.
2. **Android Vitals Thresholds:** Code changes must not push telemetry past Play Store "Bad Behavior" limits. Keep the user-perceived crash rate strictly under 1.09% and monitor ANR thresholds to protect App Store Optimization (ASO) rankings.
3. **Data Safety & Transparency:** Since HabitEngine is **completely OFFLINE-ONLY** (no internet permissions, Room/SQLite database), ensure the Data Safety declaration form in Google Play Console matches this strict offline profile (declaring "No data collected or shared").
4. **Store Metadata Restrictions:** Store listings must avoid promotional spam. Titles must be strictly 30 characters or fewer, short descriptions under 80 characters, and completely free of restricted buzzwords like "Best", "Free", or "#1".

# Google Play Closed Testing & Production Access Questionnaire
When applying for production access after running a 14-day closed test with 20+ testers, Google Play Console requires answers to the following sections. Answers must be grounded in HabitEngine's actual details and formatted to meet strict character limits:

## 1. Tester Recruitment (Character limit: ~500 chars)
*   **Context:** Detail organic and genuine recruitment methods to avoid suspicion of click-farms or automated bot networks.
*   **HabitEngine Details:** Recruited through personal networks (friends/family/colleagues interested in productivity tools) and peer developer forums (e.g. r/AndroidClosedTesting). Manage testers via a dedicated Google Group.
*   **Developer Info:**
    *   Developer Name: Ankit Sudegora
    *   Feedback Email: ankitrai.dev@gmail.com

## 2. Tester Engagement & Usage (Character limit: 300 chars)
*   **Question:** Describe the engagement received, feature utilization, and whether it matches real-world use.
*   **Context:** Show that testers used core features daily. Mention that some features (like BLE accountability sync) were tested more frequently than normal for debugging, while core functions (Habit grid check-offs, Glance widgets, local backups) followed typical daily morning/evening routine tracking.

## 3. Feedback & Actions (Character limit: 300 chars)
*   **Question:** Summarize tester feedback received and changes made as a result.
*   **HabitEngine Specifics:**
    *   *Feedback:* UI lag/freeze during rapid updates -> *Action:* Offloaded Room operations to background thread using Coroutines (`Dispatchers.IO`) to fix ANRs.
    *   *Feedback:* Widget loading layout issues on homescreen -> *Action:* Mapped `@layout/glance_default_loading_layout` placeholder.
    *   *Feedback:* Timezone transitions breaking streaks -> *Action:* Implemented `TimeTransitionReceiver` for midnight refreshes.
    *   *Feedback:* Request to share achievements -> *Action:* Built gallery progress card exporter (`ShareCardGenerator`).

# Output Format
When executing `/play-store`, provide:
1.  **Compliance Status:** Pass/Fail assessment against current Play Store terms.
2.  **Risk Analysis:** Evaluation of permissions, SDK variants, or metadata traps.
3.  **Console Questionnaire Drafts:** Pre-formatted, copy-pasteable answers for Play Console reviews, strictly adhering to the 300/500 character boundaries and HabitEngine's offline identity.