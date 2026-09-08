# Firebase setup

The project intentionally opens and previews without a Firebase project. When `app/google-services.json` is absent, the login/register UI runs in local Preview Mode so Android Studio can render and launch the application immediately.

To enable production Firebase:
1. Create an Android app in Firebase with package `com.irsyadlabs.espbridge` (or change `applicationId` first).
2. Download `google-services.json` into `app/google-services.json`.
3. Enable Authentication → Email/Password.
4. Enable Authentication → Google.
5. Create Realtime Database.
6. Deploy `firebase/database.rules.json`.
7. Optional API mode: deploy `firebase/functions` with Firebase CLI.
8. Rebuild. `app/build.gradle.kts` automatically applies the Google Services plugin when `google-services.json` exists.

Important: the locally generated Setup credentials are for UI/development until the provisioning endpoint is wired to the mobile client. In a production deployment, call `provisionDevice`, store the returned secret in Keystore, and show it once/reveal behind device authentication.
