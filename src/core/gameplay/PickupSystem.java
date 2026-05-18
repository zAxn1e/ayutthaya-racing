package core.gameplay;

import core.player.CollisionMap;
import core.level.Maze;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages spawning and rendering of all pickup objects (points + kill items).
 */
public class PickupSystem {
    private CollisionMap collisionMap;
    private Maze maze;
    private final List<PickupState> pickups = new ArrayList<>();
    private final java.util.Map<PickupType, Image> sprites = new java.util.EnumMap<>(PickupType.class);
    private boolean spawnZoneConfigured;

    public PickupSystem(CollisionMap collisionMap, Maze maze) {
        this.collisionMap = collisionMap;
        this.maze = maze;
        this.spawnZoneConfigured = maze.hasAnySpawnZone();
        loadSprites();
    }

    private void loadSprites() {
        for (PickupType type : PickupType.values()) {
            Image img = loadImage(type.assetKey);
            if (img != null) {
                sprites.put(type, img);
            }
        }
    }

    private Image loadImage(String assetKey) {
        // Try .png then .webp from classpath
        for (String ext : new String[]{".png", ".webp"}) {
            String classpath = "/objects/" + assetKey + ext;
            java.net.URL url = PickupSystem.class.getResource(classpath);
            if (url != null) {
                javax.swing.ImageIcon icon = new javax.swing.ImageIcon(url);
                if (icon.getIconWidth() > 0) return icon.getImage();
            }
        }
        // Try filesystem
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

    /**
     * Try to spawn point pickups up to the cap.
     */
    public void trySpawnPoints(Rectangle playerBounds) {
        long pointCount = pickups.stream().filter(p -> p.isActive() && p.getType() == PickupType.POINT).count();
        while (pointCount < GameplayConfig.MAX_POINT_PICKUPS) {
            PickupState p = spawnPickup(PickupType.POINT, playerBounds);
            if (p != null) {
                pickups.add(p);
                pointCount++;
            } else {
                break;
            }
        }
    }

    /**
     * Try to spawn a kill pickup for a random unlocked enemy type (respects cap).
     */
    public void trySpawnKill(Rectangle playerBounds, SpawnDirector director) {
        long killCount = pickups.stream().filter(p -> p.isActive() && p.getType().isKillPickup()).count();
        if (killCount >= GameplayConfig.MAX_KILL_PICKUPS) return;

        // Build list of eligible kill types
        List<PickupType> eligible = new ArrayList<>();
        for (PickupType pt : PickupType.values()) {
            if (pt.isKillPickup() && director.isTypeUnlocked(pt.targetEnemy)) {
                eligible.add(pt);
            }
        }
        if (eligible.isEmpty()) return;

        PickupType chosen = eligible.get(ThreadLocalRandom.current().nextInt(eligible.size()));
        PickupState p = spawnPickup(chosen, playerBounds);
        if (p != null) {
            pickups.add(p);
        }
    }

    private PickupState spawnPickup(PickupType type, Rectangle playerBounds) {
        List<int[]> candidates = collectSpawnCandidates(spawnZoneConfigured);
        if (candidates.isEmpty()) {
            return null;
        }
        int tileSize = collisionMap.getTileSize();

        int attempts = Math.min(GameplayConfig.PICKUP_SPAWN_ATTEMPTS, Math.max(1, candidates.size() * 2));
        for (int i = 0; i < attempts; i++) {
            int[] tile = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
            int tx = tile[0];
            int ty = tile[1];

            PickupState candidate = new PickupState(type, tx, ty, tileSize);
            if (candidate.getBounds().intersects(playerBounds)) continue;

            // Avoid overlapping existing pickups
            boolean overlaps = false;
            for (PickupState existing : pickups) {
                if (existing.isActive() && existing.getBounds().intersects(candidate.getBounds())) {
                    overlaps = true;
                    break;
                }
            }
            if (overlaps) continue;

            return candidate;
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
                result.add(new int[]{x, y});
            }
        }
        return result;
    }

    /**
     * Collect pickups that intersect the player bounds.
     * Returns list of collected pickup types (caller handles scoring/effects).
     */
    public List<PickupState> collectIntersecting(Rectangle playerBounds) {
        List<PickupState> collected = new ArrayList<>();
        for (PickupState p : pickups) {
            if (p.isActive() && p.getBounds().intersects(playerBounds)) {
                p.deactivate();
                collected.add(p);
            }
        }
        return collected;
    }

    /** Remove inactive pickups from the list. */
    public void prune() {
        pickups.removeIf(p -> !p.isActive());
    }

