import editor.MazeEditorWindow;
import game.GameLauncher;
import javax.swing.SwingUtilities;

import core.config.ProjectPaths;
import java.io.File;

public class Main {
    public static void main(String[] args) {
        if (args.length > 0) {
            if ("editor".equalsIgnoreCase(args[0])) {
                MazeEditorWindow.openOnEdt();
                return;
            }
            if ("game".equalsIgnoreCase(args[0])) {
                SwingUtilities.invokeLater(() -> GameLauncher.main(new String[0]));
                return;
            }
        }

        SwingUtilities.invokeLater(() -> menu.MainMenuScreen.main(args));
    }
}
