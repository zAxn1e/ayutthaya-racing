# 🏛️ Architecture Overview — อยุธยา พาซิ่ง!

> สถาปัตยกรรมโปรแกรมฉบับเต็ม — อธิบายโครงสร้าง, หลักการออกแบบ, และ Data Flow

---

## 1. ภาพรวมแพ็กเกจ (Package Map)

```mermaid
graph TB
    subgraph ROOT["src/ (Entry Point)"]
        Main["Main.java"]
    end

    subgraph MENU["menu/ — ระบบเมนูหลัก"]
        direction TB
        MainMenuScreen
        MainMenuState
        MainMenuButtons
        MainMenuImageLoader
        SessionContext
    end

    subgraph GAME["game/ — ตัวเปิดเกม"]
        GameLauncher
    end

    subgraph PANELS["panels/ — Game Loop & Rendering"]
        GamePanel
        GameDebugRenderer
    end

    subgraph CORE["core/ — ระบบหลัก"]
        direction TB
        subgraph CONFIG["config/"]
            ProjectPaths
            PlayerConfig
        end
        subgraph DATA["data/"]
            AppDatabase
            LeaderboardUI
        end
        subgraph DEBUG["debug/"]
            DebugSettings
            PlayerDebugSnapshot
        end
        subgraph ENTITIES["entities/"]
            Entity
            Player
            Direction
        end
        subgraph GAMEPLAY["gameplay/ (9 คลาส)"]
            GameplayManager
            SpawnDirector
            EnemySystem
            PickupSystem
            GameplayConfig
        end
        subgraph LEVEL["level/ (10 คลาส)"]
            Maze
            MazeTextLoader
            ActiveMazeRegistry
            MazeShuffleConfig
        end
        subgraph PLAYER["player/ (6 คลาส)"]
            CollisionMap
            MazeCollisionMapAdapter
            PlayerController
            PlayerModule
        end
    end

    subgraph EDITOR["editor/ — Maze Editor"]
        MazeEditorWindow
        MazeEditorPanel
        MazeEditorDocument
    end

    Main --> MENU
    Main --> GAME
    Main --> EDITOR
    MENU --> GAME
    GAME --> PANELS
    PANELS --> GAMEPLAY
    PANELS --> PLAYER
    PANELS --> LEVEL
```

---

## 2. หลักการออกแบบ (Design Principles)

### 2.1 Separation of Concerns (แยกหน้าที่)

| Layer | หน้าที่ | ตัวอย่าง |
|-------|--------|---------|
| **Entry** | จุดเริ่มต้น + CLI dispatch | `Main.java` |
| **UI/Menu** | หน้าจอเมนู, Login/Register, Animation | `menu.*` (5 คลาส) |
| **Game Launch** | สร้าง GamePanel + ใส่ใน JFrame | `GameLauncher` |
| **Game Loop** | Fixed-timestep update + rendering | `GamePanel` |
| **Gameplay** | Score, Enemy AI, Pickup spawn | `core.gameplay.*` |
| **Entities** | ตัวละคร, movement, sprites | `core.entities.*` |
| **Level** | โครงสร้างเขาวงกต, file I/O | `core.level.*` |
| **Collision** | ตรวจการชน, hitbox | `core.player.*` |
| **Data** | SQLite DB, Leaderboard overlay | `core.data.*` |
| **Config** | ค่าคงที่, path resolution | `core.config.*` |

### 2.2 Design Patterns ที่ใช้

```mermaid
graph LR
    subgraph Patterns
        A["Adapter Pattern"]
        B["Facade Pattern"]
        C["Singleton"]
        D["Strategy (Interface)"]
        E["Observer/Callback"]
        F["Record (Immutable Data)"]
    end

    A --> A1["MazeCollisionMapAdapter\n(Maze → CollisionMap)"]
    B --> B1["PlayerModule\n(Player + Controller + Collision)"]
    B --> B2["GameplayManager\n(Enemy + Pickup + Spawn)"]
    C --> C1["SessionContext\n(current user)"]
    D --> D1["CollisionMap interface\n(2 implementations)"]
    E --> E1["GamePanel KeyBindings\n(input → controller)"]
    F --> F1["MazeFileData, MazeMetadata\nMazeShuffleConfig, SwitchReport"]
```

### 2.3 ทำไมถึงออกแบบแบบนี้?

