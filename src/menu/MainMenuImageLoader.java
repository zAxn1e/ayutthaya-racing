package menu;

import javax.swing.*;
import core.config.ProjectPaths;
import java.awt.*;
import java.awt.image.BufferedImage;

public class MainMenuImageLoader {
    private ImageIcon placeholder(String name, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setColor(new Color(200, 200, 200));
            g.fillRect(0, 0, w, h);
            g.setColor(Color.RED);
            g.setFont(new Font("SansSerif", Font.BOLD, Math.max(12, h/10)));
            String txt = "MISSING: " + name;
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(txt);
            g.drawString(txt, Math.max(10, (w - tw)/2), Math.max(20, h/2));
        } finally {
            g.dispose();
        }
        return new ImageIcon(img);
    }

    public void prepareImages(MainMenuState state) {
        System.out.println("MainMenuImageLoader: resourcesRoot=" + ProjectPaths.resourcesRoot());

        state.frames = new ImageIcon[22];
        for (int i = 0; i < 22; i++) {
            String fname = (i + 2) + ".png";
            String p = ProjectPaths.uiFilePath(fname);
            ImageIcon ic = new ImageIcon(p);
            if (ic.getIconWidth() <= 0) {
                System.out.println("MainMenuImageLoader: missing " + p + " — using placeholder");
                ic = placeholder(fname, 1024, 768);
            }
            state.frames[i] = ic;
        }

        state.loginFrames = new ImageIcon[7];
        for (int i = 0; i < 7; i++) {
            String fname = (i + 22) + ".png";
            String p = ProjectPaths.uiFilePath(fname);
            ImageIcon ic = new ImageIcon(p);
            if (ic.getIconWidth() <= 0) {
                System.out.println("MainMenuImageLoader: missing " + p + " — using placeholder");
                ic = placeholder(fname, 1024, 768);
            }
            state.loginFrames[i] = ic;
        }

        state.regisFrames = new ImageIcon[7];
        for (int i = 0; i < 7; i++) {
            String fname = (i + 29) + ".png";
            String p = ProjectPaths.uiFilePath(fname);
            ImageIcon ic = new ImageIcon(p);
            if (ic.getIconWidth() <= 0) {
                System.out.println("MainMenuImageLoader: missing " + p + " — using placeholder");
                ic = placeholder(fname, 1024, 768);
            }
            state.regisFrames[i] = ic;
        }

        {
            String fname = "popframe01.png";
            String p = ProjectPaths.uiFilePath(fname);
            ImageIcon ic = new ImageIcon(p);
            if (ic.getIconWidth() <= 0) {
                System.out.println("MainMenuImageLoader: missing " + p + " — using placeholder");
                ic = placeholder(fname, 400, 200);
            }
            state.popFrames = new ImageIcon[]{ic};
        }

        state.enterFrames = new ImageIcon[]{
                loadOrPlaceholder("36.png", 1024, 768),
                loadOrPlaceholder("37.png", 1024, 768)
        };

        state.gameFrames = new ImageIcon[]{loadOrPlaceholder("game1.png", 1024, 768)};

        state.howFrames = new ImageIcon[7];
        for (int i = 0; i < 7; i++) {
            String fname = (i + 40) + ".png";
            state.howFrames[i] = loadOrPlaceholder(fname, 1024, 768);
        }

        state.countdownFrames = new ImageIcon[4];
        for (int i = 0; i < 4; i++) {
            String fname = (i + 47) + ".png";
            state.countdownFrames[i] = loadOrPlaceholder(fname, 1024, 768);
        }

        System.out.println("MainMenuImageLoader: prepareImages done");
    }

    private ImageIcon loadOrPlaceholder(String fname, int w, int h) {
        String p = ProjectPaths.uiFilePath(fname);
        ImageIcon ic = new ImageIcon(p);
        if (ic.getIconWidth() <= 0) {
            System.out.println("MainMenuImageLoader: missing " + p + " — using placeholder");
            ic = placeholder(fname, w, h);
        }
        return ic;
    }
}
