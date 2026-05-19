package core.config;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.util.Enumeration;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

public final class GameFonts {
    private static final Font BASE_FONT = loadBaseFont();
    private static final Font FALLBACK_FONT = new Font("Tahoma", Font.PLAIN, 20);

    private GameFonts() {
    }

    public static Font base() {
        return BASE_FONT;
    }

    public static Font plain(float size) {
        return BASE_FONT.deriveFont(Font.PLAIN, size);
    }

    public static Font bold(float size) {
        return BASE_FONT.deriveFont(Font.BOLD, size);
    }

    public static Font italic(float size) {
        return BASE_FONT.deriveFont(Font.ITALIC, size);
    }

    public static Font fallbackPlain(float size) {
        return FALLBACK_FONT.deriveFont(Font.PLAIN, size);
    }

    public static Font fallbackBold(float size) {
        return FALLBACK_FONT.deriveFont(Font.BOLD, size);
    }

    public static Font fallbackItalic(float size) {
        return FALLBACK_FONT.deriveFont(Font.ITALIC, size);
    }

    public static Font forText(String text, int style, float size) {
        Font candidate = BASE_FONT.deriveFont(style, size);
        if (text != null && candidate.canDisplayUpTo(text) != -1) {
            return FALLBACK_FONT.deriveFont(style, size);
        }
        return candidate;
    }

    public static void installSwingDefaults() {
        FontUIResource uiFont = new FontUIResource(FALLBACK_FONT.deriveFont(Font.PLAIN, 18f));
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, uiFont);
            }
        }
    }

    private static Font loadBaseFont() {
        try {
            File fontFile = new File("font/Gamefont.ttf");
            Font font = Font.createFont(Font.TRUETYPE_FONT, fontFile);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font;
        } catch (Exception e) {
            return FALLBACK_FONT;
        }
    }
}
