# Pixel GBA 1.1 — verification

Verified on 13 September 2026.

## 1.1 update checks

- Native 5x path executes 50 emulated frames in ten calls (no old 4x cap).
- Each of the five toolbar speed chips selects its exact speed and exposes selected state to accessibility.
- All three screen sizes increase monotonically in portrait and landscape, preserve 3:2 and stay within the view; size setting cycles and persists.
- Installed 1.0, created a homebrew auto/battery save, upgraded with install -r to 1.1, verified both save files remain and resumed the cartridge.
- Visual screenshots of the toolbar and all three landscape sizes: screenshots/1.1.

## Passed

- APK built for arm64-v8a, armeabi-v7a and x86_64; Android 8.0+ (min SDK 26), target SDK 35.
- Installed and launched on Android 11 / API 30 x86_64, Pixel 5 emulator using WHPX on Windows.
- Both Android instrumentation tests passed (CoreTest and UiTest). Checks cover actual ARM emulation, nonblank video, nonzero audio PCM, simultaneous input and release, VBA raw and CodeBreaker RAM writes, invalid-cheat rejection, save-state round trip, battery SRAM after closing/reopening, touch cancellation, diagonal input, drag layout persistence, all three graphics filters, accessible buttons and lifecycle checkpoint.
- Host storage checks passed: ROM header and size validation, oversized stream rejection, content identity, atomic replacement.
- Android lint: **No issues found** on the final source.
- APK signature verified using APK Signature Scheme v2.
- APK checked with `zipalign -c -P 16 4`; ARM64 ELF LOAD segments aligned to 0x4000.
- Visual inspection: library, original Pixel Lab ROM in portrait and landscape. No AndroidRuntime exception found during these checks.

## Limits

- Tested with the included original homebrew, not an exhaustive commercial-game compatibility suite.
- Physical ARM Android device, actual Bluetooth controller latency, speaker playback and TalkBack gestures have not been verified on physical hardware. PCM output, input mapping and accessibility nodes were tested programmatically.
- The 16 KB alignment checks do not substitute for running on a physical 16 KB-page device.
- APK uses a debug signing key for local installation, not a Play Store production release.
- ZIP import accepts one GBA game; multiplayer, rewind, achievements and cloud synchronization are not implemented.

Raw instrumentation results are included as `android-tests.xml`. Reproduce tests with `tools/build.ps1 -Test` after connecting a device/emulator. Source and build instructions are included in `PixelGBA-source.zip`.
