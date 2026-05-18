package first_page;

import core.data.AppDatabase;
import core.data.LeaderboardUI;
import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import core.config.ProjectPaths;

public class BTN {
    private final Data state;

    public BTN(Data state) {
        this.state = state;
    }

    /**
     * Hide ALL interactive buttons/fields at once.
     * Every state transition calls this first, then selectively shows
     * only the components needed — prevents stale buttons lingering.
     */
    private void hideAll() {
        setVisible(false,
                state.skipBtn, state.regisBtn, state.loginBtn, state.enterBtn,
                state.startBtn, state.leaderBtn, state.lobbyBtn, state.showPassBtn,
                state.howBtn, state.backBtn, state.loginUserField, state.loginPassField
        );
    }

    private static void setVisible(boolean visible, JComponent... components) {
        for (JComponent component : components) {
            component.setVisible(visible);
        }
    }

    private void clearLoginFields() {
        state.loginUserField.setText("");
        state.loginPassField.setText("");
    }

    private void showLoginError() {
        hideAll();
        if (state.loopTimer != null) {
            state.loopTimer.stop();
        }

        state.label.setIcon(state.popFrames[0]);
        Timer errorTimer = new Timer(2500, e1 -> goToLogin());
        errorTimer.setRepeats(false);
        errorTimer.start();
    }

