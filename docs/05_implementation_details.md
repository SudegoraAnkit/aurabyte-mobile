# Implementation Details - HabitEngine

This document contains deep-dives into three of the most crucial technical systems in HabitEngine: the domain-isolated **Dynamic Cadence System**, the custom **Compose Canvas Sparkle Physics Simulation**, and the dynamic **JSON-based Localized Dictionary System**.

---

## 1. Dynamic Cadence Logic

To keep HabitEngine clean and prevent cognitive load, I wanted to filter which habits are visible on any selected date (so a weekend-only habit doesn't clutter my dashboard on a Monday). 

Instead of writing custom database queries, I isolated this calendar-checking logic in a pure Kotlin extension function on the `Cadence` domain model in [Models.kt](file:///d:/2026/Project/HabitEngine/app/src/main/java/com/example/core/domain/Models.kt#L82-L117).

### The Implementation
```kotlin
fun Cadence.isApplicableOn(dateStr: String): Boolean {
    val dateParts = dateStr.split("-")
    if (dateParts.size != 3) return true // default fallback
    
    return try {
        val year = dateParts[0].toInt()
        val month = dateParts[1].toInt() - 1 // Calendar is 0-indexed for months
        val day = dateParts[2].toInt()
        
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
        }
        
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        when (this) {
            Cadence.DAILY -> true
            Cadence.WEEKDAYS -> {
                dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY
            }
            Cadence.WEEKENDS -> {
                dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
            }
            Cadence.MONTHLY -> {
                calendar.get(Calendar.DAY_OF_MONTH) == 1
            }
        }
    } catch (e: Exception) {
        true // fallback safe default
    }
}
```

### Key Considerations:
- **Zero-Indexed Months**: In Java/Kotlin's standard `Calendar` utility, months are represented from `0` (January) to `11` (December). Since our database stores ISO-8601 strings like `"2026-06-06"`, the month component (`06`) must be converted by subtracting `1` to align with the correct `Calendar.JUNE` index.
- **Robust Exception Fallback**: If an invalid date string is passed, the system catches the exception and returns a fallback `true`. This prevents the dashboard from crashing and ensures that a minor format glitch never locks a habit from being ticked off.

---

## 2. Dopamine Canvas Particle Simulation

To reinforce the habit completion loop (the "Reward" phase of Charles Duhigg's Habit Loop), I built an interactive particle engine in Jetpack Compose. When I tap the completion checkbox on a habit card, a burst of 25 custom particle circles explodes outward from the exact $X, Y$ coordinate of the tap.

### 2.1 The Particle Model (`UiParticle`)
Each particle runs on its own physics properties:
```kotlin
data class UiParticle(
    val id: Int,
    val initialX: Float,
    val initialY: Float,
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val maxRadius: Float,
    var currentRadius: Float,
    var alpha: Float,
    val decay: Float
)
```

### 2.2 Particle Physics Update Loop
The physics are computed on-the-fly within a high-performance Compose `LaunchedEffect` that acts as our animation clock. It runs only when the particle list is not empty, avoiding unnecessary CPU cycles when the UI is idle:

```kotlin
if (particles.isNotEmpty()) {
    LaunchedEffect(particles.size) {
        while (isActive && particles.isNotEmpty()) {
            withFrameNanos { _ ->
                val iterator = particles.iterator()
                while (iterator.hasNext()) {
                    val p = iterator.next()
                    // Update positions based on velocity vector
                    p.x += p.vx
                    p.y += p.vy
                    
                    // Decelerate radius and fade alpha
                    p.currentRadius = (p.currentRadius - p.decay * 0.1f).coerceAtLeast(0.1f)
                    p.alpha = (p.alpha - p.decay).coerceAtLeast(0f)
                    
                    // Remove decayed particles to free up memory
                    if (p.alpha <= 0f || p.currentRadius <= 0.1f) {
                        iterator.remove()
                    }
                }
            }
            delay(16) // Throttle to target ~60fps performance
        }
    }
}
```

### 2.3 Drawing the Overlay Canvas
The active particles are drawn directly onto the screen via a full-screen Compose `Canvas` overlay that is redrawn reactively:
```kotlin
if (particles.isNotEmpty()) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.95f)
    ) {
        particles.forEach { p ->
            drawCircle(
                color = p.color.copy(alpha = p.alpha),
                radius = p.currentRadius,
                center = Offset(p.x, p.y)
            )
        }
    }
}
```

---

## 3. Localization & Multi-Language System

I wanted HabitEngine to support international users out-of-the-box (English, Spanish, Hindi, German, Japanese, Portuguese). Rather than relying on Android's standard XML string resources (which require updating device system locales and recreating the entire Activity context), I built a fully dynamic, reactive localization engine.

### 3.1 App Language Definition
The languages are declared inside an enum with properties:
```kotlin
enum class AppLanguage(val label: String, val flag: String, val code: String) {
    ENGLISH("English", "🇺🇸", "en"),
    SPANISH("Español", "🇪🇸", "es"),
    HINDI("हिन्दी", "🇮🇳", "hi"),
    GERMAN("Deutsch", "🇩🇪", "de"),
    JAPANESE("日本語", "🇯🇵", "ja"),
    PORTUGUESE("Português", "🇧🇷", "pt")
}
```

### 3.2 The Localization Dictionary
All string mappings are held in a statically defined nested map structure in the `Localizations` object inside [DashboardScreen.kt](file:///d:/2026/Project/HabitEngine/app/src/main/java/com/example/infrastructure/adapters/ui/DashboardScreen.kt):

```kotlin
object Localizations {
    private val strings = mapOf(
        AppLanguage.ENGLISH to mapOf(
            "app_title" to "HabitEngine",
            "search_placeholder" to "Search loops...",
            "delete_habit" to "Delete Loop",
            // ...
        ),
        AppLanguage.SPANISH to mapOf(
            "app_title" to "HabitEngine",
            "search_placeholder" to "Buscar rutinas...",
            "delete_habit" to "Eliminar hábito",
            // ...
        )
    )

    fun getString(key: String, language: AppLanguage): String {
        return strings[language]?.get(key) ?: strings[AppLanguage.ENGLISH]?.get(key) ?: key
    }
}
```

### 3.3 Dynamic On-The-Fly Switching
1. The dashboard UI stores the selected language state using a `rememberSaveable { mutableStateOf(AppLanguage.ENGLISH) }`.
2. When the user taps a flag in the settings drawer, the state is updated instantly.
3. Because Compose reads this state dynamically inside text elements (`Localizations.getString("search_placeholder", selectedLanguage)`), the text fields automatically redraw with the new translation within 1 frame (sub-16ms), without restarting the app, losing active form input, or disrupting running background timers.
