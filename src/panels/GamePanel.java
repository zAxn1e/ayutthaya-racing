package panels;

import core.config.GameFonts;
import core.config.PlayerConfig;
import core.config.ProjectPaths;
import core.data.AppDatabase;
import core.data.LeaderboardUI;
import core.debug.DebugSettings;
import core.entities.Player;
import core.gameplay.GameplayManager;
import core.level.Maze;
import core.level.io.ActiveMazeRegistry;
import core.level.io.MazeShuffleConfig;
import core.level.io.MazeTextLoader;
import core.level.mapv2.MapV2Layouts;
import core.level.mapv2.MapV2MazeConverter;
import core.player.CollisionMap;
import core.player.MazeCollisionMapAdapter;
import core.player.PlayerController;
import core.player.PlayerModule;
import menu.SessionContext;
import menu.MainMenuScreen;
import game.GameLauncher;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.LockSupport;

public class GamePanel extends JPanel implements Runnable {
    private static final int TARGET_FPS = 60;
    private static final int WINDOW_WIDTH = 1024;
    private static final int WINDOW_HEIGHT = 768;
    private static final double FIXED_UPDATE_SECONDS = 1.0 / 120.0;
    private static final double MAX_FRAME_DELTA_SECONDS = 0.25;
    private static final int TARGET_MAZE_WIDTH = 58;
    private static final int TARGET_MAZE_HEIGHT = 44;
    private static final int TARGET_TILE_SIZE = 16;
    private static final String MAP_V2_BG1_FILE_PATH = resolveMapBackgroundPath();
    private static final String ACTIVE_MAZE_CONFIG_PATH = ActiveMazeRegistry.DEFAULT_ACTIVE_MAZE_CONFIG;
    private static final String LEGACY_ACTIVE_MAZE_POINTER = ActiveMazeRegistry.LEGACY_ACTIVE_MAZE_POINTER;
    private static final String DEFAULT_RUNTIME_MAZE = ActiveMazeRegistry.DEFAULT_RUNTIME_MAZE;
    private static final String SHUFFLE_CONFIG_PATH = ProjectPaths.mazeShuffleConfigPath();
    private static final Font HUD_SCORE_FONT = GameFonts.bold(22f);
    private static final Font HUD_HINT_FONT = GameFonts.plain(10f);
    private static final BufferedImage HUD_STATUE_ICON = createHudStatueIcon();

    private final DebugSettings debugSettings;
    private final PlayerController playerController;
    private final MazeShuffleConfig mazeShuffleConfig;
    private volatile RuntimeState runtimeState;
    private Thread gameThread;
    private volatile boolean isRunning;
    private boolean pauseMenuOpen;
    private Rectangle pauseResumeBounds = new Rectangle();
    private Rectangle pauseRestartBounds = new Rectangle();
    private Rectangle pauseExitBounds = new Rectangle();
    private Rectangle resultSaveBounds = new Rectangle();
    private Rectangle resultBackBounds = new Rectangle();
    private boolean hoverResume;
    private boolean hoverRestart;
    private boolean hoverExit;
    private boolean hoverResultSave;
    private boolean hoverResultBack;
    private volatile String activeMazePath;
    private volatile double shuffleElapsedSeconds;
    private volatile GameplayManager.SwitchReport lastEntitySwitchReport;
    private volatile boolean resultSaved;
    private volatile String resultStatusMessage = "";

    public GamePanel() {
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        setBackground(new Color(10, 12, 20));
        setFocusable(true);
        setDoubleBuffered(true);

        debugSettings = new DebugSettings();
        playerController = new PlayerController();

        String initialMazePath = resolveInitialMazePath();
        Maze initialMaze = loadMazeSafely(initialMazePath);
        runtimeState = buildRuntimeState(initialMaze, initialMazePath, null, null);
        activeMazePath = runtimeState.mazePath;
        shuffleElapsedSeconds = 0;
        lastEntitySwitchReport = GameplayManager.SwitchReport.empty();
        mazeShuffleConfig = MazeShuffleConfig.loadFromFile(SHUFFLE_CONFIG_PATH, activeMazePath);

        installInputBindings();
        installPauseMouseHandling();
    }

