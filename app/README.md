# Nexora Learn — Android Study Planner & Academic Companion (Version 3.0)

Nexora Learn is a modern, feature-rich Android study planner and academic companion application built with Kotlin and Jetpack Compose. It features an ultra-premium white aesthetic UI, robust local and cloud persistence, interactive study tools, and comprehensive syllabus tracking for students.

## Version 3.0 Release

- **Version Name**: `3.0`
- **Version Code**: `3`
- **Pre-built APK**: `releases/v3.0/NEXORA-LEARN-v3.0.apk`
- **SHA-256 Checksum**: `47464f7834d043144b1e8253c69c086e9e71d0e097470df528e6abdd99f50056`

---

## Key Features

- **Ultra-Premium White Aesthetic**: Clean white backgrounds (`#FFFFFF`), soft translucent glass surfaces (`#F8FAFC`), subtle borders, soft depth shadows, and refined Material 3 typography.
- **Smart To-Do Hub & Workload Management**: Material 3 ScrollableTabRow tabbed views for **Academic**, **Revision**, and **Extracurricular** tasks, along with search, sorting (by Date, Priority, Alphabetical), and multi-select batch actions.
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
- `releases/v3.0/` - Official Version 3.0 binary release and release notes.
