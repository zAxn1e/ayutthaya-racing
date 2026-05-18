package panels;

import core.debug.DebugSettings;
import core.debug.PlayerDebugSnapshot;
import core.level.Maze;
import core.player.CollisionDebugInfo;
import core.player.CollisionMap;
import core.player.PlayerController;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

public final class GameDebugRenderer {
    private static final Font HUD_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    private static final Font HUD_TITLE_FONT = new Font(Font.MONOSPACED, Font.BOLD, 12);

    private GameDebugRenderer() {
    }

    public static void render(Graphics2D g2, Maze maze, CollisionMap collisionMap, PlayerDebugSnapshot snapshot,
                              PlayerController controller, DebugSettings settings) {
        if (!settings.isEnabled()) {
            return;
        }

        if (settings.isWalkableOverlayVisible()) {
            drawWalkableOverlay(g2, maze);
        }
        if (settings.isGridVisible()) {
            drawGrid(g2, collisionMap);
        }
        if (settings.isPlayerHitboxVisible()) {
            drawPlayerDebug(g2, snapshot);
        }
        if (settings.isCollisionSamplesVisible()) {
            drawCollisionChecks(g2, snapshot.getRecentChecks(), maze.getTileSize(), settings.isRecentChecksVisible());
        }
        if (settings.isHudVisible()) {
            drawHud(g2, snapshot, controller, settings);
        }
    }

    private static void drawWalkableOverlay(Graphics2D g2, Maze maze) {
        int tileSize = maze.getTileSize();
        for (int y = 0; y < maze.getHeightInTiles(); y++) {
            for (int x = 0; x < maze.getWidthInTiles(); x++) {
                if (maze.isWalkable(x, y)) {
                    g2.setColor(new Color(40, 180, 99, 28));
                } else {
                    g2.setColor(new Color(231, 76, 60, 42));
                }
                g2.fillRect(x * tileSize, y * tileSize, tileSize, tileSize);
            }
        }
    }

    private static void drawGrid(Graphics2D g2, CollisionMap collisionMap) {
        int tileSize = collisionMap.getTileSize();
        g2.setColor(new Color(255, 255, 255, 35));
        for (int x = 0; x <= collisionMap.getWidthInTiles(); x++) {
            int px = x * tileSize;
            g2.drawLine(px, 0, px, collisionMap.getHeightInTiles() * tileSize);
        }
        for (int y = 0; y <= collisionMap.getHeightInTiles(); y++) {
            int py = y * tileSize;
            g2.drawLine(0, py, collisionMap.getWidthInTiles() * tileSize, py);
        }
    }

    private static void drawPlayerDebug(Graphics2D g2, PlayerDebugSnapshot snapshot) {
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(52, 152, 219, 220));
        g2.draw(new Rectangle(Math.round(snapshot.getX()), Math.round(snapshot.getY()),
                Math.round(snapshot.getWidth()), Math.round(snapshot.getHeight())));

