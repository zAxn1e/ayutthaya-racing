# 📐 Class Diagram — อยุธยา พาซิ่ง!

> Complete class relationship diagram for all 48 classes in the project.

---

## 1. Top-Level Application Flow

```mermaid
graph LR
    Main["Main"]
    MMS["menu.MainMenuScreen"]
    GL["game.GameLauncher"]
    MEW["editor.MazeEditorWindow"]
    GP["panels.GamePanel"]

    Main -->|"no args"| MMS
    Main -->|"game"| GL
    Main -->|"editor"| MEW
    MMS -->|"Start Game"| GL
    GL -->|"creates"| GP
    GP -->|"Exit to Menu"| MMS
    GP -->|"Restart"| GL
```

---

## 2. Menu Package (`menu.*`)

```mermaid
classDiagram
    class MainMenuScreen {
        -MainMenuState state
        -MainMenuImageLoader imageLoader
        -MainMenuButtons buttons
        +main(args) void
        +returnToMainMenu() void
        -start() void
        -setupFirstScreen() void
        -createTimers() void
        -launchGameFromCountdown() void
    }

    class MainMenuState {
        +JFrame appFrame
        +JPanel mainPanel
        +JLabel label, regisBtn, loginBtn, ...
        +JTextField loginUserField
        +JPasswordField loginPassField
        +Font customFont
        +int currentFrame
        +boolean isRegisMode, isRunning, ...
        +Timer introTimer, loopTimer, countTimer
        +ImageIcon[] frames, loginFrames, ...
        +loadCustomFont() void
    }

    class MainMenuButtons {
        -MainMenuState state
        -hideAll() void
        +goToEnter() void
        +goToLogin() void
        +goToHow() void
        +setupEnterButton() void
        +setStartBtn() void
        +setupLeaderButton() void
        +setupRegisButton() void
        +setupLoginButton() void
    }

    class MainMenuImageLoader {
        +prepareImages(state) void
        -loadOrPlaceholder(fname, w, h) ImageIcon
    }

    class SessionContext {
        -String currentUser$
        +getCurrentUser()$ String
        +setCurrentUser(username)$ void
    }

    MainMenuScreen --> MainMenuState : uses
    MainMenuScreen --> MainMenuButtons : uses
    MainMenuScreen --> MainMenuImageLoader : uses
    MainMenuButtons --> MainMenuState : reads/writes
    MainMenuButtons --> SessionContext : sets user
    MainMenuButtons --> AppDatabase : login/register
    MainMenuButtons --> LeaderboardUI : opens overlay
```

---

## 3. Game Panel (`panels.*`)

```mermaid
classDiagram
    class GamePanel {
        -DebugSettings debugSettings
        -PlayerController playerController
        -MazeShuffleConfig mazeShuffleConfig
        -RuntimeState runtimeState
        -Thread gameThread
        -boolean isRunning, pauseMenuOpen
        +startGameThread() void
        +stopGameThread() void
        +run() void
        -paintComponent(g) void
        -handleDebugToggle(keyCode) boolean
        -exitToMainMenu() void
        -restartCurrentGame() void
    }

    class RuntimeState {
        <<record>>
        +Maze maze
        +CollisionMap collisionMap
        +Player player
        +PlayerModule playerModule
        +GameplayManager gameplayManager
        +String mazePath
    }

    class GameDebugRenderer {
        +renderDebugOverlay(g2, ...) void
    }

    GamePanel *-- RuntimeState : contains
    GamePanel --> GameDebugRenderer : delegates rendering
    GamePanel --> PlayerModule : update/render
    GamePanel --> GameplayManager : update/render
    GamePanel --> DebugSettings : reads flags
```

---

## 4. Core Entities (`core.entities.*`)

