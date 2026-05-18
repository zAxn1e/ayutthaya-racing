package core.data;

import core.config.ProjectPaths;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LeaderboardUI extends JFrame {
    private static final int FRAME_WIDTH = 1024;
    private static final int FRAME_HEIGHT = 768;

    private Image imgBackground;
    private Image imgGoldFrame;
    private Image imgCloseBtn;

    private Font fontTitle;
    private Font fontHeader;
    private Font fontContent;
    private final Rectangle closeBtnRect = new Rectangle(920, 55, 55, 55);

    private LeaderboardUI(Window owner) {
        setTitle("ทำเนียบยอดฝีมือ");
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(owner);
        setResizable(false);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        loadAssets();
        CustomPaintPanel paintPanel = new CustomPaintPanel();
        setupCloseButton(paintPanel);
        add(paintPanel);
    }

    private void loadAssets() {
        fontTitle = new Font("Tahoma", Font.BOLD, 40);
        fontHeader = new Font("Tahoma", Font.BOLD, 22);
        fontContent = new Font("Tahoma", Font.PLAIN, 20);
        imgBackground = loadImageIfExists(ProjectPaths.uiFilePath("bg_temple.png"));
        imgGoldFrame = loadImageIfExists(ProjectPaths.uiFilePath("frame_gold.png"));
        imgCloseBtn = loadImageIfExists(ProjectPaths.uiFilePath("btn_close.png"));
    }

    private Image loadImageIfExists(String path) {
        ImageIcon icon = new ImageIcon(path);
        return icon.getIconWidth() > 0 ? icon.getImage() : null;
    }

    private void setupCloseButton(JPanel panel) {
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (closeBtnRect.contains(e.getPoint())) {
                    dispose();
                }
            }
        });
    }

    public static void open(Window owner) {
        LeaderboardUI leaderboardUI = new LeaderboardUI(owner);
        leaderboardUI.setVisible(true);
    }

    private static String toThaiNumber(String num) {
        return num.replace('1', '๑').replace('2', '๒').replace('3', '๓')
                .replace('4', '๔').replace('5', '๕').replace('6', '๖')
                .replace('7', '๗').replace('8', '๘').replace('9', '๙').replace('0', '๐');
    }

    private class CustomPaintPanel extends JPanel {
        private final List<Object[]> topScores = readScores();

        private List<Object[]> readScores() {
            try {
                List<AppDatabase.ScoreEntry> rows = AppDatabase.getTopScores(10);
                List<Object[]> data = new ArrayList<>();
                int rank = 1;
                for (AppDatabase.ScoreEntry row : rows) {
                    data.add(new Object[]{
                            toThaiNumber(String.valueOf(rank++)),
                            row.username(),
                            toThaiNumber(String.format("%,d", row.score()))
                    });
                }
                return data;
            } catch (SQLException ex) {
                List<Object[]> error = new ArrayList<>();
                error.add(new Object[]{"-", "Database Error", ex.getMessage()});
                return error;
            }
        }

        CustomPaintPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            if (imgBackground != null) {
                g2d.drawImage(imgBackground, 0, 0, getWidth(), getHeight(), this);
            } else {
                g2d.setColor(new Color(10, 20, 32));
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }

            if (imgGoldFrame != null) {
                g2d.drawImage(imgGoldFrame, 100, 50, FRAME_WIDTH - 200, FRAME_HEIGHT - 100, this);
            } else {
                g2d.setColor(new Color(36, 46, 65, 230));
                g2d.fillRoundRect(100, 50, FRAME_WIDTH - 200, FRAME_HEIGHT - 100, 30, 30);
                g2d.setColor(new Color(255, 204, 0));
                g2d.drawRoundRect(100, 50, FRAME_WIDTH - 200, FRAME_HEIGHT - 100, 30, 30);
            }

            drawCenteredString(g2d, "ทำเนียบยอดฝีมือ", new Rectangle(100, 115, FRAME_WIDTH - 200, 80), fontTitle, Color.BLACK, 2);
            drawCenteredString(g2d, "ทำเนียบยอดฝีมือ", new Rectangle(100, 112, FRAME_WIDTH - 200, 80), fontTitle, new Color(255, 204, 0), 0);

            int startX = 250;
            int startY = 220;
            int colWidth = (FRAME_WIDTH - (startX * 2)) / 3;
            g2d.setFont(fontHeader);
            g2d.setColor(new Color(255, 215, 0));
            g2d.drawString("อันดับ", startX + 35, startY);
            g2d.drawString("ชื่อผู้เล่น", startX + colWidth + 50, startY);
            g2d.drawString("คะแนนสูงสุด", startX + (colWidth * 2) + 30, startY);

            g2d.setFont(fontContent);
            g2d.setColor(Color.WHITE);
            int y = startY + 48;
            int rowHeight = 44;
            for (Object[] row : topScores) {
                g2d.drawString(row[0].toString(), startX + 45, y);
                g2d.drawString(truncateToFit(g2d, row[1].toString(), colWidth - 30), startX + colWidth + 50, y);
                drawRightAligned(g2d, row[2].toString(), startX + (colWidth * 3) + 25, y);
                y += rowHeight;
                if (y > 680) {
                    break;
                }
            }

            if (imgCloseBtn != null) {
                g2d.drawImage(imgCloseBtn, closeBtnRect.x, closeBtnRect.y, closeBtnRect.width, closeBtnRect.height, this);
            } else {
                g2d.setColor(new Color(90, 32, 32));
                g2d.fillRoundRect(closeBtnRect.x, closeBtnRect.y, closeBtnRect.width, closeBtnRect.height, 12, 12);
                g2d.setColor(new Color(245, 225, 225));
                g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
                g2d.drawString("X", closeBtnRect.x + 18, closeBtnRect.y + 37);
            }
        }

        private void drawCenteredString(Graphics g, String text, Rectangle rect, Font font, Color color, int offset) {
            FontMetrics metrics = g.getFontMetrics(font);
            int x = rect.x + (rect.width - metrics.stringWidth(text)) / 2 + offset;
            int y = rect.y + ((rect.height - metrics.getHeight()) / 2) + metrics.getAscent() + offset;
            g.setFont(font);
            g.setColor(color);
            g.drawString(text, x, y);
        }

        private void drawRightAligned(Graphics2D g2d, String text, int rightX, int baselineY) {
            FontMetrics metrics = g2d.getFontMetrics();
            int x = rightX - metrics.stringWidth(text);
            g2d.drawString(text, x, baselineY);
        }

        private String truncateToFit(Graphics2D g2d, String text, int maxWidth) {
            FontMetrics metrics = g2d.getFontMetrics();
            if (metrics.stringWidth(text) <= maxWidth) {
                return text;
            }
            String ellipsis = "...";
            int limit = Math.max(0, text.length() - 1);
            while (limit > 0) {
                String candidate = text.substring(0, limit) + ellipsis;
                if (metrics.stringWidth(candidate) <= maxWidth) {
                    return candidate;
                }
                limit--;
            }
            return ellipsis;
        }
    }
}
