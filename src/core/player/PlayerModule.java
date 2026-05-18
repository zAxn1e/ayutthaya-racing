package core.player;

import core.debug.PlayerDebugSnapshot;
import core.entities.Direction;
import core.entities.Player;

import java.awt.Graphics2D;

public class PlayerModule {
    private final Player player;
    private final CollisionMap collisionMap;
    private final PlayerController controller;
    private final PlayerDebugSnapshot debugSnapshot;

    public PlayerModule(Player player, CollisionMap collisionMap, PlayerController controller) {
        this.player = player;
        this.collisionMap = collisionMap;
        this.controller = controller;
        this.debugSnapshot = new PlayerDebugSnapshot();
    }

    public void onKeyPressed(int keyCode) {
        controller.onKeyPressed(keyCode);
    }

    public void onKeyReleased(int keyCode) {
        controller.onKeyReleased(keyCode);
    }

    public void update(double deltaSeconds) {
        Direction desiredDirection = controller.getDesiredDirection();
        boolean currentDirectionHeld = controller.isPressed(player.getCurrentDirection());
        player.setDirection(desiredDirection, currentDirectionHeld);

        player.update(deltaSeconds);
        player.move(collisionMap, deltaSeconds, debugSnapshot);
    }

    public void render(Graphics2D g2) {
        player.render(g2);
    }

    public Player getPlayer() {
        return player;
    }

    public CollisionMap getCollisionMap() {
        return collisionMap;
    }

    public PlayerController getController() {
        return controller;
    }

    public PlayerDebugSnapshot getDebugSnapshot() {
        return debugSnapshot;
    }
}