        g2.setColor(new Color(241, 196, 15, 230));
        int centerX = Math.round(snapshot.getCenterX());
        int centerY = Math.round(snapshot.getCenterY());
        g2.drawLine(centerX - 6, centerY, centerX + 6, centerY);
        g2.drawLine(centerX, centerY - 6, centerX, centerY + 6);
    }

    private static void drawCollisionChecks(Graphics2D g2, List<CollisionDebugInfo> checks, int tileSize, boolean drawHistory) {
        int start = drawHistory ? Math.max(0, checks.size() - 5) : Math.max(0, checks.size() - 1);
        for (int i = start; i < checks.size(); i++) {
            CollisionDebugInfo check = checks.get(i);
            Color outline = check.canOccupy() ? new Color(46, 204, 113, 180) : new Color(231, 76, 60, 210);
            g2.setColor(outline);
            g2.drawRect(Math.round(check.getX()), Math.round(check.getY()),
                    Math.round(check.getWidth()), Math.round(check.getHeight()));

            for (CollisionDebugInfo.TileHit tile : check.getTiles()) {
                g2.setColor(tile.isWalkable() ? new Color(46, 204, 113, 24) : new Color(231, 76, 60, 54));
                g2.fillRect(tile.getTileX() * tileSize, tile.getTileY() * tileSize, tileSize, tileSize);
            }

            for (CollisionDebugInfo.SamplePoint sample : check.getSamplePoints()) {
                g2.setColor(sample.isWalkable() ? new Color(46, 204, 113, 220) : new Color(231, 76, 60, 220));
                int px = Math.round(sample.getX());
                int py = Math.round(sample.getY());
                g2.fillOval(px - 3, py - 3, 6, 6);
            }
        }
    }

    private static void drawHud(Graphics2D g2, PlayerDebugSnapshot snapshot, PlayerController controller,
                                DebugSettings settings) {
        int boxX = 12;
        int boxY = 36;
        int boxWidth = 350;
        int boxHeight = 222;

        g2.setColor(new Color(5, 8, 18, 195));
        g2.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 14, 14);
        g2.setColor(new Color(120, 160, 255, 210));
        g2.drawRoundRect(boxX, boxY, boxWidth, boxHeight, 14, 14);

        g2.setFont(HUD_TITLE_FONT);
        g2.setColor(Color.WHITE);
        g2.drawString("DEBUG MODE", boxX + 12, boxY + 18);

        g2.setFont(HUD_FONT);
        int lineY = boxY + 38;
        line(g2, boxX, lineY, "pos", "%.2f, %.2f".formatted(snapshot.getX(), snapshot.getY()));
        lineY += 16;
        line(g2, boxX, lineY, "center", "%.2f, %.2f".formatted(snapshot.getCenterX(), snapshot.getCenterY()));
        lineY += 16;
        line(g2, boxX, lineY, "tile", snapshot.getGridX() + ", " + snapshot.getGridY());
        lineY += 16;
        line(g2, boxX, lineY, "dir", snapshot.getCurrentDirection() + " -> " + snapshot.getNextDirection());
        lineY += 16;
        line(g2, boxX, lineY, "facing", snapshot.getFacingDirection().name());
        lineY += 16;
        line(g2, boxX, lineY, "moving", snapshot.isMoving() + " | held=" + snapshot.isCurrentDirectionHeld());
        lineY += 16;
        line(g2, boxX, lineY, "input", controller.getPressedDirectionsSummary());
        lineY += 16;
        line(g2, boxX, lineY, "delta", "%.4fs".formatted(snapshot.getDeltaSeconds()));
        lineY += 16;
        line(g2, boxX, lineY, "speed", "%.2f px/s".formatted(snapshot.getSpeed()));
        lineY += 16;
        line(g2, boxX, lineY, "step", snapshot.getStepCount() + " x " + "%.3f".formatted(snapshot.getStepDistance()));
        lineY += 16;
        line(g2, boxX, lineY, "travel", "%.3f".formatted(snapshot.getMoveDistance()));
        lineY += 16;
        line(g2, boxX, lineY, "mode", snapshot.getLastMovementMode());
        lineY += 16;
        line(g2, boxX, lineY, "blocked", snapshot.getLastBlockedReason());

        int hintY = boxY + boxHeight - 12;
        g2.setColor(new Color(190, 200, 220));
        g2.drawString("F1 all  F2 grid  F3 walk  F4 hitbox  F5 samples  F6 HUD  F7 history", boxX + 12, hintY);

        if (settings.isRecentChecksVisible()) {
            drawCheckHistory(g2, snapshot.getRecentChecks(), boxX + boxWidth + 10, boxY);
        }
    }

    private static void drawCheckHistory(Graphics2D g2, List<CollisionDebugInfo> checks, int x, int y) {
        int width = 325;
        int height = 222;
        g2.setColor(new Color(5, 8, 18, 180));
        g2.fillRoundRect(x, y, width, height, 14, 14);
        g2.setColor(new Color(120, 160, 255, 210));
        g2.drawRoundRect(x, y, width, height, 14, 14);
        g2.setColor(Color.WHITE);
        g2.setFont(HUD_TITLE_FONT);
        g2.drawString("RECENT CHECKS", x + 12, y + 18);
        g2.setFont(HUD_FONT);

        int lineY = y + 38;
        int start = Math.max(0, checks.size() - 10);
        for (int i = start; i < checks.size(); i++) {
            CollisionDebugInfo info = checks.get(i);
            g2.setColor(info.canOccupy() ? new Color(120, 255, 160) : new Color(255, 130, 130));
            String text = "%s | %s | %s".formatted(info.getLabel(), info.getResolution(), info.getBlockedReason());
            g2.drawString(text, x + 12, lineY);
            lineY += 16;
            if (lineY > y + height - 12) {
                break;
            }
        }
    }

    private static void line(Graphics2D g2, int boxX, int y, String key, String value) {
        g2.setColor(new Color(130, 180, 255));
        g2.drawString("%-8s".formatted(key), boxX + 12, y);
        g2.setColor(new Color(235, 240, 250));
        g2.drawString(value, boxX + 92, y);
    }
}
