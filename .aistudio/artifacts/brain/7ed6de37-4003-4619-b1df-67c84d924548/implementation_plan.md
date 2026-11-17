# Pixel Kick Legacy — 2D Pixel Art Football Game

Pixel Kick Legacy adalah game sepak bola 2D/2.5D bergaya retro pixel art untuk Android yang terinspirasi dari *Football Legacy* dan game arcade klasik. Aplikasi ini menghadirkan pertandingan 11v11 real-time dengan kecerdasan buatan berbasis Finite State Machine (FSM), fisika bola 2.5D (pantulan & ketinggian), serta sistem manajemen karier klub yang lengkap secara offline.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> Seluruh keputusan utama berikut telah dikonfirmasi pada tahap klarifikasi dan menjadi dasar arsitektur aplikasi:

- **Target Runtime (Confirmed)**: Aplikasi dibangun sebagai **game Android native (Kotlin + Jetpack Compose Canvas)** yang langsung terkompilasi menjadi APK dan dapat dimainkan penuh di Streaming Android Emulator maupun perangkat Android.
- **Kamera & Orientasi (Confirmed)**: Menggunakan orientasi **Landscape** dengan sudut pandang **diagonal 2.5D** (*tilted pitch perspective*), lengkap dengan **radar minimap** di bagian bawah tengah, papan skor retro di atas, dan indikator segitiga/cincin di bawah kaki pemain aktif.
- **Penyimpanan Data (Confirmed)**: Seluruh progres **Career Mode**, klasemen liga, kontrak, transfer, statistik gol/assist, dan lemari trofi disimpan **secara lokal dan offline di perangkat** menggunakan database SQLite (Room) tanpa memerlukan login akun.
- **Kontrol & Local Multiplayer (Confirmed)**: Mendukung **kontrol sentuh virtual joystick + tombol aksi** untuk single-player, **dual virtual pad** di satu layar untuk *Local Multiplayer*, serta **dukungan hardware Gamepad/Keyboard** (`InputDevice` / key events).

---

## 1. Overview & Core Concept

- **What It Does**: Menghadirkan pengalaman bermain sekaligus mengelola klub sepak bola dalam balutan grafis pixel art resolusi rendah yang tajam (*nearest-neighbor rendering*). Pemain dapat bertanding langsung di lapangan 11v11 atau mengelola musim kompetisi penuh.
- **Target Audience**: Penggemar game sepak bola retro arcade dan simulasi manajemen klub (*Football Legacy* style) di Android yang menginginkan gameplay cepat, taktis, dan bisa dimainkan kapan saja secara offline.
- **Key Value**: Menggabungkan aksi lapangan real-time (passing, through pass, shooting, sliding tackle, offside, set-piece, pergantian pemain) dengan kedalaman Career Mode (transfer, gaji, anggaran, pengembangan atribut, jadwal liga, dan trofi) dalam satu paket ringan tanpa dependensi internet.

---

## 2. User Experience & Visual Design

### Key User Flows

```
┌───────────────────────────┐
│   Main Menu (Pixel Art)   │
│  [Animated Stadium BG]    │
└─────┬───────┬───────┬─────┘
      │       │       │
      ▼       ▼       ▼
┌──────────┐ ┌──────────────┐ ┌────────────────┐
│QuickMatch│ │ Career Mode  │ │ Tournament /   │
│ & Local  │ │ (Club Hub)   │ │ Training /     │
│ Multi    │ │              │ │ Settings       │
└─────┬────┘ └──────┬───────┘ └───────┬────────┘
      │             │                 │
      │    ┌────────┴────────┐        │
      │    │ Dashboard:      │        │
      │    │ • Klasemen Liga │        │
      │    │ • Skuad & Gaji  │        │
      │    │ • Bursa Transfer│        │
      │    │ • Latihan Skill │        │
      │    │ • Trofi & Stats │        │
      │    └────────┬────────┘        │
      │             │                 │
      ▼             ▼                 ▼
┌──────────────────────────────────────────────┐
│     Live 2.5D Match / Training Pitch         │
│  Scoreboard • Radar • Stamina • Controls     │
│  Halftime Subs • Set Pieces • Celebrations   │
└──────────────────────────────────────────────┘
```