```mermaid
classDiagram
    class Entity {
        <<abstract>>
        #float x, y, width, height
        #boolean active
        +update(delta) void*
        +render(g) void*
        +getBounds() Rectangle
        +collidesWith(other) boolean
        +getCenterX() float
        +getCenterY() float
    }

    class Player {
        -float speed
        -int tileSize
        -EnumMap~Direction,BufferedImage~ idleFrames
        -EnumMap~Direction,BufferedImage[]~ runningFrames
        -Direction currentDirection, nextDirection, facingDirection
        -boolean moving
        +setDirection(dir) void
        +move(map, delta) void
        +reset(x, y) void
        +getGridX() int
        +getGridY() int
    }

    class Direction {
        <<enum>>
        UP(0, -1)
        DOWN(0, 1)
        LEFT(-1, 0)
        RIGHT(1, 0)
        NONE(0, 0)
        +int dx, dy
        +opposite() Direction
    }

    Entity <|-- Player
    Player --> Direction : uses
    Player --> CollisionMap : collision checks
    Player --> PlayerConfig : reads constants
```

---

## 5. Gameplay System (`core.gameplay.*`)

```mermaid
classDiagram
    class GameplayManager {
        -SpawnDirector director
        -PickupSystem pickupSystem
        -EnemySystem enemySystem
        -Player player
        -int score
        -Integer finalScoreSnapshot
        -boolean gameOver
        +update(delta) void
        +render(g2) void
        +getScore() int
        +getFinalScore() int
        +isGameOver() boolean
        +onMazeSwitched(map, maze) SwitchReport
    }

    class SpawnDirector {
        -double elapsedTime
        -int currentStage
        -double enemySpawnCooldown
        -double pointSpawnCooldown
        -double killSpawnCooldown
        +update(delta) void
        +isTypeUnlocked(type) boolean
        +shouldSpawnEnemy() boolean
        +shouldSpawnPoint() boolean
        +shouldSpawnKill() boolean
    }

    class EnemySystem {
        -CollisionMap collisionMap
        -Maze maze
        -List~EnemyState~ enemies
        -EnumMap~EnemyType,Image~ sprites
        +trySpawnEnemies(director, px, py) void
        +update(delta, px, py) void
        +killAllOfType(type) void
        +getCollidingEnemies(bounds) List
        +render(g2) void
        +reconcileAfterMazeSwitch(bounds) ReconcileReport
    }

    class EnemyState {
        -EnemyType type
        -float x, y, width, height
        -float baseSpeed, currentSpeed
        -Direction direction
        -boolean alive
        -double respawnTimer, turnRecoveryTimer
        +changeDirection(dir) void
        +advance(delta) void
        +snapToGrid() void
        +kill() void
        +respawn(tx, ty) void
    }

    class EnemyType {
        <<enum>>
        FAT("police_fat", 0.45f, 1)
        CAR("police_car", 0.70f, 2)
        CHICKEN("police_chicken", 1.00f, 3)
        +String assetKey
        +float speedMultiplier
        +int unlockStage
    }

    class PickupSystem {
        -CollisionMap collisionMap
        -Maze maze
        -List~PickupState~ pickups
        -Map~PickupType,Image~ sprites
        +trySpawnPoints(bounds) void
        +trySpawnKill(bounds, director) void
        +collectIntersecting(bounds) List
        +prune() void
        +render(g2) void
    }

    class PickupState {
        -PickupType type
        -int tileX, tileY
        -Rectangle bounds
        -boolean active
        +deactivate() void
        +relocateToTile(tx, ty, ts) void
    }

    class PickupType {
        <<enum>>
        POINT("object_point", null)
        KILL_FAT("kill_fat_00000", FAT)
        KILL_CAR("kill_car_00000", CAR)
        KILL_CHICKEN("kill_chicken_00000", CHICKEN)
        +String assetKey
        +EnemyType targetEnemy
        +isKillPickup() boolean
    }

    class GameplayConfig {
        <<final>>
        +STAGE_1_TIME = 0.0$
        +STAGE_2_TIME = 30.0$
        +STAGE_3_TIME = 70.0$
        +MAX_FAT_ENEMIES = 2$
        +MAX_CAR_ENEMIES = 2$
        +MAX_CHICKEN_ENEMIES = 1$
        +ENEMY_CHASE_BIAS = 0.70$
        +POINT_SCORE_VALUE = 1$
        +ENEMY_TOUCH_PENALTY = 30$
        ...
    }

    GameplayManager --> SpawnDirector
    GameplayManager --> PickupSystem
    GameplayManager --> EnemySystem
    GameplayManager --> Player : reads position/bounds
    EnemySystem --> EnemyState : manages list
    EnemySystem --> EnemyType : iterates types
    EnemySystem --> GameplayConfig : reads constants
    PickupSystem --> PickupState : manages list
    PickupSystem --> PickupType : iterates types
    PickupType --> EnemyType : targets
    SpawnDirector --> GameplayConfig : reads thresholds
    SpawnDirector --> EnemyType : checks unlock
```

