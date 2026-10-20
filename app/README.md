# Nexora Learn — Android Study Planner & Academic Companion

Nexora Learn is a modern, feature-rich Android study planner and academic companion application built with Kotlin and Jetpack Compose. It features an ultra-premium white aesthetic UI, robust local and cloud persistence, interactive study tools, and comprehensive syllabus tracking for students.

## Key Features

- **Ultra-Premium White Aesthetic**: Clean white backgrounds (`#FFFFFF`), soft translucent glass surfaces (`#F8FAFC`), subtle borders, soft depth shadows, and refined Material 3 typography.
- **Smart To-Do Hub & Workload Management**: Tabbed views for **Academic**, **Revision**, and **Extracurricular** tasks, along with search, sorting (by Date, Priority, Alphabetical), and multi-select batch actions.
- **Weekly Breakdown & Analytics**: Visual bar charts tracking completed tasks by subject over the last 7 days.
- **Exam Countdowns**: Real-time automatic countdowns for school and board examinations.
- **Robust Persistence**: Local task persistence via `SharedPreferences` (JSON serialization) combined with optional Supabase cloud synchronization and offline durability.
- **Interactive Widgets & Study Tools**: Study Planner, Subjects Hub, Marks Calculator, Profile, and Coming Soon Chat experience.

---

## Building the Debug APK

To build the debug APK from source using Gradle:

```bash
gradle :app:assembleDebug
```

The compiled APK will be output at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## Running Unit Tests

To run local unit and Robolectric tests:

```bash
gradle :app:testDebugUnitTest
```

---

## Project Structure

- `app/src/main/java/com/example/core/` - Core domain models, repositories (`StudyPlannerRepository`, `SyllabusRepository`), and Supabase integration.
- `app/src/main/java/com/example/ui/` - Jetpack Compose screens, components, animations, and theme architecture (`NexoraTheme`, `Color.kt`).
- `app/src/main/res/` - Android resources, icons, strings, and vector assets.