    public void render(Graphics2D g2) {
        int tileSize = collisionMap.getTileSize();
        for (PickupState p : pickups) {
            if (!p.isActive()) continue;
            Rectangle b = p.getBounds();
            Image sprite = sprites.get(p.getType());
            int renderSize = Math.max(b.width, Math.round(tileSize * 1.15f));
            int drawX = b.x + b.width / 2 - renderSize / 2;
            int drawY = b.y + b.height / 2 - renderSize / 2;

            g2.setColor(new java.awt.Color(0, 0, 0, 90));
            g2.fillOval(drawX + 2, drawY + renderSize - 6, renderSize - 4, 6);

            if (sprite != null) {
                g2.drawImage(sprite, drawX, drawY, renderSize, renderSize, null);
                g2.setColor(new java.awt.Color(255, 255, 255, 180));
                g2.drawOval(drawX, drawY, renderSize - 1, renderSize - 1);
            } else {
                // Fallback color by type
                if (p.getType() == PickupType.POINT) {
                    g2.setColor(new java.awt.Color(246, 205, 82));
                } else {
                    g2.setColor(new java.awt.Color(220, 60, 60));
                }
                g2.fillOval(drawX, drawY, renderSize, renderSize);
                g2.setColor(new java.awt.Color(255, 255, 255, 200));
                g2.drawOval(drawX, drawY, renderSize - 1, renderSize - 1);
            }
        }
    }

    public List<PickupState> getPickups() { return pickups; }

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

        for (PickupState pickup : pickups) {
            if (!pickup.isActive()) {
                preserved++;
                continue;
            }
            if (isPickupValid(pickup, playerBounds, pickup)) {
                preserved++;
                continue;
            }

            int[] relocationTile = findRelocationTile(pickup.getTileX(), pickup.getTileY(), pickup, playerBounds);
            if (relocationTile == null) {
                pickup.deactivate();
                removed++;
                continue;
            }
            pickup.relocateToTile(relocationTile[0], relocationTile[1], tileSize);
            relocated++;
        }
        prune();
        return new ReconcileReport(preserved, relocated, removed);
    }

    private boolean isPickupValid(PickupState pickup, Rectangle playerBounds, PickupState self) {
        if (!pickup.isActive()) {
            return false;
        }
        int tx = pickup.getTileX();
        int ty = pickup.getTileY();
        if (!collisionMap.isWalkable(tx, ty)) {
            return false;
        }
        if (spawnZoneConfigured && !maze.isSpawnZone(tx, ty)) {
            return false;
        }
        if (playerBounds != null && pickup.getBounds().intersects(playerBounds)) {
            return false;
        }
        return !intersectsOtherPickup(pickup.getBounds(), self);
    }

    private int[] findRelocationTile(int targetX, int targetY, PickupState movingPickup, Rectangle playerBounds) {
        int maxRadius = Math.max(collisionMap.getWidthInTiles(), collisionMap.getHeightInTiles());
        int tileSize = collisionMap.getTileSize();
        int size = Math.max(10, tileSize - 2);

        for (int radius = 0; radius <= maxRadius; radius++) {
            for (int y = Math.max(1, targetY - radius); y <= Math.min(collisionMap.getHeightInTiles() - 2, targetY + radius); y++) {
                for (int x = Math.max(1, targetX - radius); x <= Math.min(collisionMap.getWidthInTiles() - 2, targetX + radius); x++) {
                    if (!collisionMap.isWalkable(x, y)) {
                        continue;
                    }
                    if (spawnZoneConfigured && !maze.isSpawnZone(x, y)) {
                        continue;
                    }
                    int px = x * tileSize + (tileSize - size) / 2;
                    int py = y * tileSize + (tileSize - size) / 2;
                    Rectangle candidate = new Rectangle(px, py, size, size);
                    if (playerBounds != null && candidate.intersects(playerBounds)) {
                        continue;
                    }
                    if (intersectsOtherPickup(candidate, movingPickup)) {
                        continue;
                    }
                    return new int[]{x, y};
                }
            }
        }
        return null;
    }

    private boolean intersectsOtherPickup(Rectangle candidate, PickupState exclude) {
        for (PickupState pickup : pickups) {
            if (pickup == exclude || !pickup.isActive()) {
                continue;
            }
            if (pickup.getBounds().intersects(candidate)) {
                return true;
            }
        }
        return false;
    }

    public record ReconcileReport(int preserved, int relocated, int removed) {
    }
}
