package core.gameplay;

/**
 * Tracks elapsed in-game time (paused time excluded) and determines
 * the current stage + which enemy types are unlocked.
 */
public class SpawnDirector {
    private double elapsedTime;
    private int currentStage;
    private double enemySpawnCooldown;
    private double pointSpawnCooldown;
    private double killSpawnCooldown;

    public SpawnDirector() {
        this.elapsedTime = 0;
        this.currentStage = 1;
        this.enemySpawnCooldown = GameplayConfig.ENEMY_SPAWN_INTERVAL;
        this.pointSpawnCooldown = 0; // spawn immediately on first tick
        this.killSpawnCooldown = GameplayConfig.KILL_SPAWN_COOLDOWN;
    }

    /**
     * Advance elapsed time by delta and update stage progression.
     */
    public void update(double delta) {
        elapsedTime += delta;

        if (elapsedTime >= GameplayConfig.STAGE_3_TIME) {
            currentStage = 3;
        } else if (elapsedTime >= GameplayConfig.STAGE_2_TIME) {
            currentStage = 2;
        } else {
            currentStage = 1;
        }

        enemySpawnCooldown -= delta;
        pointSpawnCooldown -= delta;
        killSpawnCooldown -= delta;
    }

    /** Whether the given enemy type is currently unlocked. */
    public boolean isTypeUnlocked(EnemyType type) {
        return currentStage >= type.unlockStage;
    }

    /** True when the enemy spawn timer has elapsed. Resets on consumption. */
    public boolean shouldSpawnEnemy() {
        if (enemySpawnCooldown <= 0) {
            enemySpawnCooldown = GameplayConfig.ENEMY_SPAWN_INTERVAL;
            return true;
        }
        return false;
    }

    /** True when the point pickup spawn timer has elapsed. Resets on consumption. */
    public boolean shouldSpawnPoint() {
        if (pointSpawnCooldown <= 0) {
            pointSpawnCooldown = GameplayConfig.POINT_SPAWN_INTERVAL;
            return true;
        }
        return false;
    }

    /** True when the kill pickup spawn timer has elapsed. Resets on consumption. */
    public boolean shouldSpawnKill() {
        if (killSpawnCooldown <= 0) {
            killSpawnCooldown = GameplayConfig.KILL_SPAWN_COOLDOWN;
            return true;
        }
        return false;
    }

    public double getElapsedTime() { return elapsedTime; }
    public int getCurrentStage()   { return currentStage; }
}
