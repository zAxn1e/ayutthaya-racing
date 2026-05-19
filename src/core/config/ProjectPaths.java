package core.config;

import java.io.File;
import java.io.IOException;

public final class ProjectPaths {
    private static final File RESOURCES_ROOT = locateResourcesRoot();

    private static final String MAZE_DIR_NAME = "maze";
    private static final String MAP_DIR_NAME = "map";
    private static final String SPRITES_DIR_NAME = "sprites";
    private static final String THIEF_SPRITES_DIR_NAME = "thief";
    private static final String UI_DIR_NAME = "ui";
    private static final String IMAGES_DIR = "src\\images";

    private ProjectPaths() {
    }

    private static File locateResourcesRoot() {
        // Prefer using where the classes are loaded from — this typically points to
        // merged_final/out
        try {
            File codeLoc = new File(ProjectPaths.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File maybeProjectRoot = codeLoc.getParentFile() != null ? codeLoc.getParentFile() : null; // one level up
                                                                                                      // (out ->
                                                                                                      // project)
            if (maybeProjectRoot != null) {
                File candidate = new File(maybeProjectRoot, "resources");
                if (candidate.exists() && candidate.isDirectory()) {
                    try {
                        System.out.println("ProjectPaths: detected resources at " + candidate.getCanonicalPath());
                    } catch (IOException ignored) {
                    }
                    return candidate.getCanonicalFile();
                }
            }
        } catch (Exception e) {
            System.out.println("ProjectPaths: code location detection failed: " + e.getMessage());
        }

        File cur = new File(System.getProperty("user.dir"));
        for (int i = 0; i < 6; i++) {
            File candidate = new File(cur, "resources");
            if (candidate.exists() && candidate.isDirectory()) {
                try {
                    return candidate.getCanonicalFile();
                } catch (IOException ignored) {
                    return candidate;
                }
            }
            // also check common sibling folder (when running from workspace root)
            File sibling = new File(cur, "merged_final\\resources");
            if (sibling.exists() && sibling.isDirectory()) {
                try {
                    return sibling.getCanonicalFile();
                } catch (IOException ignored) {
                    return sibling;
                }
            }
            cur = cur.getParentFile();
            if (cur == null)
                break;
        }
        return new File("resources");
    }

    public static File mazeDirectory() {
        return new File(RESOURCES_ROOT, MAZE_DIR_NAME);
    }

    public static File mapDirectory() {
        return new File(RESOURCES_ROOT, MAP_DIR_NAME);
    }

    public static File spriteThiefDirectory() {
        return new File(new File(RESOURCES_ROOT, SPRITES_DIR_NAME), THIEF_SPRITES_DIR_NAME);
    }

    public static File imageDirectory() {
        return new File(IMAGES_DIR);
    }

    public static File uiDirectory() {
        return new File(RESOURCES_ROOT, UI_DIR_NAME);
    }

    public static String mazeFilePath(String fileName) {
        return new File(mazeDirectory(), fileName).getPath();
    }

    public static String mazeShuffleConfigPath() {
        return mazeFilePath("shuffle_config.txt");
    }

    public static String mapFilePath(String fileName) {
        return new File(mapDirectory(), fileName).getPath();
    }

    public static String imageFilePath(String fileName) {
        return new File(imageDirectory(), fileName).getPath();
    }

    public static String uiFilePath(String fileName) {
        return new File(uiDirectory(), fileName).getPath();
    }

    public static File resourcesRoot() {
        return RESOURCES_ROOT;
    }
}
