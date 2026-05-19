package menu;

import core.data.AppDatabase;
import core.data.LeaderboardUI;
import game.GameLauncher;
import panels.GamePanel;

import javax.swing.*;
import java.awt.*;

public class MainMenuScreen {
    private static MainMenuScreen instance;

    private final MainMenuState state;
    private final MainMenuImageLoader imageLoader;
    private final MainMenuButtons buttons;

    private MainMenuScreen() {
        state = new MainMenuState();
        imageLoader = new MainMenuImageLoader();
        buttons = new MainMenuButtons(state);
    }

    public static void main(String[] args) {
        if (instance != null) {
            instance.focusWindow();
            return;
        }
        instance = new MainMenuScreen();
        instance.start();
    }

    public static void returnToMainMenu() {
        if (instance == null) {
            main(new String[0]);
            return;
        }
        SwingUtilities.invokeLater(instance::showMainMenu);
    }

    private void start() {
        try {
            AppDatabase.initialize();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize database", e);
        }
        System.out.println("MainMenuScreen: start() - loading font and images");
        state.loadCustomFont();
        imageLoader.prepareImages(state);
        System.out.println("MainMenuScreen: images loaded: frames=" + (state.frames == null ? 0 : state.frames.length) +
                " login=" + (state.loginFrames == null ? 0 : state.loginFrames.length) +
                " regis=" + (state.regisFrames == null ? 0 : state.regisFrames.length) +
                " countdown=" + (state.countdownFrames == null ? 0 : state.countdownFrames.length));
        setupFirstScreen();
        System.out.println("MainMenuScreen: setupFirstScreen done; UI should be visible");
    }

    private void setupFirstScreen() {
        buttons.setupRegisButton();
        buttons.setupLoginButton();
        buttons.setupEnterButton();
        buttons.setupSkipButton();
        buttons.setStartBtn();
        buttons.setupLeaderButton();
        buttons.setLobbyBtn();
        buttons.setHowBtn();
        buttons.setupShowPassBtn();
        setupUserBox();
        setupPassBox();
        buttons.setBackBtn();
        setupMainScreen();
        createTimers();
        state.introTimer.start();
    }

    private void createTimers() {
        state.loopTimer = new Timer(500, e -> {
            if (state.isHowMode) {
                state.label.setIcon(state.howFrames[state.currentFrame % state.howFrames.length]);
            } else if (state.isEnterMode) {
                state.label.setIcon(state.enterFrames[state.currentFrame % 2]);
            } else if (!state.isRegisMode) {
                state.label.setIcon(state.loginFrames[state.currentFrame % state.loginFrames.length]);
            } else {
                state.label.setIcon(state.regisFrames[state.currentFrame % state.regisFrames.length]);
            }
            state.currentFrame++;
        });

        state.countTimer = new Timer(1000, e -> {
            if (state.currentFrame < state.countdownFrames.length) {
                state.label.setIcon(state.countdownFrames[state.currentFrame]);
                state.currentFrame++;
            } else {
                launchGameFromCountdown();
            }
        });

        state.introTimer = new Timer(800, e -> {
            if (state.currentFrame < state.frames.length) {
                state.label.setIcon(state.frames[state.currentFrame]);
                state.currentFrame++;
                state.skipBtn.setVisible(true);
            } else {
                buttons.goToLogin();
            }
        });
    }

    private void launchGameFromCountdown() {
        state.countTimer.stop();
        if (state.loopTimer != null) {
            state.loopTimer.stop();
        }
        if (state.introTimer != null) {
            state.introTimer.stop();
        }
        GameLauncher.launchInFrame(state.appFrame);
    }

    private void setupUserBox() {
        state.loginUserField.setOpaque(false);
        state.loginUserField.setBorder(null);
        state.loginUserField.setFont(state.customFont.deriveFont(Font.PLAIN, 50f));
        state.loginUserField.setForeground(Color.BLACK);
        state.loginUserField.setCaretColor(Color.BLACK);
        state.loginUserField.setBounds(275, 355, 500, 90);
        state.loginUserField.setVisible(false);
        buttons.addLimit(state.loginUserField, 10);
    }

    private void setupPassBox() {
        state.loginPassField.setOpaque(false);
        state.loginPassField.setBorder(null);
        state.loginPassField.setForeground(Color.GRAY);
        state.loginPassField.setCaretColor(Color.GRAY);
        state.loginPassField.setFont(state.customFont.deriveFont(Font.PLAIN, 50f));
        state.loginPassField.setBounds(495, 450, 500, 90);
        state.loginPassField.setEchoChar('●');
        state.loginPassField.setVisible(false);
    }

    private void setupMainScreen() {
        JPanel panel = new JPanel(null);
        panel.setPreferredSize(new Dimension(1024, 768));

        panel.add(state.skipBtn);
        panel.add(state.regisBtn);
        panel.add(state.loginBtn);
        panel.add(state.enterBtn);
        panel.add(state.startBtn);
        panel.add(state.leaderBtn);
        panel.add(state.lobbyBtn);
        panel.add(state.showPassBtn);
        panel.add(state.loginUserField);
        panel.add(state.loginPassField);
        panel.add(state.backBtn);
        panel.add(state.howBtn);

        state.label.setBounds(0, 0, 1024, 768);
        panel.add(state.label);

        state.mainPanel = panel;
        LeaderboardUI.close(state.appFrame);
        state.appFrame.setContentPane(panel);
        state.appFrame.pack();
        state.appFrame.setLocationRelativeTo(null);
        state.appFrame.setVisible(true);
    }

    private void focusWindow() {
        state.appFrame.toFront();
        state.appFrame.requestFocus();
    }

    private void showMainMenu() {
        stopActiveGamePanel();
        if (state.mainPanel == null) {
            setupMainScreen();
        } else {
            LeaderboardUI.close(state.appFrame);
            state.appFrame.setContentPane(state.mainPanel);
        }
        buttons.goToEnter();
        state.appFrame.revalidate();
        state.appFrame.repaint();
        focusWindow();
    }

    private void stopActiveGamePanel() {
        if (state.appFrame.getContentPane() instanceof GamePanel gamePanel) {
            gamePanel.stopGameThread();
        }
    }
}
