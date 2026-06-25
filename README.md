# Friday 🌦️

**Friday** is a premium, modern, glassmorphic Android weather assistant designed to keep you updated with clean layouts, live timezone-aware updates, and immersive features.

Built entirely using **Kotlin**, **Jetpack Compose**, and **Clean Architecture (MVVM)**, Friday offers a visually stunning experience styled with custom *Liquid Glass* components, ambient weather soundscapes, and comprehensive metrics.

---

## 🎨 Premium Visuals

Friday leverages **Liquid Glass glassmorphism** styling, dynamic layout transitions, and high-fidelity themes.

<p align="center">
  <img src="details.png.jpg" width="30%" alt="Weather Details" />
  <img src="darkmode.png.jpg" width="30%" alt="Dark Mode Dashboard" />
  <img src="news.png.jpg" width="30%" alt="Weather News" />
</p>

<details>
<summary>📸 View More Screenshots</summary>

### Onboarding & Setup
<p align="center">
  <img src="onboarding1.png.jpg" width="30%" alt="Welcome" />
  <img src="onboarding2.png.jpg" width="30%" alt="Features" />
  <img src="onboarding3.png.jpg" width="30%" alt="Get Started" />
</p>

### Search & Saved Cities
<p align="center">
  <img src="searchcity.png.jpg" width="45%" alt="Search" />
  <img src="saved_city.png.jpg" width="45%" alt="Saved Cities" />
</p>

### Settings & Configurations
<p align="center">
  <img src="setting1.png.jpg" width="30%" alt="Settings Main" />
  <img src="setting2.png.jpg" width="30%" alt="Theme Customization" />
  <img src="setting3.png.jpg" width="30%" alt="Notification Settings" />
</p>
</details>

---

## 🚀 Key Features

- **Timezone-Aware Live Digital Clock**: Automatically resolves the selected city's local time zone ID and ticks dynamically second-by-second directly below the city name.
- **Glassmorphic Glass-UI Design**: Custom components utilizing frosted glass effects, subtle borders, and vivid gradient backgrounds.
- **Multi-Source Weather Engine**: Utilizes Google Maps / Weather APIs with global fallback support via Open-Meteo.
- **Ambient Weather Soundscapes**: Dynamic audio playbacks (Birds, Rain, Thunder, Wind) matching current weather states.
- **Interactive Home Screen Widget**: Quick-glance forecasts and hourly updates right on your home screen.
- **Smart Weather Notifications**: Local alerts ("Friday Alert") notifying you of critical daily changes.
- **Pollen & Environmental Metrics**: Tracks dust, pollen, and AQI metrics dynamically.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 1.9+
- **UI Framework**: Jetpack Compose (Declarative UI)
- **Asynchronous Flow**: Kotlin Coroutines & Flow
- **Data Persistence**: Jetpack DataStore
- **Dependency Inversion**: MVVM (Model-View-ViewModel) architecture
- **Background Tasks**: WorkManager (for background widget updates)
- **Local Soundscapes**: MediaPlayer integrations for seamless loop playback

---

## ⚙️ Getting Started & Configuration

### Prerequisites
- Android Studio (Koala or newer recommended)
- Android SDK 34+
- Gradle JDK 17+

### 🔑 Local Key Setup
To prevent accidental exposure, API credentials are isolated in your `local.properties` file:

1. Clone this repository.
2. In the project root, open or create `local.properties`.
3. Add your Weather API Key:
   ```properties
   WEATHER_API_KEY=YOUR_SECURE_API_KEY_HERE
   ```
4. Build and run.

---

## 📦 Building the App

To compile and check unit tests locally, run:

```bash
# Clean project and run unit tests
./gradlew clean test

# Build debug APK
./gradlew assembleDebug
```
