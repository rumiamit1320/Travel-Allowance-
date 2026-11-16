# Travel Allowance Manager (Android)

Kotlin + Jetpack Compose Android application for GPS-based travel allowance tracking.

## Project source

The complete Android Studio source project is stored in `TravelAllowanceManager-Completed.tar.xz`. Extract it and open the `TravelAllowanceManager` directory in Android Studio.

## Calculation rule

Combined road distance = Start → Destination + Destination → Return. Full TA applies when combined distance is greater than or equal to 80 km; otherwise half TA applies. Rates and threshold are configurable.

## Cloud and maps setup

See the project README inside the archive for Firebase Authentication, Cloud Firestore, Google Maps SDK and Routes API setup. Do not commit real API keys or `google-services.json` to this public repository.

## Build

The GitHub Actions workflow at `.github/workflows/android-ci.yml` extracts the archive and runs `gradle assembleDebug` on pushes and pull requests. The Google Services Gradle plugin is applied only when `app/google-services.json` exists, allowing CI to compile without exposing Firebase configuration. Add your own Firebase config and API keys for runtime testing.
