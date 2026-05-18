package editor;

import core.config.ProjectPaths;
import core.gameplay.EnemyType;
import core.gameplay.GameplayConfig;
import core.level.Maze;
import core.level.io.ActiveMazeRegistry;
import core.level.io.MazeMetadata;
import core.level.mapv2.MapV2Layouts;
import core.level.mapv2.MapV2MazeConverter;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MazeEditorWindow extends JFrame {
    private static final String DEFAULT_IMPORTED_BACKGROUND = ProjectPaths.imageFilePath("BG1.jpg");

    private MazeEditorDocument document;
    private final MazeEditorPanel editorPanel;
    private final JLabel statusLabel = new JLabel();
    private final JLabel pointerLabel = new JLabel("Cell: -");
    private final JTextField mapNameField = new JTextField(16);
    private final JTextField tileSizeField = new JTextField(4);
    private final JTextField backgroundField = new JTextField(16);
    private final JComboBox<Maze.Tile> tileSelector = new JComboBox<>(Maze.Tile.values());
    private final JComboBox<MazeEditorTool> toolSelector = new JComboBox<>(MazeEditorTool.values());

    public MazeEditorWindow() {
        super("Maze Editor");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        document = MazeEditorDocument.createBlank(58, 44, 16);
        editorPanel = new MazeEditorPanel(document);
        editorPanel.setTilePickedListener(tile -> tileSelector.setSelectedItem(tile));
        editorPanel.setChangeListener(this::refreshStatus);

        setJMenuBar(createMenuBar());
        add(createTopBar(), BorderLayout.NORTH);
        add(new JScrollPane(editorPanel), BorderLayout.CENTER);
        add(createSidePanel(), BorderLayout.EAST);
        add(createBottomBar(), BorderLayout.SOUTH);

        toolSelector.addActionListener(e -> editorPanel.setTool((MazeEditorTool) toolSelector.getSelectedItem()));
        tileSelector.addActionListener(e -> editorPanel.setSelectedTile((Maze.Tile) tileSelector.getSelectedItem()));

        mapNameField.addActionListener(e -> {
            document.setMetadataValue("name", mapNameField.getText());
            refreshStatus();
        });

        refreshMetadataFields();
        refreshStatus();
        setSize(1400, 900);
        setLocationRelativeTo(null);

        new javax.swing.Timer(100, e -> updatePointerLabel()).start();
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem newItem = new JMenuItem("New");
        newItem.addActionListener(e -> createNewMaze());
        JMenuItem openItem = new JMenuItem("Open...");
        openItem.addActionListener(e -> openMaze());
        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.addActionListener(e -> saveMaze(false));
        JMenuItem saveAsItem = new JMenuItem("Save As...");
        saveAsItem.addActionListener(e -> saveMaze(true));
        JMenuItem setActiveItem = new JMenuItem("Set As Active For Game");
        setActiveItem.addActionListener(e -> setAsActiveMaze());
        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.add(setActiveItem);

        JMenu editMenu = new JMenu("Edit");
        JMenuItem undoItem = new JMenuItem("Undo");
        undoItem.addActionListener(e -> {
            document.undo();
            editorPanel.refreshFromDocument();
            refreshStatus();
        });
        JMenuItem redoItem = new JMenuItem("Redo");
        redoItem.addActionListener(e -> {
            document.redo();
            editorPanel.refreshFromDocument();
            refreshStatus();
        });
        JMenuItem resizeItem = new JMenuItem("Resize Canvas...");
        resizeItem.addActionListener(e -> resizeMaze());
        editMenu.add(undoItem);
        editMenu.add(redoItem);
        editMenu.add(resizeItem);

        JMenu importMenu = new JMenu("Import");
        JMenuItem importMap1 = new JMenuItem("MapV2 Layout 1");
        importMap1.addActionListener(e -> importMapV2(1));
        JMenuItem importMap2 = new JMenuItem("MapV2 Layout 2");
        importMap2.addActionListener(e -> importMapV2(2));
        importMenu.add(importMap1);
        importMenu.add(importMap2);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(importMenu);
        return menuBar;
    }

    private JPanel createTopBar() {
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        topBar.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        topBar.add(new JLabel("Tool"));
        topBar.add(toolSelector);
        topBar.add(new JLabel("Tile"));
        topBar.add(tileSelector);

        JSpinner zoomSpinner = new JSpinner(new SpinnerNumberModel(editorPanel.getZoom(), 1, 8, 1));
        zoomSpinner.addChangeListener(e -> editorPanel.setZoom((Integer) zoomSpinner.getValue()));
        topBar.add(new JLabel("Zoom"));
        topBar.add(zoomSpinner);

        JCheckBox labels = new JCheckBox("Labels", true);
        labels.addActionListener(e -> editorPanel.setShowLabels(labels.isSelected()));
        topBar.add(labels);

        JCheckBox spawnZoneLayerToggle = new JCheckBox("Edit Spawn Zone Layer");
        spawnZoneLayerToggle.addActionListener(e -> editorPanel.setEditingSpawnZoneLayer(spawnZoneLayerToggle.isSelected()));
        topBar.add(spawnZoneLayerToggle);

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> saveMaze(false));
        topBar.add(saveButton);

        JButton undoButton = new JButton("Undo");
        undoButton.addActionListener(e -> {
            document.undo();
            editorPanel.refreshFromDocument();
            refreshStatus();
        });
        topBar.add(undoButton);

        JButton redoButton = new JButton("Redo");
        redoButton.addActionListener(e -> {
            document.redo();
            editorPanel.refreshFromDocument();
            refreshStatus();
        });
        topBar.add(redoButton);

        return topBar;
    }

    private JPanel createSidePanel() {
        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        sidePanel.setPreferredSize(new Dimension(270, 0));

        sidePanel.add(sectionTitle("Palette"));
        JPanel palettePanel = new JPanel(new GridLayout(0, 2, 6, 6));
        ButtonGroup paletteGroup = new ButtonGroup();
        for (Maze.Tile tile : Maze.Tile.values()) {
            JToggleButton button = new JToggleButton(tile.getDisplayName());
            button.setBackground(tile.getEditorColor());
            button.setOpaque(true);
            button.setForeground(tile == Maze.Tile.WALL ? Color.WHITE : Color.BLACK);
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.addActionListener(e -> {
                tileSelector.setSelectedItem(tile);
                editorPanel.setSelectedTile(tile);
            });
            if (tile == Maze.Tile.WALL) {
                button.setSelected(true);
            }
            paletteGroup.add(button);
            palettePanel.add(button);
        }
        sidePanel.add(palettePanel);

        sidePanel.add(sectionTitle("Map Info"));
        JPanel metaPanel = new JPanel(new GridLayout(0, 2, 6, 6));
        metaPanel.add(new JLabel("Name"));
        metaPanel.add(mapNameField);
        tileSizeField.setEditable(false);
        metaPanel.add(new JLabel("Tile Size"));
        metaPanel.add(tileSizeField);
        backgroundField.setEditable(false);
        metaPanel.add(new JLabel("Background"));
        metaPanel.add(backgroundField);
        sidePanel.add(metaPanel);

        JButton chooseBackground = new JButton("Choose Background...");
        chooseBackground.addActionListener(e -> chooseBackground());
        sidePanel.add(chooseBackground);

        JButton clearBackground = new JButton("Clear Background");
        clearBackground.addActionListener(e -> {
            document.setMetadataValue("background", "");
            refreshMetadataFields();
            refreshStatus();
        });
        sidePanel.add(clearBackground);

        JButton setActiveButton = new JButton("Use In Game");
        setActiveButton.addActionListener(e -> setAsActiveMaze());
        sidePanel.add(setActiveButton);

        sidePanel.add(sectionTitle("Quick Actions"));
        JButton borderWalls = new JButton("Seal Border With Walls");
        borderWalls.addActionListener(e -> {
            document.beginEdit();
            Maze maze = document.getMaze();
            for (int y = 0; y < maze.getHeightInTiles(); y++) {
                for (int x = 0; x < maze.getWidthInTiles(); x++) {
                    if (x == 0 || y == 0 || x == maze.getWidthInTiles() - 1 || y == maze.getHeightInTiles() - 1) {
                        document.setTile(x, y, Maze.Tile.WALL);
                    }
                }
            }
            editorPanel.refreshFromDocument();
            refreshStatus();
        });
        sidePanel.add(borderWalls);

        JButton clearWalkable = new JButton("Clear To Empty");
        clearWalkable.addActionListener(e -> {
            document.beginEdit();
            Maze maze = document.getMaze();
            for (int y = 0; y < maze.getHeightInTiles(); y++) {
                for (int x = 0; x < maze.getWidthInTiles(); x++) {
                    document.setTile(x, y, Maze.Tile.EMPTY);
                }
            }
            editorPanel.refreshFromDocument();
            refreshStatus();
        });
        sidePanel.add(clearWalkable);

        JButton normalizeSpawns = new JButton("Keep One Player Spawn");
        normalizeSpawns.addActionListener(e -> normalizeSingleSpecialTile(Maze.Tile.PLAYER_SPAWN));
        sidePanel.add(normalizeSpawns);

        JButton normalizeGhost = new JButton("Keep One Ghost Spawn");
        normalizeGhost.addActionListener(e -> normalizeSingleSpecialTile(Maze.Tile.GHOST_SPAWN));
        sidePanel.add(normalizeGhost);

        return sidePanel;
    }

    private JPanel createBottomBar() {
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        bottomBar.add(statusLabel, BorderLayout.WEST);
        bottomBar.add(pointerLabel, BorderLayout.EAST);
        return bottomBar;
    }

    private JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setBorder(BorderFactory.createEmptyBorder(12, 0, 6, 0));
        return label;
    }

    private void createNewMaze() {
        JSpinner widthSpinner = new JSpinner(new SpinnerNumberModel(58, 5, 300, 1));
        JSpinner heightSpinner = new JSpinner(new SpinnerNumberModel(44, 5, 300, 1));
        JSpinner tileSizeSpinner = new JSpinner(new SpinnerNumberModel(16, 4, 128, 1));
        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        panel.add(new JLabel("Width"));
        panel.add(widthSpinner);
        panel.add(new JLabel("Height"));
        panel.add(heightSpinner);
        panel.add(new JLabel("Tile Size"));
        panel.add(tileSizeSpinner);

        if (JOptionPane.showConfirmDialog(this, panel, "New Maze", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            document = MazeEditorDocument.createBlank((Integer) widthSpinner.getValue(), (Integer) heightSpinner.getValue(),
                    (Integer) tileSizeSpinner.getValue());
            editorPanel.setDocument(document);
            refreshMetadataFields();
            refreshStatus();
        }
    }

    private void resizeMaze() {
        Maze maze = document.getMaze();
        JSpinner widthSpinner = new JSpinner(new SpinnerNumberModel(maze.getWidthInTiles(), 5, 300, 1));
        JSpinner heightSpinner = new JSpinner(new SpinnerNumberModel(maze.getHeightInTiles(), 5, 300, 1));
        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        panel.add(new JLabel("Width"));
        panel.add(widthSpinner);
        panel.add(new JLabel("Height"));
        panel.add(heightSpinner);

        if (JOptionPane.showConfirmDialog(this, panel, "Resize Maze", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            int newWidth = (Integer) widthSpinner.getValue();
            int newHeight = (Integer) heightSpinner.getValue();
            document.beginEdit();
            Maze resized = new Maze(newWidth, newHeight, maze.getTileSize(), null, null);
            for (int y = 0; y < Math.min(newHeight, maze.getHeightInTiles()); y++) {
                for (int x = 0; x < Math.min(newWidth, maze.getWidthInTiles()); x++) {
                    resized.setTile(x, y, maze.getTile(x, y));
                }
            }
            document.replaceMaze(resized);
            editorPanel.refreshFromDocument();
            refreshMetadataFields();
            refreshStatus();
        }
    }

    private void importMapV2(int layoutIndex) {
        Maze imported = new Maze(58, 44, 16, null, null);
        if (layoutIndex == 1) {
            imported = MapV2MazeConverter.buildMazeFromWalls(imported, MapV2Layouts.map1Walls());
        } else {
            imported = MapV2MazeConverter.buildMazeFromWalls(imported, MapV2Layouts.map2Walls());
        }
        document.beginEdit();
        document.replaceMaze(imported);
        document.setMetadataValue("name", "map_v2_layout_" + layoutIndex);
        document.setMetadataValue("background", DEFAULT_IMPORTED_BACKGROUND);
        editorPanel.refreshFromDocument();
        refreshMetadataFields();
        refreshStatus();
    }

    private void openMaze() {
        JFileChooser chooser = new JFileChooser(ProjectPaths.mazeDirectory());
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {
            document = MazeEditorDocument.load(chooser.getSelectedFile(), 16);
            editorPanel.setDocument(document);
            refreshMetadataFields();
            refreshStatus();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Open failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveMaze(boolean saveAs) {
        if (!confirmZoneWarnings("Save")) {
            return;
        }
        File target = document.getCurrentFile();
        if (saveAs || target == null) {
            JFileChooser chooser = new JFileChooser(ProjectPaths.mazeDirectory());
            if (document.getCurrentFile() != null) {
                chooser.setSelectedFile(document.getCurrentFile());
            }
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            target = chooser.getSelectedFile();
        }

        document.setMetadataValue("name", mapNameField.getText());
        try {
            document.save(target);
            refreshStatus();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Save failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void chooseBackground() {
        File initialDir = ProjectPaths.imageDirectory().exists()
                ? ProjectPaths.imageDirectory()
                : ProjectPaths.mapDirectory();
        JFileChooser chooser = new JFileChooser(initialDir);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selected = chooser.getSelectedFile();
        String backgroundValue = selected.getPath();
        File mazeFile = document.getCurrentFile();
        if (mazeFile != null && mazeFile.getParentFile() != null) {
            try {
                backgroundValue = mazeFile.getParentFile().toPath().relativize(selected.toPath()).toString();
            } catch (Exception ignored) {
                backgroundValue = selected.getPath();
            }
        }

        document.setMetadataValue("background", backgroundValue);
        refreshMetadataFields();
        refreshStatus();
    }

    private void setAsActiveMaze() {
        if (!confirmZoneWarnings("Set Active")) {
            return;
        }
        if (document.getCurrentFile() == null) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Maze must be saved before it can be used in game. Save now?",
                    "Save Required", JOptionPane.OK_CANCEL_OPTION);
            if (choice != JOptionPane.OK_OPTION) {
                return;
            }
            saveMaze(false);
            if (document.getCurrentFile() == null) {
                return;
            }
        }

        try {
            ActiveMazeRegistry.writeActiveMazePath(ActiveMazeRegistry.DEFAULT_ACTIVE_MAZE_CONFIG, document.getCurrentFile());
            refreshStatus();
            JOptionPane.showMessageDialog(this,
                    "Active maze set for game:\n" + document.getCurrentFile().getPath(),
                    "Maze Activated", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to set active maze: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void normalizeSingleSpecialTile(Maze.Tile tileType) {
        Maze maze = document.getMaze();
        boolean foundOne = false;
        document.beginEdit();
        for (int y = 0; y < maze.getHeightInTiles(); y++) {
            for (int x = 0; x < maze.getWidthInTiles(); x++) {
                if (maze.getTile(x, y) == tileType) {
                    if (!foundOne) {
                        foundOne = true;
                    } else {
                        document.setTile(x, y, Maze.Tile.EMPTY);
                    }
                }
            }
        }
        editorPanel.refreshFromDocument();
        refreshStatus();
    }

    private void updatePointerLabel() {
        if (!isDisplayable()) {
            return;
        }
        if (editorPanel.getHoverCell() == null) {
            pointerLabel.setText("Cell: -");
        } else {
            Maze maze = document.getMaze();
            int x = editorPanel.getHoverCell().x;
            int y = editorPanel.getHoverCell().y;
            pointerLabel.setText("Cell: " + x + "," + y + "  Tile: " + maze.getTile(x, y).name());
        }
    }

    private void refreshMetadataFields() {
        MazeMetadata metadata = document.getMetadata();
        mapNameField.setText(metadata.get("name", document.getDisplayName()));
        tileSizeField.setText(Integer.toString(document.getMaze().getTileSize()));
        backgroundField.setText(metadata.get("background", "-"));
    }

    private void refreshStatus() {
        Maze maze = document.getMaze();
        int spawnZoneCount = countSpawnZoneCells();
        String dirtyMarker = document.isDirty() ? " *" : "";
        statusLabel.setText(document.getDisplayName() + dirtyMarker + " | "
                + maze.getWidthInTiles() + "x" + maze.getHeightInTiles()
                + " | tile=" + maze.getTileSize()
                + " | spawnZone=" + spawnZoneCount
                + " | undo=" + document.canUndo()
                + " | redo=" + document.canRedo());
        setTitle("Maze Editor - " + document.getDisplayName() + dirtyMarker);
        refreshMetadataFields();
    }

    private int countSpawnZoneCells() {
        Maze maze = document.getMaze();
        int count = 0;
        for (int y = 0; y < maze.getHeightInTiles(); y++) {
            for (int x = 0; x < maze.getWidthInTiles(); x++) {
                if (maze.isSpawnZone(x, y)) {
                    count++;
                }
            }
        }
        return count;
    }

    private boolean hasUsableSpawnZoneCell() {
        Maze maze = document.getMaze();
        for (int y = 1; y < maze.getHeightInTiles() - 1; y++) {
            for (int x = 1; x < maze.getWidthInTiles() - 1; x++) {
                if (!maze.isSpawnZone(x, y) || !maze.isWalkable(x, y)) {
                    continue;
                }
                if (maze.isWalkable(x + 1, y) || maze.isWalkable(x - 1, y)
                        || maze.isWalkable(x, y + 1) || maze.isWalkable(x, y - 1)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<String> collectZoneWarnings() {
        List<String> warnings = new ArrayList<>();
        int spawnZoneCount = countSpawnZoneCells();

        if (spawnZoneCount == 0) {
            warnings.add("- Spawn Zone ยังไม่ถูกกำหนด (เกมจะ fallback ไปสุ่มทุกจุดที่เดินได้)");
        } else {
            int enemyCap = Math.max(
                    GameplayConfig.maxEnemiesForType(EnemyType.FAT),
                    Math.max(
                            GameplayConfig.maxEnemiesForType(EnemyType.CAR),
                            GameplayConfig.maxEnemiesForType(EnemyType.CHICKEN)
                    )
            );
            if (spawnZoneCount < Math.max(GameplayConfig.MAX_POINT_PICKUPS, enemyCap)) {
                warnings.add("- Spawn Zone มีพื้นที่น้อย อาจไม่พอสำหรับ enemy/pickup พร้อมกัน");
            }
            if (!hasUsableSpawnZoneCell()) {
                warnings.add("- Spawn Zone ไม่มีตำแหน่ง usable (ไม่มีทางเชื่อมเดินได้)");
            }
        }
        return warnings;
    }

    private boolean confirmZoneWarnings(String actionName) {
        List<String> warnings = collectZoneWarnings();
        if (warnings.isEmpty()) {
            return true;
        }
        String message = "พบข้อสังเกตเกี่ยวกับ Spawn Zone:\n\n"
                + String.join("\n", warnings)
                + "\n\nต้องการ " + actionName + " ต่อหรือไม่?";
        int choice = JOptionPane.showConfirmDialog(this, message, "Spawn Zone Warnings",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        return choice == JOptionPane.OK_OPTION;
    }

    public static void openOnEdt() {
        SwingUtilities.invokeLater(() -> {
            MazeEditorWindow window = new MazeEditorWindow();
            window.setVisible(true);
        });
    }
}