---

## 6. Level System (`core.level.*`)

```mermaid
classDiagram
    class Maze {
        -Tile[][] tiles
        -boolean[][] spawnZoneLayer
        -int widthInTiles, heightInTiles, tileSize
        -BufferedImage backgroundImage
        +setTile(x, y, tile) void
        +getTile(x, y) Tile
        +isWalkable(x, y) boolean
        +isSpawnZone(x, y) boolean
        +hasAnySpawnZone() boolean
        +render(g) void
    }

    class Tile {
        <<enum>>
        EMPTY('.', true)
        WALL('#', false)
        PLAYER_SPAWN('P', true)
        GHOST_SPAWN('G', true)
        PELLET('o', true)
        POWER_PELLET('O', true)
        +char symbol
        +boolean walkable
        +fromSymbol(c) Tile
    }

    class MazeTextLoader {
        <<utility>>
        +loadFromFile(path, ts, bg, bgFile)$ Maze
        +loadFileData(path, ts, bg, bgFile)$ MazeFileData
        +loadFromRows(rows, ts, bg, bgFile)$ Maze
    }

    class MazeTextSerializer {
        <<utility>>
        +toRows(maze)$ List~String~
        +toText(maze, metadata)$ String
        +encodeSpawnZoneRows(maze)$ String
    }

    class ActiveMazeRegistry {
        <<utility>>
        +readActiveMazePath(configPath)$ String
        +resolveRuntimeMazePath(cfg, legacy, def)$ String
        +writeActiveMazePath(cfg, file)$ void
    }

    class MazeShuffleConfig {
        <<record>>
        +boolean enabled
        +double intervalSeconds
        +List~String~ mazePaths
        +loadFromFile(path, fallback)$ MazeShuffleConfig
    }

    class MazeFileData {
        <<record>>
        +Maze maze
        +MazeMetadata metadata
        +File sourceFile
        +String resolvedBackgroundPath
    }

    class MazeMetadata {
        <<record>>
        +Map~String,String~ entries
        +fromCommentLines(lines)$ MazeMetadata
        +withEntry(key, value) MazeMetadata
        +get(key, fallback) String
    }

    Maze *-- Tile : grid of
    MazeTextLoader --> Maze : creates
    MazeTextLoader --> MazeFileData : returns
    MazeTextLoader --> MazeMetadata : parses
    MazeTextSerializer --> Maze : serializes
    MazeFileData --> Maze : contains
    MazeFileData --> MazeMetadata : contains
```

---

## 7. MapV2 Subsystem (`core.level.mapv2.*`)

```mermaid
classDiagram
    class MapV2Layouts {
        <<utility>>
        +SOURCE_WIDTH = 1024$
        +SOURCE_HEIGHT = 768$
        +map1Walls()$ List~Rectangle~
        +map2Walls()$ List~Rectangle~
    }

    class MapV2CoordinateMapper {
        -float scaleX, scaleY
        +MapV2CoordinateMapper(widthPx, heightPx)
        +scaleRect(sourceRect) Rectangle
    }

    class MapV2MazeConverter {
        <<utility>>
        +buildMazeFromWalls(walls)$ Maze
        +buildMazeFromWalls(maze, walls)$ Maze
    }

    MapV2MazeConverter --> MapV2CoordinateMapper : uses
    MapV2MazeConverter --> MapV2Layouts : uses
    MapV2MazeConverter --> Maze : creates
```

