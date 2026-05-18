package core.gameplay;

import core.entities.Direction;
import core.level.Maze;
import core.player.CollisionMap;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages police enemy lifecycle: spawning, maze-aware movement with
 * intersection turning, turn-slowdown, death/respawn, and rendering.
 */
public class EnemySystem {
    private CollisionMap collisionMap;
    private Maze maze;
    private final List<EnemyState> enemies = new ArrayList<>();
    private final EnumMap<EnemyType, Image> sprites = new EnumMap<>(EnemyType.class);
    private final float playerBaseSpeed;
    private boolean spawnZoneConfigured;

    public EnemySystem(CollisionMap collisionMap, Maze maze, float playerBaseSpeed) {
        this.collisionMap = collisionMap;
        this.maze = maze;
        this.playerBaseSpeed = playerBaseSpeed;
        this.spawnZoneConfigured = maze.hasAnySpawnZone();
        loadSprites();
    }

    private void loadSprites() {
        for (EnemyType type : EnemyType.values()) {
            Image img = loadImage(type.assetKey);
            if (img != null) {
                sprites.put(type, img);
            }
        }
    }

    private Image loadImage(String assetKey) {
        for (String ext : new String[]{".png", ".webp"}) {
            String classpath = "/objects/" + assetKey + ext;
            java.net.URL url = EnemySystem.class.getResource(classpath);
            if (url != null) {
                javax.swing.ImageIcon icon = new javax.swing.ImageIcon(url);
                if (icon.getIconWidth() > 0) return icon.getImage();
            }
        }
        java.io.File resRoot = core.config.ProjectPaths.resourcesRoot();
        for (String ext : new String[]{".png", ".webp"}) {
            java.io.File file = new java.io.File(resRoot, "objects/" + assetKey + ext);
            if (file.exists()) {
                javax.swing.ImageIcon icon = new javax.swing.ImageIcon(file.getPath());
                if (icon.getIconWidth() > 0) return icon.getImage();
            }
        }
        return null;
    }

    // ── Spawning ──

    /**
     * Try to spawn enemies for all unlocked types up to per-type cap.
     */
    public void trySpawnEnemies(SpawnDirector director, int playerGridX, int playerGridY) {
        for (EnemyType type : EnemyType.values()) {
            if (!director.isTypeUnlocked(type)) continue;
            int typeCap = GameplayConfig.maxEnemiesForType(type);
            long activeSlots = enemies.stream()
                    .filter(e -> e.getType() == type)
                    .count();
            if (activeSlots >= typeCap) continue;

            int[] tile = findSpawnTile(playerGridX, playerGridY);
            if (tile != null) {
                EnemyState enemy = new EnemyState(type, tile[0], tile[1],
                        collisionMap.getTileSize(), playerBaseSpeed);
                enemies.add(enemy);
            }
        }
    }

    private int[] findSpawnTile(int playerGridX, int playerGridY) {
        List<int[]> candidates = collectSpawnCandidates(spawnZoneConfigured);
        if (candidates.isEmpty()) {
            return null;
        }

        int attempts = Math.min(GameplayConfig.ENEMY_SPAWN_ATTEMPTS, Math.max(1, candidates.size() * 2));
        for (int i = 0; i < attempts; i++) {
            int[] tile = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
            int dist = Math.abs(tile[0] - playerGridX) + Math.abs(tile[1] - playerGridY);
            if (dist >= GameplayConfig.ENEMY_MIN_SPAWN_DISTANCE_TILES) {
                return tile;
            }
        }
        return null;
    }

