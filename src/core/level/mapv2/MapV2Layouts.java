package core.level.mapv2;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public final class MapV2Layouts {
    public static final int SOURCE_WIDTH = 1024;
    public static final int SOURCE_HEIGHT = 768;

    private MapV2Layouts() {
    }

    public static List<Rectangle> map1Walls() {
        List<Rectangle> walls = new ArrayList<>();

        // Outer border
        walls.add(new Rectangle(220, 215, 280, 20));
        walls.add(new Rectangle(540, 215, 280, 20));
        walls.add(new Rectangle(220, 215, 10, 345));
        walls.add(new Rectangle(220, 540, 55, 20));
        walls.add(new Rectangle(265, 540, 10, 55));
        walls.add(new Rectangle(810, 215, 10, 345));
        walls.add(new Rectangle(765, 540, 55, 20));
        walls.add(new Rectangle(765, 540, 10, 55));
        walls.add(new Rectangle(265, 595, 235, 20));
        walls.add(new Rectangle(540, 595, 235, 20));

        // Inner border - horizontal
        walls.add(new Rectangle(275, 260, 50, 20));
        walls.add(new Rectangle(370, 260, 80, 20));
        walls.add(new Rectangle(540, 260, 60, 20));
        walls.add(new Rectangle(645, 260, 125, 20));
        walls.add(new Rectangle(315, 300, 95, 20));
        walls.add(new Rectangle(490, 305, 120, 20));
        walls.add(new Rectangle(645, 305, 90, 20));
        walls.add(new Rectangle(265, 335, 110, 20));
        walls.add(new Rectangle(540, 355, 70, 20));
        walls.add(new Rectangle(700, 355, 70, 20));
        walls.add(new Rectangle(360, 380, 60, 20));
        walls.add(new Rectangle(590, 400, 135, 20));
        walls.add(new Rectangle(360, 465, 55, 20));
        walls.add(new Rectangle(220, 490, 55, 20));
        walls.add(new Rectangle(360, 500, 90, 20));
        walls.add(new Rectangle(370, 545, 120, 20));
        walls.add(new Rectangle(590, 545, 70, 20));
        walls.add(new Rectangle(590, 445, 180, 20));
        walls.add(new Rectangle(720, 495, 45, 20));

        // Inner border - vertical
        walls.add(new Rectangle(315, 260, 10, 60));
        walls.add(new Rectangle(440, 260, 10, 45));
        walls.add(new Rectangle(490, 215, 10, 110));
        walls.add(new Rectangle(540, 260, 10, 65));
        walls.add(new Rectangle(265, 310, 10, 135));
        walls.add(new Rectangle(600, 305, 10, 70));
        walls.add(new Rectangle(725, 305, 10, 70));
        walls.add(new Rectangle(760, 305, 10, 70));
        walls.add(new Rectangle(410, 335, 10, 70));
        walls.add(new Rectangle(440, 335, 10, 55));
        walls.add(new Rectangle(490, 355, 10, 155));
        walls.add(new Rectangle(540, 355, 10, 60));
        walls.add(new Rectangle(645, 355, 10, 65));
        walls.add(new Rectangle(315, 385, 10, 180));
        walls.add(new Rectangle(360, 380, 10, 50));
        walls.add(new Rectangle(760, 400, 10, 65));
        walls.add(new Rectangle(360, 460, 10, 60));
        walls.add(new Rectangle(440, 425, 10, 90));
        walls.add(new Rectangle(540, 450, 10, 165));
        walls.add(new Rectangle(590, 500, 10, 65));
        walls.add(new Rectangle(655, 450, 10, 65));
        walls.add(new Rectangle(720, 495, 10, 70));

        return walls;
    }

    public static List<Rectangle> map2Walls() {
        List<Rectangle> walls = new ArrayList<>();

        // Outer border
        walls.add(new Rectangle(220, 215, 280, 20));
        walls.add(new Rectangle(540, 215, 280, 20));
        walls.add(new Rectangle(220, 215, 10, 345));
        walls.add(new Rectangle(220, 540, 55, 20));
        walls.add(new Rectangle(265, 540, 10, 55));
        walls.add(new Rectangle(810, 215, 10, 345));
        walls.add(new Rectangle(765, 540, 55, 20));
        walls.add(new Rectangle(765, 540, 10, 55));
        walls.add(new Rectangle(265, 595, 235, 20));
        walls.add(new Rectangle(540, 595, 235, 20));

        // Inner border - horizontal
        walls.add(new Rectangle(220, 260, 55, 20));
        walls.add(new Rectangle(370, 260, 80, 20));
        walls.add(new Rectangle(540, 260, 60, 20));
        walls.add(new Rectangle(645, 260, 125, 20));
        walls.add(new Rectangle(315, 300, 95, 20));
        walls.add(new Rectangle(490, 305, 120, 20));
        walls.add(new Rectangle(645, 305, 90, 20));
        walls.add(new Rectangle(265, 335, 110, 20));
        walls.add(new Rectangle(440, 355, 50, 20));
        walls.add(new Rectangle(540, 355, 70, 20));
        walls.add(new Rectangle(700, 355, 70, 20));
        walls.add(new Rectangle(360, 380, 60, 20));
        walls.add(new Rectangle(590, 400, 135, 20));
        walls.add(new Rectangle(360, 465, 55, 20));
        walls.add(new Rectangle(220, 490, 55, 20));
        walls.add(new Rectangle(360, 500, 90, 20));
        walls.add(new Rectangle(370, 545, 120, 20));
        walls.add(new Rectangle(590, 545, 70, 20));
        walls.add(new Rectangle(590, 445, 180, 20));
        walls.add(new Rectangle(720, 495, 45, 20));

        // Inner border - vertical
        walls.add(new Rectangle(315, 260, 10, 60));
        walls.add(new Rectangle(440, 260, 10, 45));
        walls.add(new Rectangle(490, 215, 10, 110));
        walls.add(new Rectangle(540, 215, 10, 45));
        walls.add(new Rectangle(265, 310, 10, 135));
        walls.add(new Rectangle(600, 305, 10, 70));
        walls.add(new Rectangle(760, 260, 10, 45));
        walls.add(new Rectangle(760, 305, 10, 70));
        walls.add(new Rectangle(410, 335, 10, 70));
        walls.add(new Rectangle(440, 335, 10, 55));
        walls.add(new Rectangle(490, 355, 10, 155));
        walls.add(new Rectangle(540, 355, 10, 60));
        walls.add(new Rectangle(645, 355, 10, 65));
        walls.add(new Rectangle(315, 385, 10, 180));
        walls.add(new Rectangle(360, 380, 10, 50));
        walls.add(new Rectangle(760, 445, 50, 20));
        walls.add(new Rectangle(360, 460, 10, 60));
        walls.add(new Rectangle(440, 425, 10, 90));
        walls.add(new Rectangle(540, 450, 10, 165));
        walls.add(new Rectangle(590, 500, 10, 65));
        walls.add(new Rectangle(655, 450, 10, 65));
        walls.add(new Rectangle(720, 495, 10, 70));

        return walls;
    }
}