---

## 8. Player / Collision System (`core.player.*`)

```mermaid
classDiagram
    class CollisionMap {
        <<interface>>
        +getTileSize() int
        +getWidthInTiles() int
        +getHeightInTiles() int
        +isWalkable(tx, ty) boolean
        +canOccupy(x, y, w, h, margin) boolean
        +inspectOccupancy(label, x, y, w, h, margin) CollisionDebugInfo
    }

    class MazeCollisionMapAdapter {
        -Maze maze
        +canOccupy(x, y, w, h, margin) boolean
        +inspectOccupancy(...) CollisionDebugInfo
        +getMaze() Maze
    }

    class MapV2CollisionMap {
        -Rectangle[] walls
        -int tileSize, widthInTiles, heightInTiles
        +isWalkable(tx, ty) boolean
        +canOccupy(x, y, w, h, margin) boolean
    }

    class PlayerController {
        -EnumMap~Direction,Boolean~ pressed
        -EnumMap~Direction,Long~ pressedOrder
        +onKeyPressed(keyCode) void
        +onKeyReleased(keyCode) void
        +getDesiredDirection() Direction
        +isPressed(dir) boolean
    }

    class PlayerModule {
        -Player player
        -CollisionMap collisionMap
        -PlayerController controller
        -PlayerDebugSnapshot debugSnapshot
        +onKeyPressed(keyCode) void
        +onKeyReleased(keyCode) void
        +update(deltaSeconds) void
        +render(g2) void
    }

    class CollisionDebugInfo {
        -String label
        -float x, y, width, height
        -int margin
        -boolean canOccupy, usedNudge
        -String resolution, blockedReason
        -List~SamplePoint~ samplePoints
        -List~TileHit~ tiles
    }

    CollisionMap <|.. MazeCollisionMapAdapter : implements
    CollisionMap <|.. MapV2CollisionMap : implements
    MazeCollisionMapAdapter --> Maze : wraps
    PlayerModule --> Player : controls
    PlayerModule --> CollisionMap : collision queries
    PlayerModule --> PlayerController : reads input
    PlayerModule --> PlayerDebugSnapshot : logs frames
    CollisionMap --> CollisionDebugInfo : produces
```

---

## 9. Config & Data (`core.config.*`, `core.data.*`)

```mermaid
classDiagram
    class ProjectPaths {
        <<utility>>
        -File RESOURCES_ROOT$
        +mazeDirectory()$ File
        +mapDirectory()$ File
        +spriteThiefDirectory()$ File
        +uiDirectory()$ File
        +mazeFilePath(name)$ String
        +uiFilePath(name)$ String
        +resourcesRoot()$ File
    }

    class PlayerConfig {
        <<utility>>
        +BASE_TILE_SIZE = 32$
        +SPRITE_FRAME_COUNT = 6$
        +SPRITE_SOURCE_FRAME_SIZE = 128$
        +CORNER_SLIDE_ATTEMPTS = 4$
        +moveSpeedForTile(ts)$ float
        +hitboxInsetForTile(ts)$ int
        +collisionMarginForTile(ts)$ int
    }

    class AppDatabase {
        <<utility>>
        +initialize()$ void
        +registerUser(user, pass)$ boolean
        +authenticate(user, pass)$ boolean
        +saveScore(user, score)$ void
        +getTopScores(limit)$ List~String[]~
        -hashPassword(pass)$ String
        -getConnection()$ Connection
    }

    class LeaderboardUI {
        +open(parent)$ void
        -paintComponent(g) void
    }

    AppDatabase --> LeaderboardUI : provides data
```