    private void installInputBindings() {
        bindKeyPressed(KeyEvent.VK_W);
        bindKeyPressed(KeyEvent.VK_A);
        bindKeyPressed(KeyEvent.VK_S);
        bindKeyPressed(KeyEvent.VK_D);
        bindKeyPressed(KeyEvent.VK_UP);
        bindKeyPressed(KeyEvent.VK_DOWN);
        bindKeyPressed(KeyEvent.VK_LEFT);
        bindKeyPressed(KeyEvent.VK_RIGHT);

        bindKeyReleased(KeyEvent.VK_W);
        bindKeyReleased(KeyEvent.VK_A);
        bindKeyReleased(KeyEvent.VK_S);
        bindKeyReleased(KeyEvent.VK_D);
        bindKeyReleased(KeyEvent.VK_UP);
        bindKeyReleased(KeyEvent.VK_DOWN);
        bindKeyReleased(KeyEvent.VK_LEFT);
        bindKeyReleased(KeyEvent.VK_RIGHT);

        bindDebugToggle(KeyEvent.VK_F1);
        bindDebugToggle(KeyEvent.VK_F2);
        bindDebugToggle(KeyEvent.VK_F3);
        bindDebugToggle(KeyEvent.VK_F4);
        bindDebugToggle(KeyEvent.VK_F5);
        bindDebugToggle(KeyEvent.VK_F6);
        bindDebugToggle(KeyEvent.VK_F7);
        bindPauseToggle();
    }

