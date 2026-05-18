package editor;

import core.level.Maze;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.AlphaComposite;
import java.awt.image.BufferedImage;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.function.Consumer;

public class MazeEditorPanel extends JPanel {
    private MazeEditorDocument document;
    private MazeEditorTool tool = MazeEditorTool.PAINT;
    private Maze.Tile selectedTile = Maze.Tile.WALL;
    private int zoom = 2;
    private boolean showLabels = true;
    private boolean editingSpawnZoneLayer = false;
    private Point hoverCell;
    private Point dragStartCell;
    private Point dragCurrentCell;
    private Consumer<Maze.Tile> tilePickedListener;
    private Runnable changeListener;

    public MazeEditorPanel(MazeEditorDocument document) {
        this.document = document;
        setBackground(new Color(15, 17, 24));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point cell = toCell(e.getPoint());
                if (cell == null) {
                    return;
                }

                if (tool == MazeEditorTool.RECTANGLE) {
                    dragStartCell = cell;
                    dragCurrentCell = cell;
                    repaint();
                    return;
                }

                applyToolAt(cell, SwingUtilities.isRightMouseButton(e));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (tool == MazeEditorTool.RECTANGLE && dragStartCell != null && dragCurrentCell != null) {
                    document.beginEdit();
                    if (editingSpawnZoneLayer) {
                        document.drawSpawnZoneRectangle(
                                dragStartCell,
                                dragCurrentCell,
                                !SwingUtilities.isRightMouseButton(e),
                                e.isShiftDown()
                        );
                    } else {
                        document.drawRectangle(dragStartCell, dragCurrentCell,
                                SwingUtilities.isRightMouseButton(e) ? Maze.Tile.EMPTY : selectedTile,
                                e.isShiftDown());
                    }
                    dragStartCell = null;
                    dragCurrentCell = null;
                    refreshFromDocument();
                    notifyChanged();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverCell = null;
                repaint();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point cell = toCell(e.getPoint());
                if (cell == null) {
                    return;
                }
                hoverCell = cell;
                if (tool == MazeEditorTool.RECTANGLE) {
                    dragCurrentCell = cell;
                    repaint();
                    return;
                }
                if (tool == MazeEditorTool.PAINT || tool == MazeEditorTool.ERASE) {
                    applyToolAt(cell, SwingUtilities.isRightMouseButton(e) || tool == MazeEditorTool.ERASE);
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                hoverCell = toCell(e.getPoint());
                repaint();
            }
        });

