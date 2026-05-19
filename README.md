# 🏛️ อยุธยา พาซิ่ง! (Ayutthaya Racing)

> เกมแนว Maze Runner/Pac-Man สไตล์ไทย — วิ่งหนีตำรวจ เก็บแต้ม ทำลายสถิติ!  
> Thai-themed maze arcade game built with Java Swing

---

## 📋 สารบัญ (Table of Contents)

1. [ภาพรวมโครงงาน](#-ภาพรวมโครงงาน)
2. [ธีมและขอบเขต](#-ธีมและขอบเขต)
3. [วิธีรันโปรแกรม](#-วิธีรันโปรแกรม)
4. [ฟีเจอร์หลัก](#-ฟีเจอร์หลัก)
5. [สถาปัตยกรรมโปรแกรม](#-สถาปัตยกรรมโปรแกรม)
6. [รายละเอียดแพ็กเกจและคลาส](#-รายละเอียดแพ็กเกจและคลาส)
7. [เกณฑ์การให้คะแนน](#-เกณฑ์การให้คะแนน)
8. [เทคโนโลยีที่ใช้](#-เทคโนโลยีที่ใช้)

---

## 🎯 ภาพรวมโครงงาน

**อยุธยา พาซิ่ง!** เป็นเกมแนว Maze Runner ที่ผู้เล่นรับบทเป็นโจร วิ่งหนีตำรวจ 3 ประเภท ในเขาวงกตอยุธยา  
ผู้เล่นต้องเก็บแต้ม (Point Pickups) เก็บไอเท็มฆ่าตำรวจ (Kill Pickups) และหลบหนีให้นานที่สุดเพื่อทำคะแนนสูงสุด

### การเล่นหลัก
- **เก็บเหรียญ** — ได้คะแนนทีละ 1 แต้ม
- **เก็บไอเท็มพิเศษ** — ฆ่าตำรวจเฉพาะประเภท + โบนัส 2 แต้ม
- **หลบหนีตำรวจ** — ถ้าโดนจับ = Game Over!
- **บันทึกคะแนน** — เก็บลง SQLite Database แล้วเปิด Leaderboard แบบ overlay ในหน้าต่างเดิม

---

## 🏛️ ธีมและขอบเขต

| รายละเอียด | คำอธิบาย |
|-----------|----------|
| **ธีม** | อยุธยา — ศิลปะ วัฒนธรรม สถาปัตยกรรมไทย |
| **แนวเกม** | Maze Runner / Pac-Man Style Arcade |
| **ตัวละครผู้เล่น** | โจร (Thief) — มี Sprite Animation 4 ทิศ × 6 เฟรม |
| **ศัตรู** | ตำรวจ 3 ประเภท (อ้วน, รถตำรวจ, ไก่ตำรวจ) |
| **สไตล์ UI** | ภาพพื้นหลังลายอยุธยา + `Gamefont.ttf` สำหรับตัวอักษรอังกฤษ/ตัวเลข + fallback ฟอนต์ระบบสำหรับภาษาไทย |

---

## 🚀 วิธีรันโปรแกรม

### ความต้องการ
- **Java 17** หรือใหม่กว่า (ใช้ Record, Switch Expression, Pattern Matching)
- **SQLite JDBC Driver** (อยู่ใน `lib/sqlite-jdbc-3.45.1.0.jar`)

### รันจาก IntelliJ IDEA
1. เปิดโปรเจกต์ `merged_final` ใน IntelliJ
2. ตั้ง SDK เป็น Java 17+
3. เพิ่ม `lib/*.jar` เข้า Project Libraries
4. รัน `Main.java` เป็น Main Class

### รันจาก Command Line
```bash
# คอมไพล์ + รัน (ตัวอย่าง)
javac -cp "lib/*" -d out src/**/*.java
java -cp "out;lib/*" Main

# รันเฉพาะเกม (ข้ามเมนู)
java -cp "out;lib/*" Main game

# รัน Maze Editor
java -cp "out;lib/*" Main editor
```

### ไฟล์ Batch สำเร็จรูป
- `start_game.bat` — **ตัวหลัก** คอมไพล์ + รันเกมแบบเมนูหลัก  
- `start_without_compile.bat` — รันเกมโดยใช้ class ที่คอมไพล์ไว้แล้ว (ไม่ต้องคอมไพล์ใหม่)

### สำหรับนักพัฒนา (dev/)
- `dev/run_test.bat` — รันพร้อมแสดง console output + F1 เปิด Debug Mode
- `dev/run_game.bat` — รันเฉพาะโหมดเกม (ข้ามเมนู)
- `dev/run_menu.bat` — รันเฉพาะเมนู
- `dev/run_editor.bat` — เปิด Maze Editor

---

## 🎮 ฟีเจอร์หลัก

### 1. ระบบล็อกอิน/สมัครสมาชิก
- สมัครสมาชิก (Register) — เก็บ Username + SHA-256 Hashed Password ลง SQLite
- เข้าสู่ระบบ (Login) — ตรวจสอบ Username + Password
- เล่นเป็น Guest ได้ (Logout กลับหน้า Login)
- จำกัดความยาว Username สูงสุด 10 ตัวอักษร
- ปุ่มแสดง/ซ่อนรหัสผ่าน (Show/Hide Password)

### 2. ระบบเกม
- **เขาวงกต (Maze)** — โหลดจากไฟล์ Text + รองรับ Background Image
- **Maze Shuffle** — สลับแผนที่อัตโนมัติระหว่างเล่น (ตั้งค่าได้)
- **ตำรวจ 3 ประเภท** ปลดล็อกตามเวลา:
  - Stage 1 (0s): ตำรวจอ้วน — ช้า (45% ความเร็วผู้เล่น)
  - Stage 2 (30s): รถตำรวจ — ปานกลาง (70%)
  - Stage 3 (70s): ไก่ตำรวจ — เร็ว (100%)
- **AI ตำรวจ** — ไล่ล่าผู้เล่นที่ทางแยก (Chase Bias 70%)
- **Turn Slowdown** — ตำรวจชะลอเมื่อเลี้ยว แล้วค่อยเร่งกลับ
- **Pickups** — เหรียญ (Point) + ไอเท็มฆ่าตำรวจ (Kill)
- **Game Over** — โดนตำรวจจับ → แสดงคะแนน + บันทึกลง DB

### 3. ระบบ Leaderboard
- แสดงอันดับ Top 10 ด้วยตัวเลขไทย (๑, ๒, ๓, ...)
- Leaderboard ธีมอยุธยา (พื้นหลังวัด + กรอบทอง) แสดงเป็น overlay ใน window เดิม
- เปิดได้จากเมนูหลัก และหลังบันทึกคะแนนจากหน้า Game Over
- เข้าดูได้จากเมนูหลัก

### 4. Maze Editor (เครื่องมือเสริม)
- สร้าง/แก้ไขแผนที่ด้วย GUI
- เครื่องมือ: Paint, Erase, Fill, Rectangle, Picker
- กำหนด Spawn Zone สำหรับศัตรูและ Pickups
- บันทึก/โหลดไฟล์แผนที่ + Metadata

### 5. Debug Mode (สำหรับพัฒนา)
- กด F1–F7 เปิด/ปิด Debug Overlays:
  - Grid, Walkable Tiles, Player Hitbox, Collision Samples, HUD, History

---

## 🏗️ สถาปัตยกรรมโปรแกรม

### แผนภาพ Package

```
src/
├── Main.java                          # จุดเริ่มต้นโปรแกรม (Entry Point)
│
├── menu/                              # ระบบเมนูหลัก (5 คลาส)
│   ├── MainMenuScreen.java            # Orchestrator หน้าเมนู
│   ├── MainMenuState.java             # สถานะ UI (ปุ่ม, ไทเมอร์, เฟรม)
│   ├── MainMenuButtons.java           # ตั้งค่าปุ่มและจัดการ Click Event
│   ├── MainMenuImageLoader.java       # โหลดภาพสำหรับเมนู
│   └── SessionContext.java            # Singleton เก็บ Username ปัจจุบัน
│
├── game/                              # ตัวเปิดเกม (1 คลาส)
│   └── GameLauncher.java              # สร้าง GamePanel ใน JFrame
│
├── panels/                            # UI เกมหลัก (2 คลาส)
│   ├── GamePanel.java                 # Game Loop, Rendering, Input, Pause Overlay
│   └── GameDebugRenderer.java         # Debug Overlay Rendering
│
├── core/
│   ├── config/                        # ค่าคงที่และ Path (2 คลาส)
│   │   ├── PlayerConfig.java          # ค่าฟิสิกส์ผู้เล่น (ความเร็ว, hitbox)
│   │   └── ProjectPaths.java          # จัดการ Path ไฟล์ทรัพยากร
│   │
│   ├── data/                          # ฐานข้อมูลและ Leaderboard overlay (2 คลาส)
│   │   ├── AppDatabase.java           # SQLite: สมัคร/ล็อกอิน/บันทึกคะแนน
│   │   └── LeaderboardUI.java         # Leaderboard overlay แบบ Custom Paint
│   │
│   ├── debug/                         # เครื่องมือ Debug (2 คลาส)
│   │   ├── DebugSettings.java         # Flag เปิด/ปิด Debug ต่างๆ
│   │   └── PlayerDebugSnapshot.java   # ข้อมูล Debug ต่อเฟรม
│   │
│   ├── entities/                      # เอนทิตี้พื้นฐาน (3 คลาส)
│   │   ├── Entity.java                # Abstract Base Class
│   │   ├── Player.java                # ผู้เล่น (เคลื่อนที่, Sprite, Collision)
│   │   └── Direction.java             # Enum ทิศทาง (UP, DOWN, LEFT, RIGHT, NONE)
│   │
│   ├── gameplay/                      # ระบบเกมเพลย์ (9 คลาส)
│   │   ├── GameplayManager.java       # Orchestrator: Spawn, Collect, Collide
│   │   ├── GameplayConfig.java        # ค่าปรับแต่งทั้งหมด (ความเร็ว, จำนวน, เวลา)
│   │   ├── SpawnDirector.java         # จัดการ Stage + Spawn Timer
│   │   ├── EnemySystem.java           # จัดการตำรวจทั้งหมด (Spawn, AI, Move)
│   │   ├── EnemyState.java            # สถานะตำรวจแต่ละตัว
│   │   ├── EnemyType.java             # Enum ประเภทตำรวจ (FAT, CAR, CHICKEN)
│   │   ├── PickupSystem.java          # จัดการ Pickup ทั้งหมด
│   │   ├── PickupState.java           # สถานะ Pickup แต่ละชิ้น
│   │   └── PickupType.java            # Enum ประเภท Pickup (POINT, KILL_*)
│   │
│   ├── level/                         # ระบบเขาวงกต (10 คลาส)
│   │   ├── Maze.java                  # Tile Grid + Background + Spawn Zone
│   │   ├── io/                        # File I/O
│   │   │   ├── ActiveMazeRegistry.java
│   │   │   ├── MazeTextLoader.java
│   │   │   ├── MazeTextSerializer.java
│   │   │   ├── MazeShuffleConfig.java
│   │   │   ├── MazeFileData.java
│   │   │   └── MazeMetadata.java
│   │   └── mapv2/                     # Fallback Map Layout
│   │       ├── MapV2Layouts.java
│   │       ├── MapV2MazeConverter.java
│   │       └── MapV2CoordinateMapper.java
│   │
│   └── player/                        # ระบบ Collision + Input (6 คลาส)
│       ├── CollisionMap.java           # Interface: ตรวจ Collision
│       ├── MazeCollisionMapAdapter.java # Adapter: Maze → CollisionMap
│       ├── MapV2CollisionMap.java      # CollisionMap สำหรับ Wall Rectangles
│       ├── CollisionDebugInfo.java     # ข้อมูล Debug ของ Collision
│       ├── PlayerController.java       # Keyboard → Direction (Input Handler)
│       └── PlayerModule.java           # Facade: Controller + Player + Collision
│
└── editor/                            # Maze Editor (5 คลาส)
    ├── MazeEditorMain.java
    ├── MazeEditorWindow.java
    ├── MazeEditorPanel.java
    ├── MazeEditorDocument.java
    └── MazeEditorTool.java
```

### Flow การทำงาน

```
Main.main()
  │
  ├── [no args] → MainMenuScreen (เมนูหลัก)
  │     ├── Intro Animation → Login/Register
  │     ├── Login สำเร็จ → Lobby (Start, Leaderboard, How to Play)
  │     ├── Start → Countdown → GameLauncher.launchInFrame()
  │     │     └── GamePanel (Game Loop)
  │     │           ├── PlayerModule.update() → Player.move()
  │     │           ├── GameplayManager.update() → Enemy/Pickup/Score
  │     │           ├── Maze Shuffle Timer
  │     │           ├── ESC → Pause Overlay (Resume/Restart/Exit)
  │     │           └── Game Over → Save Score → Leaderboard Overlay → Back to Menu
  │     └── Leaderboard → LeaderboardUI overlay
  │
  ├── [game] → GameLauncher.main() (เกมอย่างเดียว)
  └── [editor] → MazeEditorWindow (Maze Editor)
```

---

## 📦 รายละเอียดแพ็กเกจและคลาส

### จำนวนคลาสทั้งหมด: **48 คลาส** ใน **7 แพ็กเกจ**

| แพ็กเกจ | จำนวนคลาส | หน้าที่ |
|---------|:---------:|--------|
| `(root)` | 1 | Entry Point |
| `menu` | 5 | ระบบเมนูหลัก, Login/Register |
| `game` | 1 | ตัวเปิดเกม |
| `panels` | 2 | Game Loop + Debug Rendering |
| `core.*` | 34 | ระบบ Core ทั้งหมด |
| `editor` | 5 | Maze Editor |

### คลาสหลักที่สำคัญ

| คลาส | LOC | หน้าที่ |
|------|:---:|--------|
| `GamePanel` | 810 | หัวใจของเกม — Game Loop, Rendering, Input, Pause/Result Menu |
| `Player` | 437 | เอนทิตี้ผู้เล่น — Movement, Collision, Sprite Animation |
| `EnemySystem` | 489 | ระบบศัตรู — Spawning, AI Movement, Chase Bias |
| `MainMenuButtons` | 327 | ระบบปุ่มเมนู — Login, Register, Start, Leaderboard |
| `PickupSystem` | 300 | ระบบ Pickup — Spawning, Collection, Reconciliation |
| `Maze` | 254 | โครงสร้างเขาวงกต — Tile Grid, Rendering, Spawn Zone |
| `LeaderboardUI` | 205 | Leaderboard overlay แบบ Custom Paint สไตล์อยุธยา |
| `AppDatabase` | 172 | SQLite Database — Users + Leaderboard + SHA-256 |

---

## ✅ เกณฑ์การให้คะแนน

### 1. Theme และขอบเขต (2 คะแนน)
- **ธีม:** อยุธยา — เมืองโบราณ ศิลปะไทย
- **ขอบเขต:** เกม Maze Runner แบบเล่นได้จริง มีระบบ Login, Score, Leaderboard, Maze Editor
- **ตัวละคร:** โจร (Thief) กับ ตำรวจ 3 ประเภท (อ้วน, รถ, ไก่)

### 2. ใช้ GUI (1 คะแนน)
- **Java Swing** — JFrame, JPanel, JLabel, JTextField, JPasswordField, JTable, JScrollPane
- **Custom Paint** — `paintComponent()` ใน GamePanel, LeaderboardUI, GameDebugRenderer
- **เมนูเต็มรูปแบบ** — Intro Animation, Login/Register Forms, Lobby, Pause Overlay, Game Over Screen, Leaderboard Overlay

### 3. ส่วนให้ผู้ใช้ใส่ Input (1 คะแนน)
- **JTextField** — กรอก Username (จำกัด 10 ตัวอักษรด้วย DocumentFilter)
- **JPasswordField** — กรอก Password (มีปุ่ม Show/Hide)
- **Keyboard Input** — WASD / Arrow Keys ควบคุมตัวละคร
- **Mouse Input** — คลิกปุ่มเมนู, Pause Overlay, Game Over

### 4. จำนวนคลาส ≥ 3 คลาส (1 คะแนน)
- **มีทั้งหมด 48 คลาส** (รวม enum, record, interface, abstract class)
- ตัวอย่างคลาสหลัก: `Main`, `MainMenuScreen`, `MainMenuState`, `MainMenuButtons`, `MainMenuImageLoader`, `SessionContext`, `GameLauncher`, `GamePanel`, `GameDebugRenderer`, `Player`, `Entity`, `Direction`, `GameplayManager`, `EnemySystem`, `EnemyState`, `EnemyType`, `PickupSystem`, `PickupState`, `PickupType`, `SpawnDirector`, `Maze`, `AppDatabase`, `LeaderboardUI`, `CollisionMap`, `MazeCollisionMapAdapter`, `PlayerController`, `PlayerModule` ...

### 5. ใช้ Selection/Repetition Statement (1 คะแนน)
- **Selection:** `if/else`, `switch` (switch expression ใน `Direction.opposite()`, `PlayerController.mapKeyToDirection()`, `GameplayConfig.maxEnemiesForType()`, `DebugSettings.handleDebugToggle()`)
- **Repetition:** `for` loops (วนเขาวงกต, enemies, pickups), `while` (game loop, maze parsing), `do-while` (ไม่มี แต่มี for+break equivalent)

### 6. ใช้ Array หรือ Collection (1 คะแนน)
- **Array:** `ImageIcon[]` (frames, loginFrames, regisFrames, etc.), `Tile[][]` (maze grid), `boolean[][]` (spawnZoneLayer), `BufferedImage[]` (player sprites), `Rectangle[]` (walls)
- **Collection:** `ArrayList` (enemies, pickups, spawn candidates), `EnumMap` (pressed keys, idle/running frames, enemy sprites), `List` (score entries, maze paths), `LinkedHashMap` (metadata), `LinkedHashSet` (dedup maze paths)

### 7. เขียน Exceptions และ/หรือ Assertions (2 คะแนน)
- **ตัวอย่าง Exception ที่ throw:**
  - `IllegalArgumentException` — ใน `GameLauncher.launchInFrame()` เมื่อ frame เป็น null
  - `IllegalArgumentException` — ใน `MazeTextLoader.loadFromRows()` เมื่อ rows ว่าง
  - `IllegalStateException` — ใน `MainMenuScreen.start()` เมื่อ DB initialize ล้มเหลว
  - `IllegalStateException` — ใน `AppDatabase.hashPassword()` เมื่อ SHA-256 ไม่ available
  - `SQLException` — ใน `AppDatabase` ทุก method (registerUser, authenticate, saveScore, getTopScores)
- **ตัวอย่าง Exception ที่ catch:**
  - `try-catch (SQLException)` — ใน Login/Register flow, Score saving, Leaderboard loading
  - `try-catch (IOException)` — ใน Maze file loading, Sprite loading, Background loading
  - `try-catch (ClassNotFoundException)` — ใน SQLite driver loading
  - `try-catch (Exception)` — ใน Font loading (fallback to Tahoma), Path resolution

### 8. เก็บข้อมูลลง File/DB (2 คะแนน)
- **SQLite Database (`leaderboard.db`):**
  - ตาราง `users` — เก็บ id, username, password_hash (SHA-256), created_at
  - ตาราง `leaderboard` — เก็บ id, username, score, created_at
  - ใช้ `PreparedStatement` ป้องกัน SQL Injection
- **File I/O:**
  - อ่าน/เขียนไฟล์ Maze Text (`.txt`) — `MazeTextLoader`, `MazeTextSerializer`
  - อ่าน/เขียน `active_maze_path.txt` — `ActiveMazeRegistry`
  - อ่าน `shuffle_config.txt` — `MazeShuffleConfig`
  - อ่านไฟล์ Properties (`db.properties`)
  - โหลด Sprite/Image Files — `Player`, `EnemySystem`, `PickupSystem`

### 9. การนำเสนอ (1 คะแนน)
- *(จัดเตรียมแยก)*

### 10. ไฟล์ Presentation (1 คะแนน)
- *(จัดเตรียมแยก)*

### 11. ความถูกต้องในการทำงาน (2 คะแนน)
- เกมทำงานได้จริง — เล่นผ่าน, ควบคุมตัวละคร, ศัตรูไล่ล่า, เก็บ Pickup, คะแนนนับถูกต้อง
- Login/Register — บันทึกและตรวจสอบจาก Database จริง
- Game Over — หยุดเกม + แสดงคะแนนสุดท้ายก่อนโดนหัก + บันทึกคะแนน
- Pause/Resume — Overlay ทันสมัยขึ้น, หยุด/เล่นต่อ/Restart/กลับเมนู
- Fixed-timestep Game Loop (120Hz physics, 60fps render) — เกมทำงานสม่ำเสมอทุกเครื่อง

---

## 🛠️ เทคโนโลยีที่ใช้

| เทคโนโลยี | การใช้งาน |
|-----------|----------|
| **Java 17+** | ภาษาหลัก (Records, Switch Expressions, Text Blocks, Pattern Matching) |
| **Java Swing** | GUI Framework (JFrame, JPanel, Custom Paint, Input Bindings) |
| **SQLite** | ฐานข้อมูลแบบ Embedded (ไม่ต้องติดตั้งเซิร์ฟเวอร์) |
| **JDBC** | เชื่อมต่อ SQLite ผ่าน `sqlite-jdbc-3.45.1.0.jar` |
| **SHA-256** | Hash รหัสผ่านก่อนเก็บลงฐานข้อมูล |
| **Java 2D** | Rendering กราฟิก (Graphics2D, BufferedImage, AffineTransform) |
| **File I/O** | อ่าน/เขียน Maze Files, Config Files, Sprites |

---

## 📁 โครงสร้างไฟล์โปรเจกต์

```
ayutthaya-racing/
├── start_game.bat          # ตัวรัน Production (ผู้ใช้ดับเบิลคลิก)
├── dev/                    # เครื่องมือสำหรับนักพัฒนา
│   ├── run_test.bat        # รัน + แสดง console output + debug
│   ├── run_game.bat        # รันเฉพาะเกม
│   ├── run_menu.bat        # รันเฉพาะเมนู
│   └── run_editor.bat      # เปิด Maze Editor
├── src/                    # ซอร์สโค้ด Java (48 คลาส)
├── resources/              # ทรัพยากร
│   ├── map/                # ภาพพื้นหลังแผนที่
│   ├── maze/               # ไฟล์เขาวงกต (.txt)
│   ├── objects/            # ภาพ Enemies + Pickups
│   ├── sprites/thief/      # Sprite ผู้เล่น (Idle + Running)
│   └── ui/                 # ภาพ UI (ปุ่ม, พื้นหลังเมนู, เฟรม)
├── docs/                   # เอกสารเพิ่มเติม
│   ├── class_diagram.md    # Class Diagram (Mermaid)
│   └── architecture.md     # สถาปัตยกรรมฉบับเต็ม
├── font/                   # ฟอนต์เกม (Gamefont.ttf)
├── lib/                    # ไลบรารี (.jar)
│   ├── sqlite-jdbc-3.45.1.0.jar
│   ├── slf4j-api-2.0.13.jar
│   └── slf4j-simple-2.0.13.jar
├── leaderboard.db          # ไฟล์ SQLite Database
├── db.properties           # ค่าตั้งค่า Database
├── GAMEPLAY_FLOW.md        # เอกสาร Gameplay Flow
└── README.md               # เอกสารนี้
```

---

## 👥 สมาชิกในกลุ่ม

- นาย ธราธร การนา 68102010204  
- นาย ปภูศักดิ์ จันทร์รัตน์ 68102010206  
- น.ส. ณัฏฐวรรณ ศรีอินทรสุทธิ์ 68102010295  
- น.ส. พิมพ์พิศา วงศ์ปราณีกุล 68102010296  

---

> **วิชา:** CP112 — Object-Oriented Programming  
> **ปีการศึกษา:** 2/2568
