# SkyNews

SkyNews is an Android news client I built as an earlier Java/Android project. It focuses on a simple mobile reading workflow: browsing news, viewing details, managing categories, saving favorites, and keeping local reading records.

## Features

- News feed and category-based browsing
- News detail pages with image loading
- Favorites, history, notes, and summaries stored locally with Room
- Category management
- Retrofit-based network layer
- Basic Zhipu AI integration for summary-related functionality

## Tech Stack

- Java
- Android SDK
- Gradle Kotlin DSL
- Retrofit + Gson
- Room
- Glide
- Material Components

## Project Structure

```text
app/src/main/java/com/java/zhangzhiyuan/
├── adapter/     # RecyclerView and ViewPager adapters
├── db/          # Room database, DAOs, and local records
├── model/       # API and local data models
├── network/     # Retrofit client and API service
├── ui/          # Home, category, detail, and personal pages
└── util/        # Utility classes
```

## Build

Create `local.properties` for optional AI summary support:

```properties
ZHIPU_API_KEY=your_api_key
```

Open the project with Android Studio and build the `app` module, or run:

```bash
./gradlew assembleDebug
```

Release APKs are not tracked in this repository.

## Status

This is an older project kept for reference. Some API endpoints or AI-related integrations may require updated credentials or service configuration before running fully.