---

## 10. Editor Package (`editor.*`)

```mermaid
classDiagram
    class MazeEditorMain {
        +main(args) void
    }

    class MazeEditorWindow {
        +openOnEdt() void
    }

    class MazeEditorPanel {
        -MazeEditorDocument document
        +paintComponent(g) void
    }

    class MazeEditorDocument {
        -Maze maze
        -MazeMetadata metadata
        -File currentFile
        +save(file) void
        +load(file) void
    }

    class MazeEditorTool {
        <<enum>>
        PAINT
        ERASE
        FILL
        RECTANGLE
        PICKER
    }

    MazeEditorMain --> MazeEditorWindow : opens
    MazeEditorWindow --> MazeEditorPanel : contains
    MazeEditorPanel --> MazeEditorDocument : reads/writes
    MazeEditorPanel --> MazeEditorTool : current tool
    MazeEditorDocument --> Maze : manages
    MazeEditorDocument --> MazeTextLoader : loads
    MazeEditorDocument --> MazeTextSerializer : saves
```

---

## 11. Debug System (`core.debug.*`)

```mermaid
classDiagram
    class DebugSettings {
        -boolean enabled
        -boolean hudVisible
        -boolean gridVisible
        -boolean walkableOverlayVisible
        -boolean playerHitboxVisible
        -boolean collisionSamplesVisible
        -boolean recentChecksVisible
        +toggleEnabled() void
        +isEnabled() boolean
        +isGridVisible() boolean
        ...
    }

    class PlayerDebugSnapshot {
        +beginFrame(...) void
        +addCheck(info) void
        +setMovementPlan(...) void
        +setMovementOutcome(...) void
    }

    GamePanel --> DebugSettings : F-key toggles
    GameDebugRenderer --> DebugSettings : reads flags
    GameDebugRenderer --> PlayerDebugSnapshot : reads frame data
    PlayerModule --> PlayerDebugSnapshot : writes frame data
```

---

## 12. Full Dependency Overview

```mermaid
graph TB
    subgraph "Entry Points"
        Main
    end

    subgraph "Menu Layer"
        MMS["MainMenuScreen"]
        MState["MainMenuState"]
        MButtons["MainMenuButtons"]
        MImages["MainMenuImageLoader"]
        SC["SessionContext"]
    end

    subgraph "Game Layer"
        GL["GameLauncher"]
        GP["GamePanel"]
        GDR["GameDebugRenderer"]
    end

    subgraph "Core: Gameplay"
        GM["GameplayManager"]
        SD["SpawnDirector"]
        ES["EnemySystem"]
        PS["PickupSystem"]
        EST["EnemyState"]
        PST["PickupState"]
    end

    subgraph "Core: Entities"
        Entity
        Player
        Direction
    end

    subgraph "Core: Level"
        Maze
        MTL["MazeTextLoader"]
        MTS["MazeTextSerializer"]
        AMR["ActiveMazeRegistry"]
        MSC["MazeShuffleConfig"]
    end

    subgraph "Core: Player"
        CM["CollisionMap"]
        MCMA["MazeCollisionMapAdapter"]
        M2CM["MapV2CollisionMap"]
        PC["PlayerController"]
        PM["PlayerModule"]
    end

    subgraph "Core: Data"
        DB["AppDatabase"]
        LUI["LeaderboardUI"]
    end

    Main --> MMS
    Main --> GL
    MMS --> GL
    GL --> GP
    GP --> PM
    GP --> GM
    GP --> GDR
    PM --> Player
    PM --> CM
    PM --> PC
    GM --> SD
    GM --> ES
    GM --> PS
    ES --> EST
    PS --> PST
    MCMA -.->|implements| CM
    M2CM -.->|implements| CM
    Player -->|extends| Entity
    MButtons --> DB
    MButtons --> LUI
    GP --> LUI
    GP --> DB
```