1. **Menu Utama Retro**: Latar belakang stadion pixel art dengan penonton bergerak dan sorot lampu, logo **PIXEL KICK LEGACY**, serta tombol: *Play Match*, *Career Mode*, *Tournament*, *Training*, *Settings*, dan *Exit Game*, disertai transisi layar bergaya arcade dan efek suara 8-bit.
2. **Setup Pertandingan (Quick Match / Local Multiplayer)**: Layar kartu bergaya retro untuk memilih Klub 1 & Klub 2 (beserta warna jersey dan formasi), memilih sisi kontrol (1P vs AI atau 1P vs 2P Dual Pad), stadion, waktu (Siang/Malam), cuaca (*Clear / Rain / Storm*), durasi laga, dan kecepatan game.
3. **Career Mode Hub**: Pengguna memilih berperan sebagai **Manajer Klub** atau **Karier Pemain Bintang**, memilih klub awal, lalu mengelola musim melalui tab: *Next Match & Schedule*, *League Standings*, *Squad & Tactics* (perpanjangan kontrak, formasi, pengembangan skill), *Transfer Market* (negosiasi beli/jual sesuai anggaran & beban gaji), serta *Stats & Trophy Room*.
4. **Pertandingan Real-Time**: Pemain mengendalikan satu pemain aktif (auto-switch saat bertahan atau manual switch), melakukan umpan pendek/jauh, *through pass*, *sprint*, *dribbling*, *sliding tackle*, dan *shooting* dengan bar power. Saat pelanggaran, offside, corner, free kick, atau penalti terjadi, wasit meniup peluit dan laga langsung menyiapkan eksekusi bola mati. Tombol jeda membuka panel taktik dan pergantian pemain dari bangku cadangan.

### Visual Identity & Theme

- **Aesthetic Direction**: *16-Bit Retro Arcade Pitch* — tegas, kontras tinggi, sudut kotak berbingkai pixel (*beveled dark slate borders*), tanpa gaya generik modern yang datar.
- **Color Palette & Mood**:
  - `--background`: `#0D1520` (Midnight Stadium Slate)
  - `--surface`: `#182436` (Tactical Panel Blue) dengan border pixel `#3B5270`
  - `--primary`: `#F5B700` (Championship Gold untuk CTA utama & trofi)
  - `--secondary`: `#00D084` (Arcade Pitch Emerald)
  - `--accent`: `#29B6F6` (Active Player Cyan) & `#FF4757` (Card / Foul Crimson)
  - Rumput lapangan menggunakan pola papan catur dua nada hijau (`#1E7A38` & `#248A41`) dengan garis kapur putih tajam (`#F4F7F5`) dan bayangan drop-shadow di bawah pemain serta bola.
- **Typography & Hierarchy**: Menggunakan font pixel lokal via `font-util` (*Press Start 2P* untuk judul skor, banner gol, dan header retro; *Silkscreen* untuk label tombol, klasemen, dan statistik agar tetap terbaca nyaman di layar ponsel maupun tablet).
- **Sprite & Animasi Pixel Art**:
  - Setiap pemain dirender menggunakan *pixel bitmap spritesheet* terprogram (*nearest-neighbor* `FilterQuality.None`) yang mendukung animasi: **Idle, Walk, Run, Slide Tackle, Kick/Shoot, Header/Jump, Bicycle Kick, Receive, GK Dive, dan Goal Celebration**, lengkap dengan pewarnaan jersey dinamis per klub (baju, celana, kaus kaki) serta variasi warna kulit dan rambut.
  - Stadion menampilkan tribun di sisi atas/samping dengan sprite penonton pixel art yang melompat bersorak saat peluang emas atau gol tercipta.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Native Kotlin + Compose Canvas 2.5D Engine vs. Ekspor Proyek Godot**
  - *Chosen Approach*: Membangun *custom 2D/2.5D game loop* 60 FPS berbasis Kotlin Coroutines + Jetpack Compose `Canvas` dengan sprite `ImageBitmap` ter-cache dan `FilterQuality.None`.
  - *Why*: Menjamin aplikasi terkompilasi penuh menjadi APK Android dan dapat langsung dimainkan di emulator AI Studio, dengan akses langsung ke kontrol sentuh multi-touch Android, hardware gamepad, dan database Room.
  - *Alternatives Considered*: Menghasilkan file skrip `.gd`/`.tscn` statis — ditolak karena tidak dapat dijalankan di lingkungan build Android ini.

