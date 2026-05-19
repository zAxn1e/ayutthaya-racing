package core.gameplay;

import core.entities.Player;
import core.level.Maze;
import core.player.CollisionMap;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

/**
 * Top-level orchestrator that ties together the spawn director,
 * pickup system, enemy system, and collision interactions.
 * <p>
 * GamePanel calls {@link #update(double)} each fixed-timestep tick
 * and {@link #render(Graphics2D)} each paint frame.
 */
public class GameplayManager {
    private final SpawnDirector director;
    private final PickupSystem pickupSystem;
    private final EnemySystem enemySystem;
    private final Player player;
    private int score;
    private double invincibilityTimer;
    private SwitchReport lastSwitchReport;
    private boolean gameOver;
    private Integer finalScoreSnapshot;

    public GameplayManager(Player player, CollisionMap collisionMap, Maze maze) {
        this.player = player;
        this.director = new SpawnDirector();
        this.pickupSystem = new PickupSystem(collisionMap, maze);
        this.enemySystem = new EnemySystem(collisionMap, maze, player.getSpeed());
        this.score = 0;
        this.invincibilityTimer = 0;
        this.lastSwitchReport = SwitchReport.empty();
        this.gameOver = false;
        this.finalScoreSnapshot = null;
    }

    /**
     * Main gameplay tick (called from the fixed-update loop, paused time excluded).
     */
    public void update(double delta) {
        if (gameOver) {
            return;
        }
        // 1. Advance timers
        director.update(delta);
        if (invincibilityTimer > 0) {
            invincibilityTimer -= delta;
        }

        Rectangle playerBounds = player.getBounds();
        int playerGridX = player.getGridX();
        int playerGridY = player.getGridY();

        // 2. Spawn pickups
        if (director.shouldSpawnPoint()) {
            pickupSystem.trySpawnPoints(playerBounds);
        }
        if (director.shouldSpawnKill()) {
            pickupSystem.trySpawnKill(playerBounds, director);
        }

        // 3. Spawn enemies
        if (director.shouldSpawnEnemy()) {
            enemySystem.trySpawnEnemies(director, playerGridX, playerGridY);
        }

        // 4. Update enemy movement
        enemySystem.update(delta, playerGridX, playerGridY);

        // 5. Collect pickups
        List<PickupState> collected = pickupSystem.collectIntersecting(playerBounds);
        for (PickupState p : collected) {
            if (p.getType() == PickupType.POINT) {
                score += GameplayConfig.POINT_SCORE_VALUE;
            } else if (p.getType().isKillPickup()) {
                enemySystem.killAllOfType(p.getType().targetEnemy);
                // Bonus score for using a kill item
                score += GameplayConfig.POINT_SCORE_VALUE * 2;
            }
        }
        pickupSystem.prune();

        // 6. Enemy-player collisions
        if (invincibilityTimer <= 0) {
            List<EnemyState> hitting = enemySystem.getCollidingEnemies(playerBounds);
            if (!hitting.isEmpty()) {
                finalScoreSnapshot = score;
                score = Math.max(0, score - GameplayConfig.ENEMY_TOUCH_PENALTY);
                gameOver = true;
            }
        }
    }

    /**
     * Render all gameplay entities (pickups + enemies).
     * Called within the world-transform Graphics2D context.
     */
    public void render(Graphics2D g2) {
        pickupSystem.render(g2);
        enemySystem.render(g2);
    }

    // ── Accessors for HUD ──

    public int getScore()              { return score; }
    public int getFinalScore()         { return finalScoreSnapshot != null ? finalScoreSnapshot : score; }
    public int getCurrentStage()       { return director.getCurrentStage(); }
    public double getElapsedTime()     { return director.getElapsedTime(); }
    public int getAliveEnemyCount()    { return enemySystem.getAliveCount(); }
    public boolean isInvincible()      { return invincibilityTimer > 0; }
    public double getInvincibilityTimer() { return invincibilityTimer; }
    public boolean isGameOver() { return gameOver; }
    public boolean isSpawnZoneConfigured() {
        return enemySystem.isSpawnZoneConfigured() || pickupSystem.isSpawnZoneConfigured();
    }

    public SwitchReport onMazeSwitched(CollisionMap collisionMap, Maze maze) {
        enemySystem.rebindMap(collisionMap, maze);
        pickupSystem.rebindMap(collisionMap, maze);
        Rectangle playerBounds = player.getBounds();
        EnemySystem.ReconcileReport enemyReport = enemySystem.reconcileAfterMazeSwitch(playerBounds);
        PickupSystem.ReconcileReport pickupReport = pickupSystem.reconcileAfterMazeSwitch(playerBounds);
        lastSwitchReport = new SwitchReport(
                enemyReport.preserved(),
                enemyReport.relocated(),
                enemyReport.removed(),
                pickupReport.preserved(),
                pickupReport.relocated(),
                pickupReport.removed()
        );
        return lastSwitchReport;
    }

    public SwitchReport getLastSwitchReport() {
        return lastSwitchReport;
    }

    public record SwitchReport(
            int enemyPreserved,
            int enemyRelocated,
            int enemyRemoved,
            int pickupPreserved,
            int pickupRelocated,
            int pickupRemoved
    ) {
        public static SwitchReport empty() {
            return new SwitchReport(0, 0, 0, 0, 0, 0);
        }
    }
}