    private List<int[]> collectSpawnCandidates(boolean useSpawnZoneLayer) {
        int width = collisionMap.getWidthInTiles();
        int height = collisionMap.getHeightInTiles();
        List<int[]> result = new ArrayList<>();
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if (!collisionMap.isWalkable(x, y)) continue;
                if (useSpawnZoneLayer && !maze.isSpawnZone(x, y)) continue;
                if (!hasWalkableNeighbor(x, y)) continue;
                result.add(new int[]{x, y});
            }
        }
        return result;
    }

    private boolean hasWalkableNeighbor(int tx, int ty) {
        return isEnemyTileAllowed(tx + 1, ty)
                || isEnemyTileAllowed(tx - 1, ty)
                || isEnemyTileAllowed(tx, ty + 1)
                || isEnemyTileAllowed(tx, ty - 1);
    }

    // ── Update ──

    /**
     * Update all enemies: move alive ones, tick respawn on dead ones.
     */
    public void update(double delta, int playerGridX, int playerGridY) {
        for (EnemyState enemy : enemies) {
            if (enemy.isAlive()) {
                updateMovement(enemy, delta, playerGridX, playerGridY);
            } else {
                if (enemy.tickRespawn(delta)) {
                    int[] tile = findSpawnTile(playerGridX, playerGridY);
                    if (tile != null) {
                        enemy.respawn(tile[0], tile[1]);
                    } else {
                        enemy.scheduleRespawnRetry(GameplayConfig.ENEMY_RESPAWN_RETRY_DELAY);
                    }
                }
            }
        }
    }

    private void updateMovement(EnemyState enemy, double delta, int playerGridX, int playerGridY) {
        int tileSize = collisionMap.getTileSize();

        // Handle stopped enemies — pick a direction from current position
        if (enemy.getDirection() == Direction.NONE) {
            int gridX = enemy.getGridX();
            int gridY = enemy.getGridY();
            Direction newDir = chooseDirection(enemy, gridX, gridY, playerGridX, playerGridY);
            enemy.changeDirection(newDir);
            return;
        }

        // ── 1. Record pre-move center position ──
        float preCenterX = enemy.getCenterX();
        float preCenterY = enemy.getCenterY();
        float oldX = enemy.getX();
        float oldY = enemy.getY();

        // ── 2. Advance position ──
        enemy.advance(delta);

        // ── 3. Validate against collision map ──
        if (!canEnemyOccupy(enemy)) {
            enemy.setPosition(oldX, oldY);
            enemy.snapToGrid();
            enemy.changeDirection(Direction.NONE);
            return;
        }

        // Clamp within map bounds
        float maxX = collisionMap.getWidthInTiles() * tileSize - enemy.getWidth() - 1;
        float maxY = collisionMap.getHeightInTiles() * tileSize - enemy.getHeight() - 1;
        enemy.setPosition(
                Math.max(0, Math.min(maxX, enemy.getX())),
                Math.max(0, Math.min(maxY, enemy.getY()))
        );

        // ── 4. Check if we crossed a tile center this frame ──
        float postCenterX = enemy.getCenterX();
        float postCenterY = enemy.getCenterY();
        int gridX = enemy.getGridX();
        int gridY = enemy.getGridY();
        float tileCenterX = gridX * tileSize + tileSize / 2f;
        float tileCenterY = gridY * tileSize + tileSize / 2f;

        boolean crossedCenter = false;
        Direction dir = enemy.getDirection();
        if (dir.dx > 0) {
            crossedCenter = preCenterX < tileCenterX && postCenterX >= tileCenterX;
        } else if (dir.dx < 0) {
            crossedCenter = preCenterX > tileCenterX && postCenterX <= tileCenterX;
        } else if (dir.dy > 0) {
            crossedCenter = preCenterY < tileCenterY && postCenterY >= tileCenterY;
        } else if (dir.dy < 0) {
            crossedCenter = preCenterY > tileCenterY && postCenterY <= tileCenterY;
        }

        if (!crossedCenter) {
            return; // Still moving toward or away from the center — nothing to decide
        }

        // ── 5. We just crossed a tile center — evaluate direction ──
        Direction currentDir = enemy.getDirection();
        boolean canContinue = isEnemyTileAllowed(gridX + currentDir.dx, gridY + currentDir.dy);
        int walkableNeighbors = countWalkableDirections(gridX, gridY, currentDir);
        boolean isIntersection = walkableNeighbors > 1;

        if (!canContinue || isIntersection) {
            Direction newDir = chooseDirection(enemy, gridX, gridY, playerGridX, playerGridY);

            // Snap to tile center — since we JUST crossed it, the correction
            // is at most speed×delta (< 1px at 120Hz), so it's invisible
            enemy.snapToGrid();
            enemy.changeDirection(newDir);
        }
        // else: straight corridor, can continue — no snap, no change
    }

    /**
     * Count how many directions (excluding opposite of current) lead to walkable tiles.
     */
    private int countWalkableDirections(int gridX, int gridY, Direction currentDir) {
        int count = 0;
        Direction opposite = currentDir.opposite();
        for (Direction d : new Direction[]{Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT}) {
            if (d == opposite) continue;
            if (isEnemyTileAllowed(gridX + d.dx, gridY + d.dy)) {
                count++;
            }
        }
        return count;
    }

    private boolean canEnemyOccupy(EnemyState enemy) {
        int tileSize = collisionMap.getTileSize();
        int margin = 1;
        float left = enemy.getX() + margin;
        float top = enemy.getY() + margin;
        float right = enemy.getX() + enemy.getWidth() - margin;
        float bottom = enemy.getY() + enemy.getHeight() - margin;

        int tlx = (int) (left / tileSize);
        int tly = (int) (top / tileSize);
        int trx = (int) (right / tileSize);
        int try_ = (int) (top / tileSize);
        int blx = (int) (left / tileSize);
        int bly = (int) (bottom / tileSize);
        int brx = (int) (right / tileSize);
        int bry = (int) (bottom / tileSize);

        return isEnemyTileAllowed(tlx, tly)
                && isEnemyTileAllowed(trx, try_)
                && isEnemyTileAllowed(blx, bly)
                && isEnemyTileAllowed(brx, bry);
    }

    /**
     * Choose a movement direction at an intersection tile.
     * Uses a strong bias toward the player (chase) with randomized fallback.
     */
    private Direction chooseDirection(EnemyState enemy, int gridX, int gridY,
                                      int playerGridX, int playerGridY) {
        List<Direction> walkable = new ArrayList<>();
        Direction opposite = enemy.getDirection().opposite();

        for (Direction d : new Direction[]{Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT}) {
            if (d == opposite) continue;
            int nx = gridX + d.dx;
            int ny = gridY + d.dy;
            if (isEnemyTileAllowed(nx, ny)) {
                walkable.add(d);
            }
        }

        // If no forward options, allow reversal
        if (walkable.isEmpty()) {
            if (opposite != Direction.NONE && isEnemyTileAllowed(gridX + opposite.dx, gridY + opposite.dy)) {
                return opposite;
            }
            return Direction.NONE;
        }

        // If only one forward option, just go there (no randomness needed)
        if (walkable.size() == 1) {
            return walkable.get(0);
        }

        // At actual intersections, bias toward the player
        if (ThreadLocalRandom.current().nextDouble() < GameplayConfig.ENEMY_CHASE_BIAS) {
            Direction best = null;
            int bestDist = Integer.MAX_VALUE;
            for (Direction d : walkable) {
                int nx = gridX + d.dx;
                int ny = gridY + d.dy;
                int dist = Math.abs(nx - playerGridX) + Math.abs(ny - playerGridY);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = d;
                }
            }
            if (best != null) return best;
        }

        return walkable.get(ThreadLocalRandom.current().nextInt(walkable.size()));
    }

    // ── Kill effects ──

    /** Kill all alive enemies of the given type. */
    public void killAllOfType(EnemyType type) {
        for (EnemyState e : enemies) {
            if (e.getType() == type && e.isAlive()) {
                e.kill();
            }
        }
    }

    /** Get all alive enemies whose bounds intersect the given rectangle. */
    public List<EnemyState> getCollidingEnemies(Rectangle playerBounds) {
        List<EnemyState> result = new ArrayList<>();
        for (EnemyState e : enemies) {
            if (e.isAlive() && e.getBounds().intersects(playerBounds)) {
                result.add(e);
            }
        }
        return result;
    }

    // ── Rendering ──

    public void render(Graphics2D g2) {
        int tileSize = collisionMap.getTileSize();
        for (EnemyState e : enemies) {
            if (!e.isAlive()) continue;
            Rectangle b = e.getBounds();
            Image sprite = sprites.get(e.getType());
            int renderSize = Math.round(tileSize * 1.4f);
            int drawX = b.x + b.width / 2 - renderSize / 2;
            int drawY = b.y + b.height - renderSize;
            if (sprite != null) {
                g2.drawImage(sprite, drawX, drawY, renderSize, renderSize, null);
            } else {
                // Fallback: colored circle per type
                java.awt.Color c = switch (e.getType()) {
                    case FAT -> new java.awt.Color(100, 100, 220);
                    case CAR -> new java.awt.Color(220, 140, 40);
                    case CHICKEN -> new java.awt.Color(220, 60, 180);
                };
                g2.setColor(c);
                g2.fillOval(b.x, b.y, b.width, b.height);
            }
        }
    }

    public List<EnemyState> getEnemies() { return enemies; }

    public int getAliveCount() {
        return (int) enemies.stream().filter(EnemyState::isAlive).count();
    }

    public boolean isSpawnZoneConfigured() {
        return spawnZoneConfigured;
    }

    public void rebindMap(CollisionMap collisionMap, Maze maze) {
        this.collisionMap = collisionMap;
        this.maze = maze;
        this.spawnZoneConfigured = maze.hasAnySpawnZone();
    }

    public ReconcileReport reconcileAfterMazeSwitch(Rectangle playerBounds) {
        int preserved = 0;
        int relocated = 0;
        int removed = 0;
        int tileSize = collisionMap.getTileSize();

        for (var it = enemies.iterator(); it.hasNext(); ) {
            EnemyState enemy = it.next();
            if (!enemy.isAlive()) {
                preserved++;
                continue;
            }
            if (canEnemyOccupy(enemy)) {
                preserved++;
                continue;
            }

            int[] relocationTile = findRelocationTile(enemy.getGridX(), enemy.getGridY(), enemy, playerBounds);
            if (relocationTile == null) {
                it.remove();
                removed++;
                continue;
            }

            float x = relocationTile[0] * tileSize + (tileSize - enemy.getWidth()) / 2f;
            float y = relocationTile[1] * tileSize + (tileSize - enemy.getHeight()) / 2f;
            enemy.setPosition(x, y);
            enemy.snapToGrid();
            enemy.changeDirection(Direction.NONE);
            relocated++;
        }
        return new ReconcileReport(preserved, relocated, removed);
    }

    private int[] findRelocationTile(int targetX, int targetY, EnemyState movingEnemy, Rectangle playerBounds) {
        int maxRadius = Math.max(collisionMap.getWidthInTiles(), collisionMap.getHeightInTiles());
        int tileSize = collisionMap.getTileSize();

        for (int radius = 0; radius <= maxRadius; radius++) {
            for (int y = Math.max(1, targetY - radius); y <= Math.min(collisionMap.getHeightInTiles() - 2, targetY + radius); y++) {
                for (int x = Math.max(1, targetX - radius); x <= Math.min(collisionMap.getWidthInTiles() - 2, targetX + radius); x++) {
                    if (!isEnemyTileAllowed(x, y) || !hasWalkableNeighbor(x, y)) {
                        continue;
                    }

                    float px = x * tileSize + (tileSize - movingEnemy.getWidth()) / 2f;
                    float py = y * tileSize + (tileSize - movingEnemy.getHeight()) / 2f;
                    Rectangle bounds = new Rectangle((int) px, (int) py, (int) movingEnemy.getWidth(), (int) movingEnemy.getHeight());
                    if (playerBounds != null && bounds.intersects(playerBounds)) {
                        continue;
                    }
                    if (intersectsOtherEnemy(bounds, movingEnemy)) {
                        continue;
                    }
                    if (canEnemyOccupyAt(px, py, movingEnemy.getWidth(), movingEnemy.getHeight())) {
                        return new int[]{x, y};
                    }
                }
            }
        }
        return null;
    }

    private boolean intersectsOtherEnemy(Rectangle candidate, EnemyState exclude) {
        for (EnemyState enemy : enemies) {
            if (enemy == exclude || !enemy.isAlive()) {
                continue;
            }
            if (enemy.getBounds().intersects(candidate)) {
                return true;
            }
        }
        return false;
    }

    private boolean isEnemyTileAllowed(int tileX, int tileY) {
        if (!collisionMap.isWalkable(tileX, tileY)) {
            return false;
        }
        if (!spawnZoneConfigured) {
            return true;
        }
        return maze.isSpawnZone(tileX, tileY);
    }

    private boolean canEnemyOccupyAt(float x, float y, float width, float height) {
        int tileSize = collisionMap.getTileSize();
        int margin = 1;
        float left = x + margin;
        float top = y + margin;
        float right = x + width - margin;
        float bottom = y + height - margin;

        int tlx = (int) (left / tileSize);
        int tly = (int) (top / tileSize);
        int trx = (int) (right / tileSize);
        int try_ = (int) (top / tileSize);
        int blx = (int) (left / tileSize);
        int bly = (int) (bottom / tileSize);
        int brx = (int) (right / tileSize);
        int bry = (int) (bottom / tileSize);

        return isEnemyTileAllowed(tlx, tly)
                && isEnemyTileAllowed(trx, try_)
                && isEnemyTileAllowed(blx, bly)
                && isEnemyTileAllowed(brx, bry);
    }

    public record ReconcileReport(int preserved, int relocated, int removed) {
    }
}