| การตัดสินใจ | เหตุผล |
|------------|--------|
| **CollisionMap เป็น interface** | รองรับ 2 ระบบ collision: tile-based (MazeCollisionMapAdapter) และ rect-based (MapV2CollisionMap) |
| **GameplayManager เป็น Facade** | รวม 3 subsystem (Enemy, Pickup, Spawn) ให้ GamePanel เรียก update/render จุดเดียว |
| **PlayerModule** | แยก Player logic (move, render) ออกจาก GamePanel — ลดความซับซ้อน |
| **Record ใน Java** | ใช้ Record สำหรับ immutable data (MazeFileData, MazeMetadata) — ปลอดภัย, concise |
| **EnumMap** | ใช้ EnumMap แทน HashMap สำหรับ Direction/EnemyType keys — เร็วกว่า, type-safe |

---

## 3. Application Lifecycle

```mermaid
sequenceDiagram
    participant User
    participant Main
    participant MainMenuScreen
    participant AppDatabase
    participant GameLauncher
    participant GamePanel
    participant LeaderboardUI

    User->>Main: java Main
    Main->>MainMenuScreen: main(args)
    MainMenuScreen->>MainMenuScreen: loadFont + loadImages
    MainMenuScreen->>MainMenuScreen: Show Intro Animation

    User->>MainMenuScreen: Skip / Wait
    MainMenuScreen->>MainMenuScreen: Show Login Form

    User->>MainMenuScreen: Enter username + password
    MainMenuScreen->>AppDatabase: authenticate(user, pass)
    AppDatabase-->>MainMenuScreen: true/false

    MainMenuScreen->>MainMenuScreen: Show Lobby (Start, Leaderboard, How)

    User->>MainMenuScreen: Click Start
    MainMenuScreen->>MainMenuScreen: Countdown (3, 2, 1, GO!)
    MainMenuScreen->>GameLauncher: launchInFrame(frame)
    GameLauncher->>GamePanel: new GamePanel()
    GamePanel->>GamePanel: startGameThread()

    loop Game Loop (60fps render, 120Hz physics)
        GamePanel->>GamePanel: PlayerModule.update()
        GamePanel->>GamePanel: GameplayManager.update()
        GamePanel->>GamePanel: paintComponent()
    end

    Note over GamePanel: Player hit by enemy

    GamePanel->>AppDatabase: saveScore(user, finalScore)
    GamePanel->>LeaderboardUI: open(frame overlay)

    User->>GamePanel: Click "Back to Menu"
    GamePanel->>MainMenuScreen: returnToMainMenu()
```

---

## 4. Game Loop Architecture

```mermaid
graph TB
    subgraph "GamePanel.run() — Main Thread"
        A["Calculate delta time"]
        B["Accumulate into accumulator"]
        C{"accumulator >= 1/120s?"}
        D["PlayerModule.update(1/120s)"]
        E["GameplayManager.update(1/120s)"]
        F["accumulator -= 1/120s"]
        G["repaint() → paintComponent()"]
        H["Sleep to target 60fps"]

        A --> B --> C
        C -->|Yes| D --> E --> F --> C
        C -->|No| G --> H --> A
    end

    subgraph "PlayerModule.update()"
        D1["Read keyboard → desired direction"]
        D2["Player.setDirection(dir)"]
        D3["Player.update(delta) — animation"]
        D4["Player.move(map, delta) — collision"]
    end

    subgraph "GameplayManager.update()"
        E1["SpawnDirector.update() — stage progression"]
        E2["Spawn pickups if timer ready"]
        E3["Spawn enemies if timer ready"]
        E4["EnemySystem.update() — AI movement"]
        E5["Collect pickups → update score"]
        E6["Check enemy-player collisions"]
        E7{"Collision?"}
        E8["gameOver = true"]
    end

    D --> D1 --> D2 --> D3 --> D4
    E --> E1 --> E2 --> E3 --> E4 --> E5 --> E6 --> E7
    E7 -->|Yes| E8
```

---

## 5. Collision System Architecture

