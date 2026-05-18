package first_page;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class Data {
    public final JFrame appFrame;
    public JPanel mainPanel;
    public final JLabel label = new JLabel();
    public final JLabel regisBtn = new JLabel();
    public final JLabel loginBtn = new JLabel();
    public final JLabel enterBtn = new JLabel();
    public final JLabel skipBtn = new JLabel();
    public final JLabel startBtn = new JLabel();
    public final JLabel leaderBtn = new JLabel();
    public final JLabel lobbyBtn = new JLabel();
    public final JLabel showPassBtn = new JLabel();
    public final JLabel howBtn = new JLabel();
    public final JLabel backBtn = new JLabel();

    public final JTextField loginUserField = new JTextField();
    public final JPasswordField loginPassField = new JPasswordField();

    public Font customFont;

    public int currentFrame = 0;
    public boolean isRegisMode = false;
    public boolean isRunning = false;
    public boolean isEnterMode = false;
    public boolean isPassVisible = false;
    public boolean isHowMode = false;

    public Timer introTimer;
    public Timer loopTimer;
    public Timer countTimer;

    public ImageIcon[] frames;
    public ImageIcon[] loginFrames;
    public ImageIcon[] regisFrames;
    public ImageIcon[] enterFrames;
    public ImageIcon[] popFrames;
    public ImageIcon[] gameFrames;
    public ImageIcon[] howFrames;
    public ImageIcon[] countdownFrames;

    public String user;
    public String pass;

    public Data() {
        appFrame = new JFrame("อยุธยา พาซิ่ง!");
        appFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        appFrame.setResizable(false);
    }

    public void loadCustomFont() {
        try {
            File fontFile = new File("font/Gamefont.ttf");
            customFont = Font.createFont(Font.TRUETYPE_FONT, fontFile);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(customFont);
        } catch (Exception e) {
            customFont = new Font("Tahoma", Font.BOLD, 20);
        }
    }
}