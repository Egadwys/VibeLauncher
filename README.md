# VibeLauncher

Launcher Android bergaya Holo (Jelly Bean) + sentuhan Material Design.

## Build
1. Buka folder ini di Android Studio (Koala atau lebih baru, JDK 17), tunggu Gradle sync.
2. Run ke perangkat/emulator (minSdk 21).
3. Tekan tombol Home -> pilih "VibeLauncher" -> Always.

## Fitur
- Holo: tema gelap, aksen cyan #33B5E5, tab "APPS" bergaris bawah, dock translusen, tombol All Apps grid-titik, wallpaper tembus.
- Material: ripple, popup menu, Slider, search pill.
- Ikon rounded: semua ikon (adaptive maupun legacy) dipotong rounded-square seragam (lihat IconShaper.kt, ubah CORNER untuk bentuk lain).
- Grid: Home (kolom 3-6, baris 3-7) dan App drawer (kolom 3-7). Buka dari long-press home screen / menu ⋮ di drawer.
- Long-press ikon di drawer -> Add to Home / App info / Uninstall. Long-press ikon di home -> Remove.

## Belum ada
Multi-page home, drag & drop, widget, folder, icon pack, swipe-up untuk drawer.

## GitHub Actions
- **Test APK** (Actions → Test APK → Run workflow): build release APK, hasil di Artifacts.
- **Release APK** (Actions → Release APK → Run workflow, isi versi): build + publish `VibeLauncher-vX.Y.Z.apk` ke Releases.