        refreshFromDocument();
    }

    public void setDocument(MazeEditorDocument document) {
        this.document = document;
        refreshFromDocument();
    }

    public void setTool(MazeEditorTool tool) {
        this.tool = tool;
    }

    public void setSelectedTile(Maze.Tile selectedTile) {
        this.selectedTile = selectedTile;
    }

    public void setZoom(int zoom) {
        this.zoom = Math.max(1, zoom);
        refreshFromDocument();
    }

    public int getZoom() {
        return zoom;
    }

    public void setShowLabels(boolean showLabels) {
        this.showLabels = showLabels;
        repaint();
    }

    public void setEditingSpawnZoneLayer(boolean editingSpawnZoneLayer) {
        this.editingSpawnZoneLayer = editingSpawnZoneLayer;
        repaint();
    }

    public void setTilePickedListener(Consumer<Maze.Tile> tilePickedListener) {
        this.tilePickedListener = tilePickedListener;
    }

    public void setChangeListener(Runnable changeListener) {
        this.changeListener = changeListener;
    }

    public Point getHoverCell() {
        return hoverCell;
    }

    public void refreshFromDocument() {
        Maze maze = document.getMaze();
        setPreferredSize(new Dimension(maze.getWidthInTiles() * scaledTileSize(), maze.getHeightInTiles() * scaledTileSize()));
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Maze maze = document.getMaze();
        int cellSize = scaledTileSize();
        BufferedImage background = maze.getBackgroundImage();

        if (background != null) {
            g2.drawImage(background, 0, 0, maze.getWidthInTiles() * cellSize, maze.getHeightInTiles() * cellSize, null);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.72f));
        }

        for (int y = 0; y < maze.getHeightInTiles(); y++) {
            for (int x = 0; x < maze.getWidthInTiles(); x++) {
                Maze.Tile tile = maze.getTile(x, y);
                int drawX = x * cellSize;
                int drawY = y * cellSize;

                g2.setColor(tile.getEditorColor());
                g2.fillRect(drawX, drawY, cellSize, cellSize);

                g2.setColor(new Color(255, 255, 255, 28));
                g2.drawRect(drawX, drawY, cellSize, cellSize);

                if (showLabels && cellSize >= 18) {
                    g2.setColor(tile == Maze.Tile.WALL ? Color.WHITE : new Color(20, 20, 20));
                    g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, Math.max(10, cellSize / 2)));
                    g2.drawString(String.valueOf(tile.getSymbol()), drawX + cellSize / 3, drawY + (cellSize * 2 / 3));
                }

                if (maze.isSpawnZone(x, y)) {
                    g2.setColor(new Color(182, 120, 240, 140));
                    g2.fillRect(drawX, drawY, cellSize, cellSize);
                    g2.setColor(new Color(234, 218, 255, 210));
                    g2.drawRect(drawX + 1, drawY + 1, cellSize - 2, cellSize - 2);
                }
            }
        }

        g2.setComposite(AlphaComposite.SrcOver);

        if (dragStartCell != null && dragCurrentCell != null) {
            int minX = Math.min(dragStartCell.x, dragCurrentCell.x);
            int minY = Math.min(dragStartCell.y, dragCurrentCell.y);
            int maxX = Math.max(dragStartCell.x, dragCurrentCell.x);
            int maxY = Math.max(dragStartCell.y, dragCurrentCell.y);
            g2.setColor(new Color(255, 255, 255, 170));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRect(minX * cellSize, minY * cellSize, (maxX - minX + 1) * cellSize, (maxY - minY + 1) * cellSize);
        }

        if (hoverCell != null) {
            g2.setColor(new Color(255, 255, 255, 180));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRect(hoverCell.x * cellSize, hoverCell.y * cellSize, cellSize, cellSize);
        }
    }

    private void applyToolAt(Point cell, boolean useErase) {
        if (!document.getMaze().isInBounds(cell.x, cell.y)) {
            return;
        }

        switch (tool) {
            case PAINT, ERASE -> {
                document.beginEdit();
                if (editingSpawnZoneLayer) {
                    boolean inSpawnZone = !(useErase || tool == MazeEditorTool.ERASE);
                    document.setSpawnZone(cell.x, cell.y, inSpawnZone);
                } else {
                    Maze.Tile tile = useErase || tool == MazeEditorTool.ERASE ? Maze.Tile.EMPTY : selectedTile;
                    document.setTile(cell.x, cell.y, tile);
                }
                refreshFromDocument();
                notifyChanged();
            }
            case FILL -> {
                document.beginEdit();
                if (editingSpawnZoneLayer) {
                    document.floodFillSpawnZone(cell.x, cell.y, !useErase);
                } else {
                    document.floodFill(cell.x, cell.y, useErase ? Maze.Tile.EMPTY : selectedTile);
                }
                refreshFromDocument();
                notifyChanged();
            }
            case PICKER -> {
                if (!editingSpawnZoneLayer) {
                    Maze.Tile tile = document.getMaze().getTile(cell.x, cell.y);
                    selectedTile = tile;
                    if (tilePickedListener != null) {
                        tilePickedListener.accept(tile);
                    }
                }
            }
            default -> {
            }
        }
    }

    private void notifyChanged() {
        if (changeListener != null) {
            changeListener.run();
        }
    }

    private Point toCell(Point point) {
        Maze maze = document.getMaze();
        int cellSize = scaledTileSize();
        int x = point.x / cellSize;
        int y = point.y / cellSize;
        if (!maze.isInBounds(x, y)) {
            return null;
        }
        return new Point(x, y);
    }

    private int scaledTileSize() {
        return document.getMaze().getTileSize() * zoom;
    }
}