- **Decision 2: Synthesized Retro Audio Engine (`AudioTrack`)**
  - *Chosen Approach*: Generator efek suara chiptune PCM real-time (peluit wasit, tendangan keras, umpan, pantulan tiang gawang, sorak gol 8-bit, dan klik menu) menggunakan Android `AudioTrack`.
  - *Why*: Memberikan respons audio retro autentik dengan latensi rendah tanpa risiko file audio rusak atau ukuran APK membengkak, serta dapat di-mute dari menu Settings.

- **Decision 3: Multi-Touch Dual Pad & Gamepad Input Router**
  - *Chosen Approach*: Layer input `pointerInput` multi-pointer terpisah di Compose untuk sisi kiri (Virtual Joystick / D-Pad) dan kanan (Pass, Through, Shoot/Tackle, Sprint/Switch), serta mode **Dual Pad** (P1 di sisi kiri-bawah, P2 di sisi kanan-bawah atau orientasi simetris) yang terintegrasi dengan `KeyEvent` / `MotionEvent` untuk gamepad Bluetooth/USB.
  - *Why*: Memastikan dua pemain bisa bertanding di satu perangkat layar besar (tablet/ponsel landscape) maupun menggunakan stik fisik secara mulus.

- **Decision 4: Offline Local Persistence (Room SQLite)**
  - *Chosen Approach*: Menyimpan seluruh state Career Mode, klub, daftar pemain, jadwal pertandingan, klasemen, riwayat transfer, dan trofi di Room Database.
  - *Why*: Sesuai pilihan pengguna untuk langsung bermain secara offline tanpa hambatan login, sekaligus menjaga struktur data tetap terorganisir dan reaktif melalui Kotlin `Flow`.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                        UI & Screen Layer (Compose)                   │
│  MainMenu • QuickMatchSetup • CareerHub • Tournament • Settings      │
│  LiveMatchScreen (2.5D Canvas + Scoreboard + Radar + Touch/Gamepad)  │
└───────────────┬───────────────────────────────────┬──────────────────┘
                │ StateFlow / Events                │ 60 FPS Snapshot
                ▼                                   ▼
┌───────────────────────────────┐   ┌──────────────────────────────────┐
│   ViewModels & Navigation     │   │      2.5D Match Engine           │
│  • AppNavGraph (Type-Safe)    │   │  • Physics (Ball X, Y, Z + Spin) │
│  • CareerViewModel            │   │  • Rules (Offside, Foul, Cards,  │
│  • MatchViewModel             │   │    Corner, FreeKick, Penalty)    │
│  • TournamentViewModel        │   │  • AI FSM Controller (11 vs 11)  │
│  • TrainingViewModel          │   │  • Formation & Stamina Manager   │
└───────────────┬───────────────┘   └───────────────┬──────────────────┘
                │                                   │
                ▼                                   ▼
┌───────────────────────────────┐   ┌──────────────────────────────────┐
│     Repository & Audio        │   │    Pixel Asset & Sprite Cache    │
│  • CareerRepository (Room)    │   │  • Procedural Pixel SpriteSheet  │
│  • LeagueSimulator            │   │    (Idle/Run/Kick/Slide/Celebrate│
│  • TransferMarketEngine       │   │     per Club Palette)            │
│  • RetroAudioEngine (PCM SFX) │   │  • Pitch, Stadium, Crowd, Rain   │
└───────────────┬───────────────┘   └──────────────────────────────────┘
                │
                ▼