    void setupShowPassBtn() {
        ImageIcon showIcon = new ImageIcon(ProjectPaths.uiFilePath("showpass.png"));
        ImageIcon hideIcon = new ImageIcon(ProjectPaths.uiFilePath("hidepass.png"));

        state.showPassBtn.setIcon(showIcon);
        state.showPassBtn.setBounds(800, 340, showIcon.getIconWidth(), showIcon.getIconHeight());
        state.showPassBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        state.showPassBtn.setVisible(false);

        state.showPassBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                state.isPassVisible = !state.isPassVisible;
                if (state.isPassVisible) {
                    state.loginPassField.setEchoChar((char) 0);
                    state.showPassBtn.setIcon(hideIcon);
                    state.showPassBtn.setSize(hideIcon.getIconWidth(), hideIcon.getIconHeight());
                } else {
                    state.loginPassField.setEchoChar('●');
                    state.showPassBtn.setIcon(showIcon);
                    state.showPassBtn.setSize(showIcon.getIconWidth(), showIcon.getIconHeight());
                }
            }
        });
    }

    void addLimit(JTextField field, int limit) {
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if ((fb.getDocument().getLength() + text.length() - length) <= limit) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        });
    }

    public void goToEnter() {
        state.isHowMode = false;
        state.isEnterMode = true;
        state.isRunning = true;
        state.currentFrame = 0;
        if (state.enterFrames != null && state.enterFrames.length > 0) {
            state.label.setIcon(state.enterFrames[0]);
        }
        if (state.loopTimer != null) {
            state.loopTimer.setDelay(500);
            if (!state.loopTimer.isRunning()) {
                state.loopTimer.start();
            }
        }

        // Hide everything first, then show only what's needed
        hideAll();
        setVisible(true, state.startBtn, state.leaderBtn, state.howBtn, state.lobbyBtn);
    }

    public void goToLogin() {
        if (state.introTimer != null) {
            state.introTimer.stop();
        }
        state.currentFrame = 0;
        state.isRegisMode = false;
        state.isEnterMode = false;
        state.isRunning = true;
        state.isHowMode = false;
        state.label.setIcon(state.loginFrames[0]);

        // Hide everything first, then show only what's needed
        hideAll();
        setVisible(true, state.regisBtn, state.enterBtn, state.loginUserField, state.loginPassField, state.showPassBtn);

        state.loopTimer.setInitialDelay(500);
        state.loopTimer.start();
    }

    public void goToHow() {
        state.isHowMode = true;
        state.isEnterMode = false;
        state.isRunning = true;
        state.currentFrame = 0;

        if (state.loopTimer != null) {
            state.loopTimer.setDelay(2500);
        }

        // Hide everything first, then show only what's needed
        hideAll();
        state.backBtn.setVisible(true);
    }

    public void setupEnterButton() {
        ImageIcon enterIcon = new ImageIcon(ProjectPaths.uiFilePath("enter_first.png"));
        state.enterBtn.setIcon(enterIcon);
        state.enterBtn.setBounds(647, 551, enterIcon.getIconWidth(), enterIcon.getIconHeight());
        state.enterBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        state.enterBtn.setVisible(false);
        state.enterBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                state.user = state.loginUserField.getText().trim();
                state.pass = new String(state.loginPassField.getPassword());

                if (state.user.isEmpty() || state.pass.isEmpty()) {
                    showLoginError();
                    return;
                }

                try {
                    boolean success;
                    if (state.isRegisMode) {
                        success = AppDatabase.registerUser(state.user, state.pass);
                        if (!success) {
                            JOptionPane.showMessageDialog(state.appFrame, "Username นี้ถูกใช้แล้ว", "Register Failed", JOptionPane.WARNING_MESSAGE);
                        }
                    } else {
                        success = AppDatabase.authenticate(state.user, state.pass);
                        if (!success) {
                            JOptionPane.showMessageDialog(state.appFrame, "Username หรือ Password ไม่ถูกต้อง", "Login Failed", JOptionPane.WARNING_MESSAGE);
                        }
                    }

                    if (!success) {
                        showLoginError();
                        return;
                    }

                    SessionContext.setCurrentUser(state.user);
                    goToEnter();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(state.appFrame, "ไม่สามารถเชื่อมต่อฐานข้อมูลได้:\n" + ex.getMessage(),
                            "Database Error", JOptionPane.ERROR_MESSAGE);
                    showLoginError();
                }
            }
        });
    }

    public void setBackBtn() {
        ImageIcon backIcon = new ImageIcon(ProjectPaths.uiFilePath("exitbtn.png"));
        state.backBtn.setIcon(backIcon);
        state.backBtn.setBounds(51, 691, backIcon.getIconWidth(), backIcon.getIconHeight());
        state.backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        state.backBtn.setVisible(false);
        state.backBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                goToEnter();
            }
        });
    }

    public void setHowBtn() {
        ImageIcon howIcon = new ImageIcon(ProjectPaths.uiFilePath("howbtn.png"));
        state.howBtn.setIcon(howIcon);
        state.howBtn.setBounds(788, 691, howIcon.getIconWidth(), howIcon.getIconHeight());
        state.howBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        state.howBtn.setVisible(false);
        state.howBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                goToHow();
            }
        });
    }

    public void setLobbyBtn() {
        ImageIcon lobbyIcon = new ImageIcon(ProjectPaths.uiFilePath("lobbybtn.png"));
        state.lobbyBtn.setIcon(lobbyIcon);
        state.lobbyBtn.setBounds(51, 691, lobbyIcon.getIconWidth(), lobbyIcon.getIconHeight());
        state.lobbyBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        state.lobbyBtn.setVisible(false);
        state.lobbyBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                SessionContext.setCurrentUser("Guest");
                clearLoginFields();
                goToLogin();
            }
        });
    }

    public void setStartBtn() {
        ImageIcon startIcon = new ImageIcon(ProjectPaths.uiFilePath("startbtn.png"));
        state.startBtn.setIcon(startIcon);
        state.startBtn.setBounds(340, 485, startIcon.getIconWidth(), startIcon.getIconHeight());
        state.startBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        state.startBtn.setVisible(false);
        state.startBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                state.isEnterMode = false;
                state.loopTimer.stop();
                state.currentFrame = 0;
                hideAll();
                state.countTimer.start();
            }
        });
    }

    public void setupSkipButton() {
        ImageIcon skipIcon = new ImageIcon(ProjectPaths.uiFilePath("skipbtn.png"));
        state.skipBtn.setIcon(skipIcon);
        state.skipBtn.setBounds(650, 600, skipIcon.getIconWidth(), skipIcon.getIconHeight());
        state.skipBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        state.skipBtn.setVisible(false);
        state.skipBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                goToLogin();
            }
        });
    }

    public void setupLeaderButton() {
        ImageIcon leaderIcon = new ImageIcon(ProjectPaths.uiFilePath("leaderbtn.png"));
        state.leaderBtn.setIcon(leaderIcon);
        state.leaderBtn.setBounds(200, 250, leaderIcon.getIconWidth(), leaderIcon.getIconHeight());
        state.leaderBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        state.leaderBtn.setVisible(false);
        state.leaderBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                LeaderboardUI.open(state.appFrame);
            }
        });
    }

    public void setupRegisButton() {
        ImageIcon regisIcon = new ImageIcon(ProjectPaths.uiFilePath("regisbtn.png"));
        state.regisBtn.setIcon(regisIcon);
        state.regisBtn.setBounds(50, 530, regisIcon.getIconWidth(), regisIcon.getIconHeight());
        state.regisBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        state.regisBtn.setVisible(false);
        state.regisBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                state.isRegisMode = true;
                state.currentFrame = 0;
                state.label.setIcon(state.regisFrames[0]);
                state.regisBtn.setVisible(false);
                state.loginBtn.setVisible(true);
                clearLoginFields();
            }
        });
    }

    public void setupLoginButton() {
        ImageIcon loginIcon = new ImageIcon(ProjectPaths.uiFilePath("loginbtn.png"));
        state.loginBtn.setIcon(loginIcon);
        state.loginBtn.setBounds(50, 530, loginIcon.getIconWidth(), loginIcon.getIconHeight());
        state.loginBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        state.loginBtn.setVisible(false);
        state.loginBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                state.isRegisMode = false;
                state.currentFrame = 0;
                state.label.setIcon(state.loginFrames[0]);
                state.loginBtn.setVisible(false);
                state.regisBtn.setVisible(true);
                state.loginUserField.setVisible(true);
                state.loginPassField.setVisible(true);
                state.showPassBtn.setVisible(true);
                clearLoginFields();
            }
        });
    }
}
