package core.debug;

public class DebugSettings {
    public static final boolean DEV_MODE = Boolean.getBoolean("dev.mode");
    private boolean enabled = false;
    private boolean hudVisible = true;
    private boolean gridVisible = false;
    private boolean walkableOverlayVisible = false;
    private boolean playerHitboxVisible = false;
    private boolean collisionSamplesVisible = false;
    private boolean recentChecksVisible = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void toggleEnabled() {
        enabled = !enabled;
    }

    public boolean isHudVisible() {
        return enabled && hudVisible;
    }

    public void toggleHudVisible() {
        hudVisible = !hudVisible;
    }

    public boolean isGridVisible() {
        return enabled && gridVisible;
    }

    public void toggleGridVisible() {
        gridVisible = !gridVisible;
    }

    public boolean isWalkableOverlayVisible() {
        return enabled && walkableOverlayVisible;
    }

    public void toggleWalkableOverlayVisible() {
        walkableOverlayVisible = !walkableOverlayVisible;
    }

    public boolean isPlayerHitboxVisible() {
        return enabled && playerHitboxVisible;
    }

    public void togglePlayerHitboxVisible() {
        playerHitboxVisible = !playerHitboxVisible;
    }

    public boolean isCollisionSamplesVisible() {
        return enabled && collisionSamplesVisible;
    }

    public void toggleCollisionSamplesVisible() {
        collisionSamplesVisible = !collisionSamplesVisible;
    }

    public boolean isRecentChecksVisible() {
        return enabled && recentChecksVisible;
    }

    public void toggleRecentChecksVisible() {
        recentChecksVisible = !recentChecksVisible;
    }
}
