# Travel Allowance Manager (Android)

Kotlin + Jetpack Compose Android application for GPS-based travel allowance tracking.

## Project source

The complete Android Studio source project is stored in `TravelAllowanceManager-Completed.zip`. Extract it and open the `TravelAllowanceManager` directory in Android Studio.

## Calculation rule

Combined road distance = Start → Destination + Destination → Return. Full TA applies when combined distance is greater than or equal to 80 km; otherwise half TA applies. Rates and threshold are configurable.

## Cloud and maps setup

See the project README inside the ZIP for Firebase Authentication, Cloud Firestore, Google Maps SDK and Routes API setup. Do not commit real API keys or `google-services.json` to this public repository.

## Build

A GitHub Actions workflow extracts the project and runs `gradle assembleDebug` on pushes and pull requests. The workflow compiles the Android source without requiring Firebase runtime configuration; add your own `google-services.json` for a configured app build and runtime testing.
