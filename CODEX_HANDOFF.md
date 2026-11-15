# Codex hand-off — Mosque Auto-Silent v0.2

Repository target: `https://github.com/MosqueSilent/MosqueAutoSilent`

## Goal
Take this v0.2 Android prototype to a reproducible debug APK build, fix verified build/runtime issues, and push the finished source plus build workflow to the repository. Do not claim success until the APK exists and can be retrieved from a verified build artifact.

## Start here
1. Read `README.md`, `docs/BUILD_STATUS.md`, `docs/ARCHITECTURE.md`, `docs/TEST_PLAN.md`, and `docs/SOURCE_DATA.md`.
2. Treat the contents of this directory as the v0.2 baseline. Ignore any older v0.1 code; this hand-off package contains only the v0.2 tree.
3. Inspect the complete Android app and workflow before editing. Report the exact baseline and problems found.
4. Check the actual available JDK, Gradle, Android SDK and build-tools versions in the execution environment. Do not assume they exist.

## Locked product requirements
- Mosque pilot: Tarfaya Mosque, Meknes, Morocco; MAWAQIT listing previously identified as 5655.
- Tarfaya coordinates `33.90066, -5.54761` are approximate and must be verified onsite. Default geofence radius 100 m where the app uses that default; retain adjustable radius.
- Activation requires selected SSID in range AND inside geofence AND current time at/after Iqama minus 30 seconds and before Iqama plus D.
- Fixed lead time L = 30 seconds; default automatic restore duration D = 10 minutes after Iqama, adjustable 1–30 minutes.
- Save and restore the phone's original ringer profile (Normal/Vibrate/Silent); never blindly restore Normal.
- Provide local early-release control labelled End Prayer / Restore Sound.
- The current Imam release is local to one device. Do not represent it as mosque-wide. Multi-device Imam release requires a deployed authenticated backend and connected attendee clients; scaffold only exists in `backend/worker/`.
- The 2026 MAWAQIT calendar is prayer-time data, not Iqama. Keep manual Iqama for prototype; do not fabricate Iqama values.
- Browser demo is simulation only; native Android app is the device-control implementation.

## Engineering tasks
1. Make the project build reproducibly from a clean checkout using the included Android Gradle configuration and GitHub Actions workflow. Resolve real compatibility issues between AGP, Gradle, JDK, compileSdk 36, Android build tools and dependencies; prefer supported stable versions. Keep minSdk 26 and the intended target SDK unless Android compatibility/security requirements justify a documented change.
2. Run `./gradlew --no-daemon clean assembleDebug` (or the Windows equivalent) and fix all compiler/resource/manifest/dependency errors.
3. Run the browser smoke test in `tests/demo_logic_smoke.js` and preserve all existing passing scenarios. Add focused tests for the timing and restore rules where practical.
4. Review Android 17/current Android Wi-Fi scan, location, geofence, notification-policy access, background execution and battery restrictions. Do not silently bypass user-granted permissions. Make the app explain missing permissions and fail safely.
5. Verify the one-shot Wi-Fi scan scheduled for Iqama − 60 seconds, L=30 seconds activation, D restore and safety exits. Clearly document anything that cannot be proven without a physical Samsung Galaxy S23 Ultra test.
6. Confirm that the saved original ringer mode is restored exactly once on all supported exits, including process/reboot/permission edge cases. If the current architecture cannot guarantee this, implement a robust, testable state persistence/restore approach and document its limits.
7. Update app `versionName` to `0.2.0` (or a clearly justified build version), README and build-status documentation to match actual results.
8. Improve `.github/workflows/android-build.yml` so it builds on push and `workflow_dispatch`, installs the exact required SDK components, runs tests, builds the debug APK, and uploads it as an artifact. Ensure the workflow's working directory and wrapper invocation match this repository layout.
9. Add/confirm `.gitignore` for local.properties, `.gradle/`, build outputs, IDE files and generated APKs. Never commit credentials, signing keys, local SDK paths, or personal tokens.
10. Push source and workflow to `main` (or a clearly named branch with a PR if main is protected). Run the workflow and retrieve the uploaded APK artifact. Report the commit SHA, workflow URL/status, artifact name and exact limitations.

## Important safety/accuracy constraints
- Do not add a production signing key or publish to Google Play.
- Do not deploy the Cloudflare Worker or create paid services without explicit approval.
- Do not introduce paid APIs or paid infrastructure.
- Never say the APK is built unless the APK artifact is actually present and verified.
- Before destructive Git operations or overwriting existing repository content, inspect Git state and report the plan. Keep the user's existing work.

## Expected deliverables
- Clean Android source tree in the repository.
- Passing browser smoke test and Android debug build.
- GitHub Actions run with downloadable `app-debug.apk` artifact.
- Updated `docs/BUILD_STATUS.md` listing what was tested on CI versus what still requires the physical Samsung phone.