    private void bindKeyPressed(int keyCode) {
        String actionKey = "press-" + keyCode;
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke(keyCode, 0, false), actionKey);
        getActionMap().put(actionKey, new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RuntimeState state = runtimeState;
                if (!pauseMenuOpen && state != null && !state.gameplayManager.isGameOver()) {
                    state.playerModule.onKeyPressed(keyCode);
                }
            }
        });
    }

    private void bindKeyReleased(int keyCode) {
        String actionKey = "release-" + keyCode;
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke(keyCode, 0, true), actionKey);
        getActionMap().put(actionKey, new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RuntimeState state = runtimeState;
                if (!pauseMenuOpen && state != null && !state.gameplayManager.isGameOver()) {
                    state.playerModule.onKeyReleased(keyCode);
                }
            }
        });
    }

    private void bindDebugToggle(int keyCode) {
        String actionKey = "debug-" + keyCode;
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke(keyCode, 0, false), actionKey);
        getActionMap().put(actionKey, new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (handleDebugToggle(keyCode)) {
                    repaint();
                }
            }
        });
    }

    private void bindPauseToggle() {
        String actionKey = "toggle-pause";
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false),
                actionKey);
        getActionMap().put(actionKey, new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RuntimeState state = runtimeState;
                if (state != null && state.gameplayManager.isGameOver()) {
                    return;
                }
                pauseMenuOpen = !pauseMenuOpen;
                if (!pauseMenuOpen) {
                    clearPauseHover();
                }
                repaint();
            }
        });
    }

    private void installPauseMouseHandling() {
        MouseAdapter pauseMouseAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                RuntimeState state = runtimeState;
                if (state != null && state.gameplayManager.isGameOver() && e.getButton() == MouseEvent.BUTTON1) {
                    handleResultClick(state, e.getPoint());
                    return;
                }
                if (!pauseMenuOpen || e.getButton() != MouseEvent.BUTTON1) {
                    return;
                }
                if (pauseResumeBounds.contains(e.getPoint())) {
                    pauseMenuOpen = false;
                    clearPauseHover();
                    repaint();
                } else if (pauseRestartBounds.contains(e.getPoint())) {
                    restartCurrentGame();
                } else if (pauseExitBounds.contains(e.getPoint())) {
                    exitToMainMenu();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                RuntimeState state = runtimeState;
                if (state != null && state.gameplayManager.isGameOver()) {
                    boolean nextHoverSave = resultSaveBounds.contains(e.getPoint());
                    boolean nextHoverBack = resultBackBounds.contains(e.getPoint());
                    if (nextHoverSave != hoverResultSave || nextHoverBack != hoverResultBack) {
                        hoverResultSave = nextHoverSave;
                        hoverResultBack = nextHoverBack;
                        repaint();
                    }
                    return;
                }
                if (!pauseMenuOpen) {
                    return;
                }
                boolean nextHoverResume = pauseResumeBounds.contains(e.getPoint());
                boolean nextHoverRestart = pauseRestartBounds.contains(e.getPoint());
                boolean nextHoverExit = pauseExitBounds.contains(e.getPoint());
                if (nextHoverResume != hoverResume || nextHoverRestart != hoverRestart || nextHoverExit != hoverExit) {
                    hoverResume = nextHoverResume;
                    hoverRestart = nextHoverRestart;
                    hoverExit = nextHoverExit;
                    repaint();
                }
            }
        };
        addMouseListener(pauseMouseAdapter);
        addMouseMotionListener(pauseMouseAdapter);
    }

    private void clearPauseHover() {
        hoverResume = false;
        hoverRestart = false;
        hoverExit = false;
    }

    private void clearResultHover() {
        hoverResultSave = false;
        hoverResultBack = false;
    }

    private void exitToMainMenu() {
        pauseMenuOpen = false;
        clearPauseHover();
        stopGameThread();
        SwingUtilities.invokeLater(MainMenuScreen::returnToMainMenu);
    }

    private void restartCurrentGame() {
        pauseMenuOpen = false;
        clearPauseHover();
        clearResultHover();
        stopGameThread();
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof javax.swing.JFrame frame) {
            SwingUtilities.invokeLater(() -> GameLauncher.launchInFrame(frame));
        }
    }

    private String resolveInitialMazePath() {
        try {
            return ActiveMazeRegistry.resolveRuntimeMazePath(
                    ACTIVE_MAZE_CONFIG_PATH,
                    LEGACY_ACTIVE_MAZE_POINTER,
                    DEFAULT_RUNTIME_MAZE);
        } catch (Exception e) {
            return DEFAULT_RUNTIME_MAZE;
        }
    }

    private Maze loadMazeSafely(String mazePath) {
        try {
            return MazeTextLoader.loadFromFile(mazePath, TARGET_TILE_SIZE, null, null);
        } catch (Exception e) {
            System.err.println("Failed to load maze file, fallback to generated layout: " + e.getMessage());
            Maze mapWithBackground = new Maze(TARGET_MAZE_WIDTH, TARGET_MAZE_HEIGHT, TARGET_TILE_SIZE, null,
                    MAP_V2_BG1_FILE_PATH);
            return MapV2MazeConverter.buildMazeFromWalls(mapWithBackground, MapV2Layouts.map1Walls());
        }
    }

    private static String resolveMapBackgroundPath() {
        String mazeImage = ProjectPaths.imageFilePath("BG1.jpg");
        if (new File(mazeImage).exists()) {
            return mazeImage;
        }
        return ProjectPaths.mapFilePath("base.png");
    }

    private RuntimeState buildRuntimeState(Maze maze, String mazePath, Player existingPlayer,
            GameplayManager existingGameplayManager) {
        CollisionMap newCollisionMap = new MazeCollisionMapAdapter(maze);
        Player player = existingPlayer != null ? existingPlayer : createPlayerAtCenter(newCollisionMap, maze);
        if (existingPlayer != null) {
            relocatePlayerIfNeeded(player, newCollisionMap, maze);
        }
        PlayerModule newPlayerModule = new PlayerModule(player, newCollisionMap, playerController);
        GameplayManager newGameplayManager;
        if (existingGameplayManager == null) {
            newGameplayManager = new GameplayManager(player, newCollisionMap, maze);
            lastEntitySwitchReport = GameplayManager.SwitchReport.empty();
        } else {
            newGameplayManager = existingGameplayManager;
            lastEntitySwitchReport = newGameplayManager.onMazeSwitched(newCollisionMap, maze);
        }
        return new RuntimeState(maze, newCollisionMap, newPlayerModule, newGameplayManager, mazePath);
    }

    private Player createPlayerAtCenter(CollisionMap collisionMap, Maze maze) {
        int[] spawnTile = findNearestValidPlayerTile(
                collisionMap,
                maze,
                collisionMap.getWidthInTiles() / 2,
                collisionMap.getHeightInTiles() / 2,
                maze.hasAnySpawnZone(),
                true);
        if (spawnTile == null) {
            spawnTile = new int[] { 1, 1 };
        }
        int tileSize = collisionMap.getTileSize();
        int playerBodySize = tileSize - PlayerConfig.hitboxInsetForTile(tileSize);
        float playerX = spawnTile[0] * tileSize + (tileSize - playerBodySize) / 2f;
        float playerY = spawnTile[1] * tileSize + (tileSize - playerBodySize) / 2f;
        return new Player(playerX, playerY, tileSize, PlayerConfig.moveSpeedForTile(tileSize));
    }

    private void relocatePlayerIfNeeded(Player player, CollisionMap newCollisionMap, Maze newMaze) {
        int margin = PlayerConfig.collisionMarginForTile(newCollisionMap.getTileSize());
        if (newCollisionMap.canOccupy(player.getX(), player.getY(), player.getWidth(), player.getHeight(), margin)) {
            return;
        }

        int targetX = Math.max(1, Math.min(newCollisionMap.getWidthInTiles() - 2, player.getGridX()));
        int targetY = Math.max(1, Math.min(newCollisionMap.getHeightInTiles() - 2, player.getGridY()));
        boolean zoneRequired = newMaze.hasAnySpawnZone();

        int[] tile = findNearestValidPlayerTile(newCollisionMap, newMaze, targetX, targetY, zoneRequired, true);
        if (tile == null && zoneRequired) {
            tile = findNearestValidPlayerTile(newCollisionMap, newMaze, targetX, targetY, true, false);
        }
        if (tile == null) {
            tile = findNearestValidPlayerTile(newCollisionMap, newMaze, targetX, targetY, false, true);
        }
        if (tile == null) {
            tile = new int[] { 1, 1 };
        }

        int tileSize = newCollisionMap.getTileSize();
        float px = tile[0] * tileSize + (tileSize - player.getWidth()) / 2f;
        float py = tile[1] * tileSize + (tileSize - player.getHeight()) / 2f;
        player.reset(px, py);
    }

    private int[] findNearestValidPlayerTile(CollisionMap collisionMap, Maze maze, int targetX, int targetY,
            boolean zoneRequired, boolean requireCanOccupy) {
        int maxRadius = Math.max(collisionMap.getWidthInTiles(), collisionMap.getHeightInTiles());
        int margin = PlayerConfig.collisionMarginForTile(collisionMap.getTileSize());

        for (int radius = 0; radius <= maxRadius; radius++) {
            for (int y = Math.max(1, targetY - radius); y <= Math.min(collisionMap.getHeightInTiles() - 2,
                    targetY + radius); y++) {
                for (int x = Math.max(1, targetX - radius); x <= Math.min(collisionMap.getWidthInTiles() - 2,
                        targetX + radius); x++) {
                    if (!collisionMap.isWalkable(x, y)) {
                        continue;
                    }
                    if (zoneRequired && !maze.isSpawnZone(x, y)) {
                        continue;
                    }
                    if (!hasWalkableNeighbor(collisionMap, maze, x, y, zoneRequired)) {
                        continue;
                    }
                    if (!requireCanOccupy) {
                        return new int[] { x, y };
                    }
                    float px = x * collisionMap.getTileSize()
                            + (collisionMap.getTileSize() - playerWidthForTile(collisionMap)) / 2f;
                    float py = y * collisionMap.getTileSize()
                            + (collisionMap.getTileSize() - playerHeightForTile(collisionMap)) / 2f;
                    if (collisionMap.canOccupy(px, py, playerWidthForTile(collisionMap),
                            playerHeightForTile(collisionMap), margin)) {
                        return new int[] { x, y };
                    }
                }
            }
        }
        return null;
    }

    private int playerWidthForTile(CollisionMap collisionMap) {
        int tileSize = collisionMap.getTileSize();
        return tileSize - PlayerConfig.hitboxInsetForTile(tileSize);
    }

    private int playerHeightForTile(CollisionMap collisionMap) {
        return playerWidthForTile(collisionMap);
    }

    private boolean hasWalkableNeighbor(CollisionMap collisionMap, Maze maze, int tileX, int tileY,
            boolean zoneRequired) {
        return isValidPlayerTile(collisionMap, maze, tileX + 1, tileY, zoneRequired)
                || isValidPlayerTile(collisionMap, maze, tileX - 1, tileY, zoneRequired)
                || isValidPlayerTile(collisionMap, maze, tileX, tileY + 1, zoneRequired)
                || isValidPlayerTile(collisionMap, maze, tileX, tileY - 1, zoneRequired);
    }

    private boolean isValidPlayerTile(CollisionMap collisionMap, Maze maze, int tileX, int tileY,
            boolean zoneRequired) {
        if (!collisionMap.isWalkable(tileX, tileY)) {
            return false;
        }
        return !zoneRequired || maze.isSpawnZone(tileX, tileY);
    }

    private void switchMaze(String nextMazePath) {
        RuntimeState current = runtimeState;
        if (current == null) {
            return;
        }
        System.out.println("Maze shuffle: switching from " + current.mazePath + " to " + nextMazePath);
        Maze newMaze = loadMazeSafely(nextMazePath);
        RuntimeState next = buildRuntimeState(newMaze, nextMazePath, current.playerModule.getPlayer(),
                current.gameplayManager);
        runtimeState = next;
        activeMazePath = nextMazePath;
        GameplayManager.SwitchReport report = lastEntitySwitchReport;
        System.out.println("Maze shuffle reconcile: enemy p/r/r=" + report.enemyPreserved() + "/"
                + report.enemyRelocated() + "/" + report.enemyRemoved()
                + " pickup p/r/r=" + report.pickupPreserved() + "/" + report.pickupRelocated() + "/"
                + report.pickupRemoved());
    }

    private void updateMazeShuffle(double deltaSeconds) {
        RuntimeState state = runtimeState;
        if (state == null || state.gameplayManager.isGameOver()) {
            return;
        }
        if (!mazeShuffleConfig.enabled()) {
            return;
        }
        shuffleElapsedSeconds += deltaSeconds;
        if (shuffleElapsedSeconds < mazeShuffleConfig.intervalSeconds()) {
            return;
        }
        shuffleElapsedSeconds = 0;
        String nextMazePath = selectNextMazePath();
        if (nextMazePath != null && !nextMazePath.equals(activeMazePath)) {
            switchMaze(nextMazePath);
        }
    }

    private String selectNextMazePath() {
        List<String> mazes = mazeShuffleConfig.mazePaths();
        if (mazes.isEmpty()) {
            return null;
        }
        if (mazes.size() == 1) {
            return mazes.get(0);
        }

        String current = activeMazePath;
        for (int attempt = 0; attempt < 10; attempt++) {
            String candidate = mazes.get(ThreadLocalRandom.current().nextInt(mazes.size()));
            if (!candidate.equals(current)) {
                return candidate;
            }
        }

        for (String path : mazes) {
            if (!path.equals(current)) {
                return path;
            }
        }
        return mazes.get(0);
    }

    public void startGameThread() {
        if (gameThread != null && gameThread.isAlive()) {
            return;
        }

        isRunning = true;
        gameThread = new Thread(this, "player-loop");
        gameThread.setDaemon(true);
        gameThread.start();
        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }

    public void stopGameThread() {
        isRunning = false;
        Thread runningThread = gameThread;
        if (runningThread != null) {
            runningThread.interrupt();
        }
    }

    @Override
    public void removeNotify() {
        stopGameThread();
        super.removeNotify();
    }

    private boolean handleDebugToggle(int keyCode) {
        if (!DebugSettings.DEV_MODE)
            return false;
        switch (keyCode) {
            case KeyEvent.VK_F1 -> debugSettings.toggleEnabled();
            case KeyEvent.VK_F2 -> debugSettings.toggleGridVisible();
            case KeyEvent.VK_F3 -> debugSettings.toggleWalkableOverlayVisible();
            case KeyEvent.VK_F4 -> debugSettings.togglePlayerHitboxVisible();
            case KeyEvent.VK_F5 -> debugSettings.toggleCollisionSamplesVisible();
            case KeyEvent.VK_F6 -> debugSettings.toggleHudVisible();
            case KeyEvent.VK_F7 -> debugSettings.toggleRecentChecksVisible();
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public void run() {
        final long frameTimeNs = 1_000_000_000L / TARGET_FPS;
        long lastTime = System.nanoTime();
        double accumulator = 0.0;

        while (isRunning) {
            long now = System.nanoTime();
            double delta = (now - lastTime) / 1_000_000_000.0;
            lastTime = now;
            delta = Math.min(delta, MAX_FRAME_DELTA_SECONDS);
            accumulator += delta;

            while (accumulator >= FIXED_UPDATE_SECONDS) {
                if (!pauseMenuOpen) {
                    RuntimeState state = runtimeState;
                    if (state != null) {
                        if (!state.gameplayManager.isGameOver()) {
                            state.playerModule.update(FIXED_UPDATE_SECONDS);
                            state.gameplayManager.update(FIXED_UPDATE_SECONDS);
                        }
                    }
                    updateMazeShuffle(FIXED_UPDATE_SECONDS);
                }
                accumulator -= FIXED_UPDATE_SECONDS;
            }

            repaint();

            long workTimeNs = System.nanoTime() - now;
            long sleepNs = frameTimeNs - workTimeNs;
            if (sleepNs > 0) {
                LockSupport.parkNanos(sleepNs);
            }
            if (Thread.currentThread().isInterrupted()) {
                isRunning = false;
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        RuntimeState state = runtimeState;
        if (state == null) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(getBackground());
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Maze maze = state.maze;
        double scale = Math.min(getWidth() / (double) maze.getPixelWidth(),
                getHeight() / (double) maze.getPixelHeight());
        int renderWidth = (int) Math.round(maze.getPixelWidth() * scale);
        int renderHeight = (int) Math.round(maze.getPixelHeight() * scale);
        int offsetX = (getWidth() - renderWidth) / 2;
        int offsetY = (getHeight() - renderHeight) / 2;

        Graphics2D world = (Graphics2D) g2.create();
        world.translate(offsetX, offsetY);
        world.scale(scale, scale);
        maze.render(world);
        state.gameplayManager.render(world);
        state.playerModule.render(world);
        GameDebugRenderer.render(world, maze, state.collisionMap, state.playerModule.getDebugSnapshot(),
                state.playerModule.getController(), debugSettings);
        world.dispose();

        renderHUD(g2, state);
        if (state.gameplayManager.isGameOver()) {
            pauseMenuOpen = false;
            clearPauseHover();
            renderResultMenu(g2, state);
        } else if (pauseMenuOpen) {
            renderPauseMenu(g2, state);
        }
        g2.dispose();
    }

    private void renderHUD(Graphics2D g2, RuntimeState state) {
        int score = state.gameplayManager.getScore();
        boolean invincible = state.gameplayManager.isInvincible();
        int panelX = 16;
        int panelY = 10;
        int panelWidth = 210;
        int panelHeight = 58;
        int iconSize = 42;
        int iconX = panelX + 10;
        int iconY = panelY + 8;

        g2.setColor(new Color(0, 0, 0, 135));
        g2.fill(new RoundRectangle2D.Float(panelX + 4, panelY + 4, panelWidth, panelHeight, 24, 24));

        Color borderColor = new Color(199, 162, 74);
        Color fillColor = new Color(63, 35, 10, 220);
        if (invincible) {
            double flash = state.gameplayManager.getInvincibilityTimer();
            boolean visible = ((int) (flash * 6)) % 2 == 0;
            if (visible) {
                borderColor = new Color(255, 216, 120);
                fillColor = new Color(108, 43, 25, 232);
            }
        }

        g2.setColor(fillColor);
        g2.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 22, 22);
        g2.setColor(borderColor);
        g2.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 22, 22);

        g2.drawImage(HUD_STATUE_ICON, iconX, iconY, iconSize, iconSize, null);

        g2.setColor(new Color(255, 236, 183));
        g2.setFont(HUD_SCORE_FONT);
        g2.drawString(Integer.toString(score), panelX + 66, panelY + 33);

        g2.setFont(HUD_HINT_FONT);
        g2.setColor(new Color(238, 221, 174));
        g2.drawString("Score", panelX + 66, panelY + 48);
        g2.drawString("ESC", panelX + 154, panelY + 48);
    }

    private void renderPauseMenu(Graphics2D g2, RuntimeState state) {
        int panelWidth = 560;
        int panelHeight = 332;
        int x = (getWidth() - panelWidth) / 2;
        int y = (getHeight() - panelHeight) / 2;

        g2.setColor(new Color(7, 10, 18, 190));
        g2.fillRect(0, 0, getWidth(), getHeight());

        g2.setColor(new Color(0, 0, 0, 90));
        g2.fillRoundRect(x + 8, y + 8, panelWidth, panelHeight, 28, 28);
        g2.setColor(new Color(20, 26, 43, 235));
        g2.fillRoundRect(x, y, panelWidth, panelHeight, 26, 26);
        g2.setColor(new Color(205, 173, 92, 235));
        g2.drawRoundRect(x, y, panelWidth, panelHeight, 26, 26);
        g2.setColor(new Color(245, 229, 178, 90));
        g2.drawRoundRect(x + 6, y + 6, panelWidth - 12, panelHeight - 12, 22, 22);

        int score = state.gameplayManager.getScore();
        int stage = state.gameplayManager.getCurrentStage();
        double elapsed = state.gameplayManager.getElapsedTime();
        int minutes = (int) (elapsed / 60);
        int seconds = (int) (elapsed % 60);
        String sessionText = minutes + " min " + String.format("%02d", seconds) + " sec in this run";

        g2.setColor(new Color(249, 233, 193));
        String pausedTitle = "Paused";
        g2.setFont(GameFonts.forText(pausedTitle, Font.BOLD, 28f));
        g2.drawString("Paused", x + 38, y + 58);
        String pausedHint = "Press ESC to continue";
        g2.setFont(GameFonts.forText(pausedHint, Font.PLAIN, 14f));
        g2.setColor(new Color(206, 214, 230));
        g2.drawString(pausedHint, x + 40, y + 82);

        int cardY = y + 104;
        int cardWidth = 148;
        int cardHeight = 78;
        int cardGap = 14;
        drawPauseStatCard(g2, x + 36, cardY, cardWidth, cardHeight, "Score", Integer.toString(score),
                new Color(94, 66, 25), new Color(255, 226, 160));
        drawPauseStatCard(g2, x + 36 + cardWidth + cardGap, cardY, cardWidth, cardHeight, "Stage",
                Integer.toString(stage),
                new Color(33, 64, 100), new Color(172, 219, 255));
        drawPauseStatCard(g2, x + 36 + (cardWidth + cardGap) * 2, cardY, cardWidth, cardHeight, "Time",
                minutes + ":" + String.format("%02d", seconds), new Color(43, 86, 75), new Color(177, 243, 218));

        g2.setColor(new Color(31, 39, 61, 220));
        g2.fillRoundRect(x + 36, y + 198, panelWidth - 72, 46, 16, 16);
        g2.setColor(new Color(110, 127, 166));
        g2.drawRoundRect(x + 36, y + 198, panelWidth - 72, 46, 16, 16);
        g2.setFont(GameFonts.forText("Time played: " + sessionText, Font.PLAIN, 10f));
        g2.setColor(new Color(231, 236, 247));
        g2.drawString("Time played: " + sessionText, x + 54, y + 226);

        int buttonWidth = 132;
        int buttonHeight = 44;
        int resumeX = x + 46;
        int restartX = x + (panelWidth - buttonWidth) / 2;
        int exitX = x + panelWidth - buttonWidth - 46;
        int buttonY = y + 264;
        pauseResumeBounds = new Rectangle(resumeX, buttonY, buttonWidth, buttonHeight);
        pauseRestartBounds = new Rectangle(restartX, buttonY, buttonWidth, buttonHeight);
        pauseExitBounds = new Rectangle(exitX, buttonY, buttonWidth, buttonHeight);

        drawPauseButton(g2, pauseResumeBounds, hoverResume, "Resume", "Back to the run",
                new Color(57, 100, 178), new Color(132, 192, 255));
        drawPauseButton(g2, pauseRestartBounds, hoverRestart, "Restart", "New run",
                new Color(79, 91, 149), new Color(188, 205, 255));
        drawPauseButton(g2, pauseExitBounds, hoverExit, "Exit to Main", "Leave this run",
                new Color(146, 63, 63), new Color(255, 178, 178));
    }

    private void drawPauseStatCard(Graphics2D g2, int x, int y, int width, int height, String label, String value,
            Color fill, Color accent) {
        g2.setColor(fill);
        g2.fillRoundRect(x, y, width, height, 18, 18);
        g2.setColor(accent);
        g2.drawRoundRect(x, y, width, height, 18, 18);
        g2.setFont(GameFonts.forText(label, Font.PLAIN, 12f));
        g2.setColor(new Color(241, 244, 248));
        g2.drawString(label, x + 18, y + 24);
        g2.setFont(GameFonts.forText(value, Font.BOLD, 22f));
        g2.drawString(value, x + 18, y + 54);
    }

    private void drawPauseButton(Graphics2D g2, Rectangle bounds, boolean hovered, String title, String subtitle,
            Color fill, Color accent) {
        Color body = hovered ? accent : fill;
        g2.setColor(body);
        g2.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 16, 16);
        g2.setColor(new Color(245, 245, 245, hovered ? 235 : 190));
        g2.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 16, 16);
        g2.setColor(new Color(249, 249, 250));
        g2.setFont(GameFonts.forText(title, Font.BOLD, 9f));
        g2.drawString(title, bounds.x + 14, bounds.y + 18);
        g2.setFont(GameFonts.forText(subtitle, Font.PLAIN, 7f));
        g2.drawString(subtitle, bounds.x + 14, bounds.y + 31);
    }

    private void handleResultClick(RuntimeState state, java.awt.Point point) {
        if (resultSaveBounds.contains(point)) {
            if (!resultSaved) {
                try {
                    AppDatabase.saveScore(SessionContext.getCurrentUser(), state.gameplayManager.getFinalScore());
                    resultSaved = true;
                    resultStatusMessage = "Score saved";
                    Window owner = SwingUtilities.getWindowAncestor(this);
                    LeaderboardUI.open(owner);
                } catch (Exception ex) {
                    resultStatusMessage = "Save failed: " + ex.getMessage();
                }
            }
            repaint();
            return;
        }
        if (resultBackBounds.contains(point)) {
            exitToMainMenu();
        }
    }

    private void renderResultMenu(Graphics2D g2, RuntimeState state) {
        int panelWidth = 560;
        int panelHeight = 320;
        int x = (getWidth() - panelWidth) / 2;
        int y = (getHeight() - panelHeight) / 2;

        g2.setColor(new Color(0, 0, 0, 190));
        g2.fillRect(0, 0, getWidth(), getHeight());

        g2.setColor(new Color(52, 23, 23, 235));
        g2.fillRoundRect(x, y, panelWidth, panelHeight, 18, 18);
        g2.setColor(new Color(255, 220, 220));
        g2.drawRoundRect(x, y, panelWidth, panelHeight, 18, 18);

        String user = SessionContext.getCurrentUser();
        int score = state.gameplayManager.getFinalScore();

        g2.setColor(new Color(255, 232, 182));
        g2.setFont(GameFonts.bold(30f));
        g2.drawString("GAME OVER", x + 150, y + 60);

        g2.setColor(new Color(237, 238, 246));
        g2.setFont(GameFonts.plain(22f));
        g2.drawString("Player: " + user, x + 70, y + 120);
        g2.drawString("Final Score: " + score, x + 70, y + 160);

        int buttonWidth = 180;
        int buttonHeight = 46;
        int saveX = x + 65;
        int backX = x + panelWidth - buttonWidth - 65;
        int buttonY = y + 210;
        resultSaveBounds = new Rectangle(saveX, buttonY, buttonWidth, buttonHeight);
        resultBackBounds = new Rectangle(backX, buttonY, buttonWidth, buttonHeight);

        g2.setColor(hoverResultSave ? new Color(95, 140, 98) : new Color(72, 109, 74));
        g2.fillRoundRect(saveX, buttonY, buttonWidth, buttonHeight, 12, 12);
        g2.setColor(new Color(222, 242, 223));
        g2.drawRoundRect(saveX, buttonY, buttonWidth, buttonHeight, 12, 12);
        g2.drawString(resultSaved ? "Saved" : "Save Score", saveX + 39, buttonY + 31);

        g2.setColor(hoverResultBack ? new Color(124, 132, 186) : new Color(90, 99, 150));
        g2.fillRoundRect(backX, buttonY, buttonWidth, buttonHeight, 12, 12);
        g2.setColor(new Color(224, 231, 255));
        g2.drawRoundRect(backX, buttonY, buttonWidth, buttonHeight, 12, 12);
        g2.drawString("Back to Main", backX + 28, buttonY + 31);

        if (!resultStatusMessage.isBlank()) {
            g2.setFont(GameFonts.italic(15f));
            g2.setColor(new Color(230, 230, 230));
            g2.drawString(resultStatusMessage, x + 65, y + 285);
        }
    }

    public Player getPlayer() {
        RuntimeState state = runtimeState;
        return state == null ? null : state.playerModule.getPlayer();
    }

    private record RuntimeState(
            Maze maze,
            CollisionMap collisionMap,
            PlayerModule playerModule,
            GameplayManager gameplayManager,
            String mazePath) {
    }

    private static BufferedImage createHudStatueIcon() {
        BufferedImage image = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color dark = new Color(89, 50, 6);
        Color mid = new Color(166, 107, 18);
        Color gold = new Color(220, 171, 58);
        Color highlight = new Color(255, 228, 122);
        Color shadow = new Color(56, 29, 0, 180);

        g2.setColor(shadow);
        g2.fillOval(15, 67, 48, 8);

        g2.setColor(dark);
        g2.fillRoundRect(11, 55, 58, 10, 6, 6);
        g2.fillRoundRect(17, 48, 45, 10, 6, 6);
        g2.fillRoundRect(23, 41, 33, 10, 6, 6);

        g2.setColor(mid);
        g2.fillRoundRect(13, 53, 56, 10, 6, 6);
        g2.fillRoundRect(18, 46, 44, 10, 6, 6);
        g2.fillRoundRect(24, 39, 32, 10, 6, 6);

        g2.setColor(gold);
        g2.fillRoundRect(27, 21, 26, 24, 12, 12);
        g2.fillOval(31, 7, 18, 20);
        g2.fillRoundRect(24, 31, 32, 8, 6, 6);
        g2.fillRoundRect(22, 37, 36, 8, 6, 6);

        g2.setColor(mid);
        g2.fillRoundRect(23, 28, 13, 18, 8, 8);
        g2.fillRoundRect(45, 28, 13, 18, 8, 8);
        g2.fillRoundRect(28, 43, 24, 8, 6, 6);

        g2.setColor(highlight);
        g2.fillOval(35, 11, 8, 7);
        g2.fillRoundRect(34, 24, 8, 15, 4, 4);
        g2.fillRoundRect(29, 44, 18, 4, 4, 4);
        g2.fillRoundRect(19, 48, 40, 3, 3, 3);
        g2.fillRoundRect(18, 56, 41, 3, 3, 3);

        g2.setColor(dark);
        g2.fillOval(37, 19, 2, 2);
        g2.fillOval(42, 19, 2, 2);
        g2.drawArc(38, 22, 6, 4, 190, 160);
        g2.drawLine(31, 34, 28, 43);
        g2.drawLine(49, 34, 52, 43);
        g2.drawLine(33, 34, 46, 34);

        g2.dispose();
        return image;
    }
}
