# VibeLauncher

Launcher Android modern dengan perpaduan **Material Design 3** dan **Real Liquid Glass**.

## Build
1. Buka folder ini di Android Studio (Koala atau lebih baru, JDK 17), tunggu Gradle sync.
2. Run ke perangkat/emulator (minSdk 21+).
3. Tekan tombol Home -> pilih "VibeLauncher" -> Always.

## Desain & Fitur
- **Material Design 3**:
  - Palet warna modern (Material Azure/Indigo accent, surface containers, tonal elevations).
  - Floating pill search bar di home screen dan dock.
  - Pengaturan & Kustomisasi berbasis Material 3 Cards, Material Slider, dan SwitchMaterial.
  - Tombol All Apps bergaya 4-tile squircle modern.
  - Dialog (28dp corner radius) dan popup menu berdesain Material 3.
- **Real Liquid Glass (Efek Kaca Asli)**:
  - Panel kaca multi-layer: frosted translucent backdrop, specular gloss curve (pantulan kilau cahaya atas), dan refraction rim bevel (tepi bias cahaya kaca).
  - Hardware backdrop blur di Android 12+ (`RenderEffect.createBlurEffect`) saat app drawer dibuka, mengaburkan wallpaper dan widget di latar belakang layaknya kaca buram asli.
  - Ikon aplikasi dengan sentuhan Liquid Glass: kilau specular gloss miring dan tepi bevel kaca.
  - Floating capsule dock & drag target bar terapung dengan efek kaca cair.
- **Tema Tersedia (Kustomisasi)**:
  - *Material Design (Default)*: Elegan, bersih, dengan kontras tinggi dan aksen Material 3.
  - *Real Liquid Glass – Gelap*: Panel kaca buram gelap berbayang dengan kilau putih bersih.
  - *Real Liquid Glass – Terang*: Panel kaca kristal bening dengan teks kontras dan pencahayaan lembut.
- **Grid & Kustomisasi**:
  - Home screen: kolom 3–6, baris 3–7.
  - App drawer: kolom 3–7.
  - Dock dua baris opsional dengan search bar bawaan atau widget pencarian Google.
- **Gestur & Interaksi**:
  - Swipe atas di home = buka drawer.
  - Swipe bawah di drawer = tutup drawer.
  - Swipe bawah di home = buka panel notifikasi sistem.
  - Drag & drop aplikasi & widget dengan bar aksi terapung (Hapus, Info Aplikasi, Copot).
  - Ubah ukuran widget langsung di home screen.
