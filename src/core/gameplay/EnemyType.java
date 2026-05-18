package core.gameplay;

/**
 * Police enemy variants. Each type has its own base speed multiplier,
 * asset key, and stage-unlock timing.
 */
public enum EnemyType {
    FAT("police_fat", 0.45f, 1),
    CAR("police_car", 0.70f, 2),
    CHICKEN("police_chicken", 1.00f, 3);

    /** Resource key used to resolve the sprite under resources/objects/ */
    public final String assetKey;

    /** Speed as a fraction of the player base speed (1.0 = same as player) */
    public final float speedMultiplier;

    /** Stage at which this type becomes available (1-indexed) */
    public final int unlockStage;

    EnemyType(String assetKey, float speedMultiplier, int unlockStage) {
        this.assetKey = assetKey;
        this.speedMultiplier = speedMultiplier;
        this.unlockStage = unlockStage;
    }
}
