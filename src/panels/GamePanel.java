package panels;

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
import first_page.SessionContext;
import first_page.Test;
import game.MapMain;

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
    private volatile boolean gameOverLeaderboardShown;

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
        gameOverLeaderboardShown = false;

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
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), actionKey);
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
        SwingUtilities.invokeLater(Test::returnToMainMenu);
    }

    private void restartCurrentGame() {
        pauseMenuOpen = false;
        clearPauseHover();
        clearResultHover();
        stopGameThread();
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof javax.swing.JFrame frame) {
            SwingUtilities.invokeLater(() -> MapMain.launchInFrame(frame));
        }
    }

    private String resolveInitialMazePath() {
        try {
            return ActiveMazeRegistry.resolveRuntimeMazePath(
                    ACTIVE_MAZE_CONFIG_PATH,
                    LEGACY_ACTIVE_MAZE_POINTER,
                    DEFAULT_RUNTIME_MAZE
            );
        } catch (Exception e) {
            return DEFAULT_RUNTIME_MAZE;
        }
    }

    private Maze loadMazeSafely(String mazePath) {
        try {
            return MazeTextLoader.loadFromFile(mazePath, TARGET_TILE_SIZE, null, null);
        } catch (Exception e) {
            System.err.println("Failed to load maze file, fallback to generated layout: " + e.getMessage());
            Maze mapWithBackground = new Maze(TARGET_MAZE_WIDTH, TARGET_MAZE_HEIGHT, TARGET_TILE_SIZE, null, MAP_V2_BG1_FILE_PATH);
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
                true
        );
        if (spawnTile == null) {
            spawnTile = new int[]{1, 1};
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
            tile = new int[]{1, 1};
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
            for (int y = Math.max(1, targetY - radius); y <= Math.min(collisionMap.getHeightInTiles() - 2, targetY + radius); y++) {
                for (int x = Math.max(1, targetX - radius); x <= Math.min(collisionMap.getWidthInTiles() - 2, targetX + radius); x++) {
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
                        return new int[]{x, y};
                    }
                    float px = x * collisionMap.getTileSize() + (collisionMap.getTileSize() - playerWidthForTile(collisionMap)) / 2f;
                    float py = y * collisionMap.getTileSize() + (collisionMap.getTileSize() - playerHeightForTile(collisionMap)) / 2f;
                    if (collisionMap.canOccupy(px, py, playerWidthForTile(collisionMap), playerHeightForTile(collisionMap), margin)) {
                        return new int[]{x, y};
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

    private boolean hasWalkableNeighbor(CollisionMap collisionMap, Maze maze, int tileX, int tileY, boolean zoneRequired) {
        return isValidPlayerTile(collisionMap, maze, tileX + 1, tileY, zoneRequired)
                || isValidPlayerTile(collisionMap, maze, tileX - 1, tileY, zoneRequired)
                || isValidPlayerTile(collisionMap, maze, tileX, tileY + 1, zoneRequired)
                || isValidPlayerTile(collisionMap, maze, tileX, tileY - 1, zoneRequired);
    }

    private boolean isValidPlayerTile(CollisionMap collisionMap, Maze maze, int tileX, int tileY, boolean zoneRequired) {
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
        RuntimeState next = buildRuntimeState(newMaze, nextMazePath, current.playerModule.getPlayer(), current.gameplayManager);
        runtimeState = next;
        activeMazePath = nextMazePath;
        GameplayManager.SwitchReport report = lastEntitySwitchReport;
        System.out.println("Maze shuffle reconcile: enemy p/r/r=" + report.enemyPreserved() + "/" + report.enemyRelocated() + "/" + report.enemyRemoved()
                + " pickup p/r/r=" + report.pickupPreserved() + "/" + report.pickupRelocated() + "/" + report.pickupRemoved());
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
        double scale = Math.min(getWidth() / (double) maze.getPixelWidth(), getHeight() / (double) maze.getPixelHeight());
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
            showLeaderboardForGameOver();
            renderResultMenu(g2, state);
        } else if (pauseMenuOpen) {
            renderPauseMenu(g2, state);
        }
        g2.dispose();
    }

    private void renderHUD(Graphics2D g2, RuntimeState state) {
        int score = state.gameplayManager.getScore();
        int stage = state.gameplayManager.getCurrentStage();
        int enemies = state.gameplayManager.getAliveEnemyCount();
        double elapsed = state.gameplayManager.getElapsedTime();
        int minutes = (int) (elapsed / 60);
        int seconds = (int) (elapsed % 60);
        String zoneInfo = "SpawnZone:" + (state.gameplayManager.isSpawnZoneConfigured() ? "On" : "Fallback");
        String mazeName = new File(state.mazePath).getName();
        String shuffleInfo;
        if (mazeShuffleConfig.enabled()) {
            double remain = Math.max(0.0, mazeShuffleConfig.intervalSeconds() - shuffleElapsedSeconds);
            shuffleInfo = "Shuffle:" + String.format("%.0fs", remain);
        } else {
            shuffleInfo = "Shuffle:Off";
        }
        GameplayManager.SwitchReport switchReport = lastEntitySwitchReport;
        String persistInfo = String.format("Keep E:%d/%d/%d P:%d/%d/%d",
                switchReport.enemyPreserved(),
                switchReport.enemyRelocated(),
                switchReport.enemyRemoved(),
                switchReport.pickupPreserved(),
                switchReport.pickupRelocated(),
                switchReport.pickupRemoved());

        boolean invincible = state.gameplayManager.isInvincible();

        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRect(0, 0, getWidth(), 34);

        if (invincible) {
            double flash = state.gameplayManager.getInvincibilityTimer();
            boolean visible = ((int) (flash * 6)) % 2 == 0;
            g2.setColor(visible ? new Color(255, 100, 100) : new Color(220, 220, 220));
        } else {
            g2.setColor(new Color(220, 220, 220));
        }

        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        String hudText = String.format("Score:%d | Stage:%d | Enemies:%d | Time:%d:%02d | Maze:%s | %s | %s | %s | ESC:Pause",
                score, stage, enemies, minutes, seconds, mazeName, shuffleInfo, zoneInfo, persistInfo);
        g2.drawString(hudText, 10, 22);
    }

    private void renderPauseMenu(Graphics2D g2, RuntimeState state) {
        int panelWidth = 560;
        int panelHeight = 330;
        int x = (getWidth() - panelWidth) / 2;
        int y = (getHeight() - panelHeight) / 2;

        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRect(0, 0, getWidth(), getHeight());

        g2.setColor(new Color(23, 32, 57, 230));
        g2.fillRoundRect(x, y, panelWidth, panelHeight, 18, 18);
        g2.setColor(new Color(226, 233, 249));
        g2.drawRoundRect(x, y, panelWidth, panelHeight, 18, 18);

        int score = state.gameplayManager.getScore();
        int stage = state.gameplayManager.getCurrentStage();
        double elapsed = state.gameplayManager.getElapsedTime();
        int minutes = (int) (elapsed / 60);
        int seconds = (int) (elapsed % 60);

        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 34));
        g2.drawString("PAUSED", x + 140, y + 55);
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        g2.drawString("Score: " + score, x + 165, y + 100);
        g2.drawString("Stage: " + stage + "  |  Time: " + minutes + ":" + String.format("%02d", seconds), x + 85, y + 135);
        g2.drawString("Press ESC to continue", x + 110, y + 170);

        int buttonWidth = 150;
        int buttonHeight = 44;
        int resumeX = x + 32;
        int restartX = x + (panelWidth - buttonWidth) / 2;
        int exitX = x + panelWidth - buttonWidth - 32;
        int buttonY = y + 205;
        pauseResumeBounds = new Rectangle(resumeX, buttonY, buttonWidth, buttonHeight);
        pauseRestartBounds = new Rectangle(restartX, buttonY, buttonWidth, buttonHeight);
        pauseExitBounds = new Rectangle(exitX, buttonY, buttonWidth, buttonHeight);

        g2.setColor(hoverResume ? new Color(92, 128, 216) : new Color(66, 96, 170));
        g2.fillRoundRect(resumeX, buttonY, buttonWidth, buttonHeight, 12, 12);
        g2.setColor(new Color(222, 232, 249));
        g2.drawRoundRect(resumeX, buttonY, buttonWidth, buttonHeight, 12, 12);
        g2.drawString("Resume", resumeX + 34, buttonY + 30);

        g2.setColor(hoverRestart ? new Color(114, 137, 182) : new Color(83, 103, 146));
        g2.fillRoundRect(restartX, buttonY, buttonWidth, buttonHeight, 12, 12);
        g2.setColor(new Color(227, 236, 252));
        g2.drawRoundRect(restartX, buttonY, buttonWidth, buttonHeight, 12, 12);
        g2.drawString("Restart", restartX + 34, buttonY + 30);

        g2.setColor(hoverExit ? new Color(206, 96, 96) : new Color(166, 74, 74));
        g2.fillRoundRect(exitX, buttonY, buttonWidth, buttonHeight, 12, 12);
        g2.setColor(new Color(245, 225, 225));
        g2.drawRoundRect(exitX, buttonY, buttonWidth, buttonHeight, 12, 12);
        g2.drawString("Exit to Main", exitX + 19, buttonY + 30);

        g2.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 15));
        g2.setColor(new Color(180, 190, 210));
        g2.drawString("Current Maze: " + new File(state.mazePath).getName(), x + 40, y + 275);
        g2.drawString("Police touch = -" + core.gameplay.GameplayConfig.ENEMY_TOUCH_PENALTY + " pts", x + 125, y + 300);
    }

    private void handleResultClick(RuntimeState state, java.awt.Point point) {
        if (resultSaveBounds.contains(point)) {
            if (!resultSaved) {
                try {
                    AppDatabase.saveScore(SessionContext.getCurrentUser(), state.gameplayManager.getScore());
                    resultSaved = true;
                    resultStatusMessage = "Score saved";
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

    private void showLeaderboardForGameOver() {
        if (gameOverLeaderboardShown) {
            return;
        }
        gameOverLeaderboardShown = true;
        SwingUtilities.invokeLater(() -> {
            Window owner = SwingUtilities.getWindowAncestor(this);
            LeaderboardUI.open(owner);
        });
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
        int score = state.gameplayManager.getScore();

        g2.setColor(new Color(255, 232, 182));
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 34));
        g2.drawString("GAME OVER", x + 150, y + 60);

        g2.setColor(new Color(237, 238, 246));
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 24));
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
            g2.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 16));
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
            String mazePath
    ) {
    }
}
