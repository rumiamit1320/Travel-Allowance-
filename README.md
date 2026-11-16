# Travel Allowance Manager — Android

Kotlin + Jetpack Compose / Material 3 Android application for employee travel allowance records.

## Implemented in this source build

- Employee profile: name, employee ID, designation, department, office/division, office address, mobile and email.
- Firebase Cloud Firestore profile, TA settings and month-partitioned journey records.
- Anonymous first-run identity with email/password account linking and email/password sign-in for recovery on another device.
- GPS capture for starting point, destination and final return point.
- Google Routes API driving distance for both legs; total distance is the sum of both road distances.
- Configurable full TA, half TA and threshold (defaults: ₹500, ₹250 and 80 km).
- Full TA applies when combined distance is greater than or equal to threshold; half TA otherwise.
- User-selectable journey date (`YYYY-MM-DD`) and records stored under `users/{uid}/months/{YYYY-MM}`.
- Monthly dashboard and statement summary.
- Journey remarks editing and permanent deletion, with Firestore refresh.
- Route review opens Google Maps with the saved start, destination waypoint and return point.
- PDF statement generation using Android PdfDocument.
- Real `.xlsx` export using OpenXML worksheet/workbook parts, shareable through Android's share sheet.
- FileProvider configuration for safe file sharing.

## Calculation

`combinedKm = startToDestinationKm + destinationToReturnKm`

`taAmount = fullRate` when `combinedKm >= thresholdKm`; otherwise `halfRate`.

Each saved journey retains the two leg distances and the combined total. The rates applied at the time of journey creation are saved on the journey record.

## Android Studio setup

1. Open the `TravelAllowanceManager` folder in Android Studio (AGP 8.7.3 / Kotlin 2.0.21).
2. Register package `com.example.travelallowance` in a Firebase project and add the Android app.
3. Download `google-services.json` and place it in `app/`.
4. In Firebase Authentication, enable **Anonymous** and **Email/Password** providers.
5. Create Cloud Firestore and publish `firestore.rules`.
6. In Google Cloud, enable **Routes API** and **Maps SDK for Android**, configure billing, and use separate restricted keys for Android Maps and the Routes service.
7. Put a development Routes API key in `BuildConfigField.kt` and the Android Maps SDK key in the manifest metadata. Never commit real keys or `google-services.json` to a public repository.
8. Sync Gradle, build and install. Grant precise/approximate location permission when prompted.

## API key security — production requirement

The starter currently calls Routes API directly from the app for ease of initial testing. A Routes web-service key embedded in an APK can be extracted. **Do not release this configuration to production.** Move Routes API calls to an authenticated HTTPS backend (for example, Cloud Functions / Cloud Run), validate Firebase ID tokens server-side, and keep the Routes key in server-side secret storage. Restrict the Android Maps SDK key to the application ID and signing certificate. Restrict the server Routes key to the Routes API and backend environment.

## Firebase identity and data access

On first launch, the app creates an anonymous Firebase user. The Profile screen can link that same UID to an email/password credential using **Secure current cloud account**. On a replacement device, select **Sign in to existing account** with the linked email/password to retrieve the same Firestore records. Enable email/password in Firebase Authentication before using these controls. Do not uninstall the app before linking an account if the anonymous identity contains records you need to retain.

Firestore structure:

- `users/{uid}/profile/main`
- `users/{uid}/settings/ta`
- `users/{uid}/months/{YYYY-MM}/journeys/{journeyId}`

Rules restrict each authenticated user to their own user document tree. For an organisation-wide deployment with supervisors and multiple employees, replace per-user ownership with a role-based access model and admin-managed employee identities.

## Current scope / verification status

- The route review action opens Google Maps navigation with the three saved coordinates; it does not render an in-app road-polyline map yet.
- Journey editing currently supports remarks/purpose text; deleting a journey is supported. Changing historical coordinates/date/rates should be handled as a controlled correction workflow with an audit log before official deployment.
- The XLSX export is a valid minimal OpenXML workbook with the employee details and journey register; PDF is a simple printable statement. Organisation-specific bill formats, signatures, claim numbers and sanction fields can be added once a sample approved TA form is supplied.
- The source package was updated in this environment, but a full Android Gradle build and physical-device test could not be run because Gradle/Android SDK tooling and the user's Firebase/Google credentials are not available here. Build and test in Android Studio after completing setup.
