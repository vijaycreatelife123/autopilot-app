# System Universe — Integrated Android Design

This project combines the three supplied concepts into one Android app:

1. **System Scanner Pro**
   - Android/device information
   - CPU/RAM/storage
   - Battery level and temperature
   - Network state
   - Display metrics
   - Sensors
   - Camera/microphone/vibration hardware
   - Device security
   - Full scan health score

2. **Tiny Number Universe**
   - Animated space visualization
   - BigDecimal precision engine
   - Divide/multiply by 2
   - Start/pause/stop
   - Precision 10–10,000 digits
   - 30-step history
   - Scale indicator

3. **CDM Universe**
   - Entity/event/relationship/source concepts
   - Local in-memory semantic data model
   - Search
   - Snapshot
   - Relationship graph view

## Product design

Bottom navigation:
- SCAN — device diagnostics
- NUMBER — precision laboratory
- CDM — semantic data universe

The app is local-first. No server or cloud service is required for the Android prototype.

## Build

Open this folder in Android Studio, allow Gradle sync, then:
Build → Build APK(s)

APK output:
app/build/outputs/apk/debug/app-debug.apk

## Notes

The scanner reports what Android exposes. It does not pretend to measure physical quality of a camera, microphone, touchscreen, speaker, or charging circuit without an interactive test.

## AutoPilot Core (v4 design)
The integrated app now has a central AUTO layer that coordinates the Scanner, Tiny Number Universe and CDM Universe.

Flow: **Detect → Decide → Repair/Guide → Verify → Learn → Upgrade**.

- Detects actionable device conditions before taking action.
- Applies only safe, reversible in-app actions; it never silently deletes personal files or changes sensitive settings.
- Keeps an event log for each autonomous cycle.
- Separates **rule/config upgrades** (which can be refreshed from a trusted signed manifest) from **APK/OS upgrades**. Android generally requires the supported installer/user approval for a normal app to replace itself; silent installation is only appropriate for managed-device scenarios with the required device-owner/enterprise controls.
- CDM can serve as the semantic event/relationship layer; Tiny Number Universe remains the precision/calculation engine; Scanner remains the hardware/health source.

This is an architecture for automatic problem solving, not a promise that Android permits arbitrary self-modification.

## AutoPilot UI upgrades
- One-tap appearance themes: Ocean, Purple, Green, Orange.
- One-tap font colour presets: Light, Warm, Green, Purple.
- "One-Click Fix All Safe Problems" on AutoPilot and Scanner screens.
- Automatic actions are limited to safe app-owned cleanup and re-checks; Android-managed problems are handed off to supported system settings rather than falsely marked as fixed.

## AutoPilot Phase 1 + 2 (v3.1.0)
- App branding updated to AutoPilot.
- Existing premium AutoPilot/Core, Number and CDM modules preserved.
- Native System Scanner reads CPU cores/ABI, RAM, storage, battery percentage/temperature/charging state, network transport, display, sensors, camera/microphone capability, vibration and device-security state.
- One-click safe fix clears only AutoPilot-owned cache and then re-scans; it does not delete personal data or silently change Android-managed settings.
- Android network-state permission added for connectivity diagnostics.

Build note: this environment does not contain the Android SDK/Gradle toolchain, so the source package is prepared but APK compilation must be done in Android Studio or another Android build environment.

## Security Center — v3.2.0
- App Lock with PIN/password
- Optional secondary Security Key
- Lock on app exit / background return
- Unlock dialog at app launch/resume
- PIN/key stored as SHA-256 hashes, not plaintext
- No personal files or user data are deleted by security features
- Device biometric integration can be added later without changing the security architecture

Build note: this source package is ready for Android Studio/Gradle build; this environment does not include the Android SDK/Gradle toolchain, so an APK was not compiled here.
