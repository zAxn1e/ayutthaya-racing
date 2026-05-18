package core.player;

import core.entities.Direction;

import java.awt.event.KeyEvent;
import java.util.EnumMap;

public class PlayerController {
    private final EnumMap<Direction, Boolean> pressed = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, Long> pressedOrder = new EnumMap<>(Direction.class);
    private long inputCounter = 0;

    public PlayerController() {
        for (Direction direction : Direction.values()) {
            pressed.put(direction, false);
            pressedOrder.put(direction, 0L);
        }
    }

    public void onKeyPressed(int keyCode) {
        Direction direction = mapKeyToDirection(keyCode);
        if (direction == Direction.NONE) {
            return;
        }
        pressed.put(direction, true);
        pressedOrder.put(direction, ++inputCounter);
    }

    public void onKeyReleased(int keyCode) {
        Direction direction = mapKeyToDirection(keyCode);
        if (direction != Direction.NONE) {
            pressed.put(direction, false);
        }
    }

    public Direction getDesiredDirection() {
        Direction winner = Direction.NONE;
        long bestOrder = 0;

        for (Direction direction : Direction.values()) {
            if (direction == Direction.NONE || !pressed.get(direction)) {
                continue;
            }
            long order = pressedOrder.get(direction);
            if (order > bestOrder) {
                bestOrder = order;
                winner = direction;
            }
        }

        return winner;
    }

    public boolean isPressed(Direction direction) {
        if (direction == null || direction == Direction.NONE) {
            return false;
        }
        Boolean value = pressed.get(direction);
        return value != null && value;
    }

    public String getPressedDirectionsSummary() {
        StringBuilder summary = new StringBuilder();
        for (Direction direction : Direction.values()) {
            if (direction == Direction.NONE || !isPressed(direction)) {
                continue;
            }
            if (!summary.isEmpty()) {
                summary.append(", ");
            }
            summary.append(direction.name());
        }
        return summary.isEmpty() ? "-" : summary.toString();
    }

    private Direction mapKeyToDirection(int keyCode) {
        return switch (keyCode) {
            case KeyEvent.VK_W, KeyEvent.VK_UP -> Direction.UP;
            case KeyEvent.VK_S, KeyEvent.VK_DOWN -> Direction.DOWN;
            case KeyEvent.VK_A, KeyEvent.VK_LEFT -> Direction.LEFT;
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> Direction.RIGHT;
            default -> Direction.NONE;
        };
    }
}
