package game;

import core.data.LeaderboardUI;
import panels.GamePanel;

import javax.swing.JComponent;
import javax.swing.JFrame;

public class GameLauncher {
    private static final String GAME_TITLE = "อยุธยา พาซิ่ง!";

    public static void launchInFrame(JFrame frame) {
        if (frame == null) {
            throw new IllegalArgumentException("Frame must not be null");
        }

        stopPreviousGamePanel(frame);
        LeaderboardUI.close(frame);

        GamePanel gamePanel = new GamePanel();
        frame.setContentPane(gamePanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.revalidate();
        frame.repaint();
        gamePanel.startGameThread();
    }

    public static void main(String[] args) {
        JFrame window = new JFrame(GAME_TITLE);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);
        launchInFrame(window);
        window.setVisible(true);
    }

    private static void stopPreviousGamePanel(JFrame frame) {
        if (frame.getContentPane() instanceof GamePanel previousGamePanel) {
            previousGamePanel.stopGameThread();
            return;
        }
        if (frame.getContentPane() instanceof JComponent content && content.getComponentCount() == 1
                && content.getComponent(0) instanceof GamePanel previousEmbeddedGamePanel) {
            previousEmbeddedGamePanel.stopGameThread();
        }
    }
}
