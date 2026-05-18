package core.gameplay;

/**
 * All collectible pickup types in the game.
 */
public enum PickupType {
    /** Standard score pickup */
    POINT("object_point", null),

    /** Kills all FAT police on collection */
    KILL_FAT("kill_fat_00000", EnemyType.FAT),

    /** Kills all CAR police on collection */
    KILL_CAR("kill_car_00000", EnemyType.CAR),

    /** Kills all CHICKEN police on collection */
    KILL_CHICKEN("kill_chicken_00000", EnemyType.CHICKEN);

    /** Resource key used to resolve the sprite under resources/objects/ */
    public final String assetKey;

    /** The enemy type this kill-pickup targets, or null for POINT */
    public final EnemyType targetEnemy;

    PickupType(String assetKey, EnemyType targetEnemy) {
        this.assetKey = assetKey;
        this.targetEnemy = targetEnemy;
    }

    public boolean isKillPickup() {
        return targetEnemy != null;
    }
}