┌───────────────────────────────┐
│   Room Database (SQLite/KSP)  │
│  Clubs • Players • Careers    │
│  Fixtures • Transfers • Trophies│
└───────────────────────────────┘
```

### Data Model & State

- **Club (`ClubEntity`)**: ID, nama klub, kode 3 huruf, warna jersey utama & sekunder, warna celana, anggaran transfer, total beban gaji, rating tim, formasi default (`4-4-2`, `4-3-3`, `3-5-2`, `4-2-3-1`), poin/gol klasemen liga.
- **Player (`PlayerEntity`)**: ID, `clubId`, nama, umur, posisi (`GK`, `DEF`, `MID`, `FWD`), rating keseluruhan (`OVR`), atribut (`pace`, `shooting`, `passing`, `dribbling`, `defending`, `staminaMax`), gaji mingguan, sisa durasi kontrak, nilai pasar, statistik musim (`goals`, `assists`, `appearances`, `yellowCards`), serta flag pemain karier jika memilih mode *Player Career*.
- **CareerSave & Fixture (`CareerEntity`, `FixtureEntity`, `TrophyEntity`)**: Menyimpan mode karier (Manager vs Player), pekan berjalan, musim ke-berapa, daftar jadwal liga round-robin penuh antar klub, hasil skor, serta rekor trofi/penghargaan (Juara Liga, Juara Piala, Top Skor, Pemain Terbaik).
- **MatchRuntimeState (In-Memory 60 FPS)**: Posisi & kecepatan 22 pemain (beserta `stamina`, `fsmState`, `facing`, `animFrame`, `actionCooldown`, `hasYellowCard`), fisika bola 3D (`x`, `y`, `z`, `vx`, `vy`, `vz`, `ownerId`, `lastTouchTeam`), skor, waktu paruh, fase set-piece (`KICKOFF`, `PLAYING`, `CORNER`, `FREE_KICK`, `GOAL_KICK`, `PENALTY`, `OFFSIDE`, `HALFTIME`, `FULLTIME`), dan partikel cuaca/selebrasi.

### AI Finite State Machine (11v11)

Setiap pemain AI mengevaluasi kondisi tiap tick berdasarkan kepemilikan bola dan formasi tim:
- **Saat Tim Menguasai Bola**:
  - `CARRIER_DRIBBLE` / `CARRIER_PASS_OR_SHOOT`: Pembawa bola membaca jarak ke gawang lawan, sudut tembak, tekanan bek lawan, serta rekan yang bebas (*open lane check*) untuk menembak, mengumpan pendek, atau mengirim *through pass*.
  - `SUPPORT_SPACE` / `MAKE_RUN`: Gelandang dan penyerang mencari ruang kosong di sepertiga akhir atau jalur operan terbuka.
- **Saat Tim Bertahan**:
  - `PRESS_BALL`: 1–2 pemain terdekat menekan pembawa bola dan melakukan *interception* atau *slide tackle* jika peluang bersih.
  - `MARK_OPPONENT` / `RECOVER_SHAPE`: Pemain lain menjaga penyerang lawan dan kembali ke jangkar formasi (mempertahankan garis pertahanan agar bisa memancing jebakan *offside*).
- **Goalkeeper AI**:
  - `GK_TRACK_ANGLE`: Memposisikan diri di antara bola dan titik tengah gawang.
  - `GK_DIVE_SAVE` / `GK_SWEEP`: Bereaksi melompat menepis tembakan berdasarkan vektor kecepatan bola (`vx`, `vy`, `vz`) lalu segera mendistribusikan bola ke rekan yang bebas.

### Interactive Component & State Mapping

- **Main Menu & Settings**: Memilih mode memicu navigasi type-safe Compose; *Settings* memperbarui preferensi volume SFX, tingkat kesulitan AI, durasi laga default, dan sensitivitas analog virtual.
- **Quick Match & Tournament**: Mengubah pilihan klub, formasi, cuaca, atau bracket langsung memperbarui preview lapangan 2.5D dan memicu pertandingan dengan parameter yang dipilih.
- **Career Hub Tabs**:
  - Memainkan *Next Match* meluncurkan pertandingan real-time lalu otomatis menyimpan hasil, pencetak gol/assist, kelelahan/perkembangan XP pemain, dan memperbarui klasemen liga serta berita transfer.
  - Membeli/menjual pemain di *Transfer Market* memvalidasi kecukupan anggaran & slot gaji secara atomik di Room, lalu langsung memindahkan pemain ke skuad.
  - Melakukan *Training Drill* (Passing / Shooting / Dribbling) langsung memuat mini-skenario di lapangan yang hasilnya meningkatkan atribut pemain terkait.
- **Live Match HUD & Controls**:
  - Input sentuh kiri menggerakkan pemain aktif; tombol kanan memicu *Short Pass / Switch*, *Through Pass / Lofted*, *Shoot / Slide Tackle* (kontekstual saat membawa bola vs bertahan), dan *Sprint*.
  - Mode *Local Multiplayer* menampilkan kontrol P1 (kiri) dan P2 (kanan) secara bersamaan di layar serta membaca input Gamepad/Keyboard untuk kedua sisi.
