package editor;

public enum MazeEditorTool {
    PAINT("Paint"),
    ERASE("Erase"),
    FILL("Fill"),
    RECTANGLE("Rectangle"),
    PICKER("Picker");

    private final String displayName;

    MazeEditorTool(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
