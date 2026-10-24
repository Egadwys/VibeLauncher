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
- Grid: Home (kolom 3-6, baris 3-7) dan App drawer (kolom 3-7). Pengaturan: long-press area kosong home -> Pengaturan, atau menu ⋮ di drawer.
- Widget: long-press area kosong home -> Widget -> pilih widget. Widget bisa di-drag / dihapus seperti app.
- Drag & drop: long-press ikon (home atau drawer) lalu geser ke sel tujuan. Bar di atas muncul saat drag: Hapus / Info aplikasi / Copot. Lepas di tempat semula = menu. Drop di atas app lain = tukar posisi.
- Tanpa efek sentuhan kotak di home screen & dock.
- Dock dua baris (Pengaturan -> Dock): baris bawah berisi search bar bawaan atau widget Google Search.
- App drawer berhalaman, geser ke samping ala Jelly Bean, dengan indikator titik.

## Belum ada
Multi-page home, resize widget, folder, icon pack, swipe-up untuk drawer.

## GitHub Actions
- **Test APK** (Actions → Test APK → Run workflow): build release APK, hasil di Artifacts.
- **Release APK** (Actions → Release APK → Run workflow, isi versi): build + publish `VibeLauncher-vX.Y.Z.apk` ke Releases.
