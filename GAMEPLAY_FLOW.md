# Gameplay Flow — merged_final

## Runtime Architecture

```
GamePanel (Swing JPanel + game loop thread)
 ├── PlayerModule          – input → direction → movement + collision
 ├── GameplayManager       – orchestrates all gameplay subsystems
 │    ├── SpawnDirector    – elapsed time, stage progression, spawn timers
 │    ├── PickupSystem     – spawns/renders/collects point & kill pickups
 │    └── EnemySystem      – spawns/renders/moves police enemies
 └── GameDebugRenderer     – F-key debug overlays
```

---

## Game Loop (fixed-timestep)

```
┌────────────────────────────────────┐
│  1. Accumulate wall-clock delta    │
│  2. While accumulator ≥ 1/120s:   │
│     ├─ PlayerModule.update()       │
│     └─ GameplayManager.update()    │
│        ├─ SpawnDirector.update()   │
│        ├─ Spawn pickups (point/kill)│
│        ├─ Spawn enemies            │
│        ├─ EnemySystem.update()     │  ← movement + AI
│        ├─ Collect pickups          │  ← score / kill effects
│        └─ Enemy-player collisions  │  ← penalty + invincibility
│  3. repaint() → paintComponent()   │
│     ├─ maze.render()               │
│     ├─ gameplayManager.render()    │  ← pickups + enemies
│     ├─ playerModule.render()       │
│     ├─ debug overlays              │
│     └─ HUD (score/stage/time)      │
└────────────────────────────────────┘
```

Paused time is excluded: when `pauseMenuOpen == true`, neither `PlayerModule` nor `GameplayManager` receives `update()` calls, so elapsed game-time freezes.

---

## Stage Timeline & Unlock Rules

| Stage | Elapsed Time | Unlocked Enemy Types       |
|-------|-------------|---------------------------|
| 1     | 0 s         | `police_fat` only         |
| 2     | 30 s        | + `police_car`            |
| 3     | 70 s        | + `police_chicken`        |

Thresholds are configured in `GameplayConfig.STAGE_*_TIME` and can be tuned without code changes.

---

## Enemy Types

| Type      | Asset               | Speed (× player) | Unlock Stage |
|-----------|---------------------|-------------------|--------------|
| FAT       | `police_fat.png`    | 0.45×             | 1            |
| CAR       | `police_car.png`    | 0.70×             | 2            |
| CHICKEN   | `police_chicken.png`| 1.00×             | 3            |

### Movement AI
- Enemies roam the maze on walkable tiles.
- At intersections they choose a direction:
  - **35% chance** (configurable `ENEMY_CHASE_BIAS`) they pick the direction that minimizes Manhattan distance to the player.
  - Otherwise, a random valid direction (excluding immediate reversal unless it's the only option).
- **Turn slowdown**: on direction change, speed drops to `baseSpeed × 0.35` and smoothly recovers to `baseSpeed` over 0.45 seconds (linear interpolation).

### Spawn & Respawn
- Up to **2 enemies per type** alive simultaneously (`MAX_ENEMIES_PER_TYPE`).
- Spawn check every **4 s** (`ENEMY_SPAWN_INTERVAL`).
- Enemies spawn on walkable tiles at least **8 tiles** from the player.
- After being killed (via kill pickup), an enemy respawns after **8 s** cooldown (`ENEMY_RESPAWN_DELAY`).

---

## Pickup Types

| Pickup        | Asset                    | Effect                              |
|---------------|--------------------------|-------------------------------------|
| POINT         | `object_point.png/webp`  | +10 score                           |
| KILL_FAT      | `kill_fat_00000.png`     | Kill all alive FAT police, +20 score|
| KILL_CAR      | `kill_car_00000.png`     | Kill all alive CAR police, +20 score|
| KILL_CHICKEN  | `kill_chicken_00000.png` | Kill all alive CHICKEN police, +20  |

### Spawn Rules
- **Point pickups**: up to 5 on the map, checked every 2 s.
- **Kill pickups**: up to 2 on the map (across all types), cooldown of 12 s between spawns. Only types whose enemy is already unlocked can appear.
- All pickups spawn on walkable tiles, avoiding overlap with the player and existing pickups.

---

## Interaction Matrix

| Entity A | Entity B     | Result                                             |
|----------|-------------|-----------------------------------------------------|
| Player   | POINT       | Score += 10, pickup removed                         |
| Player   | KILL_X      | All alive type-X police killed, score += 20         |
| Player   | Enemy       | Score -= 30 (min 0), 2 s invincibility              |
| Enemy    | Enemy       | No interaction                                      |
| Enemy    | Pickup      | No interaction (enemies ignore pickups)             |

---

## Key Tuning Knobs (GameplayConfig)

| Constant                        | Default | Description                                    |
|---------------------------------|---------|------------------------------------------------|
| `STAGE_2_TIME`                  | 30.0 s  | When CAR police unlock                         |
| `STAGE_3_TIME`                  | 70.0 s  | When CHICKEN police unlock                     |
| `MAX_ENEMIES_PER_TYPE`          | 2       | Cap per enemy type                             |
| `ENEMY_SPAWN_INTERVAL`          | 4.0 s   | Delay between spawn checks                    |
| `ENEMY_RESPAWN_DELAY`           | 8.0 s   | Cooldown after kill before respawn             |
| `ENEMY_MIN_SPAWN_DISTANCE_TILES`| 8      | Min tile distance from player for spawn        |
| `TURN_SLOWDOWN_FACTOR`          | 0.35    | Speed multiplier during turn recovery          |
| `TURN_RECOVERY_SECONDS`         | 0.45 s  | Time to recover full speed after turn          |
| `ENEMY_CHASE_BIAS`              | 0.35    | Probability of chasing player at intersection  |
| `MAX_POINT_PICKUPS`             | 5       | Simultaneous point pickups on map              |
| `POINT_SPAWN_INTERVAL`          | 2.0 s   | Interval between point spawn attempts          |
| `POINT_SCORE_VALUE`             | 10      | Score per point pickup                         |
| `MAX_KILL_PICKUPS`              | 2       | Simultaneous kill pickups on map               |
| `KILL_SPAWN_COOLDOWN`           | 12.0 s  | Cooldown between kill pickup spawns            |
| `ENEMY_TOUCH_PENALTY`           | 30      | Score lost when touched by enemy               |
| `PLAYER_INVINCIBILITY_SECONDS`  | 2.0 s   | Invincibility window after being hit           |

---

## File Map

```
merged_final/src/
├── core/gameplay/
│   ├── GameplayConfig.java     ← all tuning constants
│   ├── EnemyType.java          ← FAT / CAR / CHICKEN enum
│   ├── PickupType.java         ← POINT / KILL_* enum
│   ├── EnemyState.java         ← per-enemy runtime state
│   ├── PickupState.java        ← per-pickup runtime state
│   ├── SpawnDirector.java      ← time tracking + stage + spawn timers
│   ├── EnemySystem.java        ← enemy lifecycle + movement AI
│   ├── PickupSystem.java       ← pickup spawn / collect / render
│   └── GameplayManager.java    ← orchestrator (single update/render entry)
├── panels/
│   └── GamePanel.java          ← game loop integration point
└── ...existing packages unchanged...
```
