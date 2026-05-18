import editor.MazeEditorWindow;
import game.GameLauncher;
import javax.swing.SwingUtilities;

import core.config.ProjectPaths;
import java.io.File;

public class Main {
    public static void main(String[] args) {
        // Pre-flight diagnostic to help debug run_merged white-screen issues
        try {
            File resRoot = ProjectPaths.resourcesRoot();
            System.out.println("Startup diagnostics:");
            System.out.println("  resourcesRoot=" + (resRoot==null?"<null>":resRoot.getAbsolutePath()));
            System.out.println("  base.png exists=" + (resRoot!=null && new File(resRoot, "map/base.png").exists()));
            System.out.println("  active_maze_path.txt exists=" + (resRoot!=null && new File(resRoot, "maze/active_maze_path.txt").exists()));
            System.out.println("  sprites/thief exists=" + (resRoot!=null && new File(resRoot, "sprites/thief").exists()));
        } catch (Throwable t) {
            System.out.println("Startup diagnostics failed: " + t);
        }

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