```mermaid
graph TB
    subgraph "Player Movement Pipeline"
        A["Player.move(collisionMap, delta)"]
        B["Check nextDirection viable?"]
        C["Calculate move distance"]
        D["Sub-step loop (speed / 4px steps)"]
        E["tryMoveTo() — direct check"]
        F{"Blocked?"}
        G["tryMoveWithCornerSlide()"]
        H["tryPartialAdvance()"]
        I["Update position"]
        J["clampToMapBounds()"]
    end

    A --> B --> C --> D --> E --> F
    F -->|Yes| G --> H --> I
    F -->|No| I --> J

    subgraph "CollisionMap Implementations"
        CM["CollisionMap (interface)"]
        MCMA["MazeCollisionMapAdapter"]
        M2CM["MapV2CollisionMap"]

        CM -.-> MCMA
        CM -.-> M2CM
    end

    subgraph "MazeCollisionMapAdapter"
        MA1["9-point sampling (4 corners + 4 edges + center)"]
        MA2["Tolerance margin"]
        MA3["Nudge correction (±1px)"]
    end

    subgraph "MapV2CollisionMap"
        MV1["Rectangle-based wall collision"]
        MV2["Wall deflation (inset)"]
        MV3["Hitbox vs Rectangle[] intersection"]
    end

    E --> CM
    MCMA --> MA1
    M2CM --> MV1
```

---

## 6. Enemy AI System

```mermaid
stateDiagram-v2
    [*] --> Spawning: trySpawnEnemies()
    Spawning --> Moving: valid tile found
    Spawning --> [*]: no valid tile (retry later)

    Moving --> AtIntersection: crossed tile center
    Moving --> Blocked: wall ahead
    Moving --> Moving: straight corridor

    AtIntersection --> ChasePlayer: random < 0.70
    AtIntersection --> RandomDirection: random >= 0.70
    ChasePlayer --> Moving: pick closest-to-player direction
    RandomDirection --> Moving: pick random valid direction

    Blocked --> Stopped: snap to grid
    Stopped --> Moving: chooseDirection()

    Moving --> Dead: killAllOfType() called
    Dead --> RespawnTimer: wait 8 seconds
    RespawnTimer --> Spawning: timer elapsed

    state Moving {
        [*] --> AdvancePosition
        AdvancePosition --> RecoverSpeed: turnRecoveryTimer > 0
        RecoverSpeed --> AdvancePosition
    }
```

---

## 7. Maze Shuffle System

```mermaid
sequenceDiagram
    participant Timer as Shuffle Timer
    participant GP as GamePanel
    participant MTL as MazeTextLoader
    participant GM as GameplayManager
    participant ES as EnemySystem
    participant PS as PickupSystem

    Note over Timer: Every 45s (configurable)

    Timer->>GP: shuffleElapsedSeconds >= interval
    GP->>GP: Pick random maze from shuffle pool
    GP->>MTL: loadMazeSafely(nextMazePath)
    MTL-->>GP: new Maze

    GP->>GP: buildRuntimeState(newMaze, ...)
    Note over GP: Player position preserved

    GP->>GM: onMazeSwitched(newCollisionMap, newMaze)
    GM->>ES: rebindMap(newMap, newMaze)
    GM->>PS: rebindMap(newMap, newMaze)
    GM->>ES: reconcileAfterMazeSwitch(playerBounds)
    Note over ES: Enemies: preserved / relocated / removed
    GM->>PS: reconcileAfterMazeSwitch(playerBounds)
    Note over PS: Pickups: preserved / relocated / removed
    GM-->>GP: SwitchReport
```

---

## 8. Data Layer Architecture

```mermaid
graph TB
    subgraph "SQLite Database (leaderboard.db)"
        T1["users table\n- id (PK)\n- username (UNIQUE)\n- password_hash (SHA-256)\n- created_at"]
        T2["leaderboard table\n- id (PK)\n- username\n- score\n- created_at"]
    end

    subgraph "AppDatabase.java"
        I["initialize() — create tables"]
        R["registerUser(user, pass)"]
        A["authenticate(user, pass)"]
        S["saveScore(user, score)"]
        G["getTopScores(limit)"]
        H["hashPassword(pass) — SHA-256"]
    end

    subgraph "Usage Points"
        MB["MainMenuButtons — login/register"]
        GP["GamePanel — save score on Game Over"]
        LUI["LeaderboardUI — display top 10"]
    end

    MB --> R
    MB --> A
    GP --> S
    LUI --> G
    R --> H
    A --> H
    I --> T1
    I --> T2
    R --> T1
    A --> T1
    S --> T2
    G --> T2
```

---

## 9. File System Layout

