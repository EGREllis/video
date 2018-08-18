package net.ellise.sudoku;

import java.awt.image.BufferedImage;

public class Filtered {
    private BufferedImage image;
    private int present;

    public Filtered(BufferedImage image, int present) {
        this.image = image;
        this.present = present;
    }

    public BufferedImage getImage() {
        return image;
    }

    public int getPresent() {
        return present;
    }
}
