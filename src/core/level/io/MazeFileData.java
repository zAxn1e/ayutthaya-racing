package core.level.io;

import core.level.Maze;

import java.io.File;

public record MazeFileData(Maze maze, MazeMetadata metadata, File sourceFile, String resolvedBackgroundPath) {
}