```
merged_final/
├── start_game.bat              ← Production launcher (ผู้ใช้)
├── dev/                        ← Dev tools
│   ├── run_test.bat            ← Full console output + debug keys
│   ├── run_game.bat            ← Game-only mode
│   ├── run_menu.bat            ← Menu-only mode
│   └── run_editor.bat          ← Maze Editor
│
├── src/                        ← Source code (48 Java files)
│   ├── Main.java               ← Entry point dispatcher
│   ├── menu/                   ← 5 classes — login/menu system
│   ├── game/                   ← 1 class — game launcher bridge
│   ├── panels/                 ← 2 classes — game loop + debug renderer
│   ├── core/
│   │   ├── config/             ← 2 classes — paths + player constants
│   │   ├── data/               ← 2 classes — database + leaderboard overlay
│   │   ├── debug/              ← 2 classes — debug toggles + snapshots
│   │   ├── entities/           ← 3 classes — entity hierarchy
│   │   ├── gameplay/           ← 9 classes — enemy/pickup/spawn systems
│   │   ├── level/              ← 10 classes — maze + file I/O
│   │   └── player/             ← 6 classes — collision + input
│   ├── editor/                 ← 5 classes — maze editor tool
│   ├── database/               ← UI images (bg_temple.png, ...)
│   └── images/                 ← Background images
│
├── resources/                  ← Game assets
│   ├── map/                    ← Maze background images
│   ├── maze/                   ← Maze layout text files
│   │   ├── active_maze_path.txt
│   │   └── shuffle_config.txt
│   ├── objects/                ← Enemy + pickup sprites
│   ├── sprites/thief/          ← Player sprites (idle + running)
│   └── ui/                     ← Menu UI images
│
├── lib/                        ← Dependencies
│   ├── sqlite-jdbc-3.45.1.0.jar
│   ├── slf4j-api-2.0.13.jar
│   └── slf4j-simple-2.0.13.jar
│
├── font/                       ← Custom Thai game font
├── db.properties               ← Database configuration
├── leaderboard.db              ← SQLite database (runtime)
│
├── docs/                       ← Documentation
│   ├── class_diagram.md        ← Complete class diagrams (Mermaid)
│   └── architecture.md         ← This file
│
├── GAMEPLAY_FLOW.md            ← Gameplay mechanics reference
└── README.md                   ← Project overview + grading criteria
```

---

## 10. Threading Model

```mermaid
graph LR
    subgraph "EDT (Event Dispatch Thread)"
        A["Swing UI Events"]
        B["Mouse clicks / Key presses"]
        C["paintComponent()"]
        D["Timer callbacks (menu)"]
    end

    subgraph "Game Thread"
        E["GamePanel.run()"]
        F["Physics update loop"]
        G["repaint() call"]
    end

    subgraph "Shared State (volatile)"
        H["runtimeState"]
        I["isRunning"]
        J["pauseMenuOpen"]
        K["activeMazePath"]
    end

    B -->|"KeyEvent"| F
    F -->|"update physics"| H
    G -->|"triggers"| C
    C -->|"reads"| H
    E --> F --> G

    style H fill:#ff9,stroke:#333
    style I fill:#ff9,stroke:#333
    style J fill:#ff9,stroke:#333
    style K fill:#ff9,stroke:#333
```

**Thread Safety:**
- `runtimeState`, `isRunning`, `pauseMenuOpen`, `activeMazePath` ใช้ `volatile`
- `RuntimeState` เป็น `record` (immutable) — ปลอดภัยในการอ่านข้าม thread
- Input (KeyEvents) มาจาก EDT → เขียนลง `PlayerController` (EnumMap) → อ่านจาก Game Thread
- `repaint()` เรียกจาก Game Thread → `paintComponent()` ทำงานบน EDT

---

## 11. Exception Handling Strategy

| บริเวณ | Exception | การจัดการ |
|--------|-----------|----------|
| **Database** | `SQLException`, `ClassNotFoundException` | catch → แสดง error dialog / fallback |
| **File I/O** | `IOException` | catch → fallback to default maze / null sprite |
| **Sprite Loading** | `IOException` | catch → render fallback shape (circle/arc) |
| **Font Loading** | `FontFormatException` | catch → fallback to `new Font("Tahoma")` |
| **Maze Parsing** | `IllegalArgumentException` | throw → if rows empty or unequal width |
| **Null Arguments** | `IllegalArgumentException` | throw → `GameLauncher.launchInFrame(null)` |
| **DB Init Failure** | `IllegalStateException` | throw → wraps original exception |
| **Path Resolution** | `URISyntaxException` | catch → fallback to `user.dir` walking |
