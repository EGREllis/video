package net.ellise.sudoku;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;

public class Buckets {
    int[][] buckets;
    int xWidth;
    int yWidth;

    private Buckets(int[][] buckets, int xwidth, int ywidth) {
        this.buckets = buckets;
        this.xWidth = xwidth;
        this.yWidth = ywidth;
    }

    public int getXWidth() {
        return xWidth;
    }

    public int getYWidth() {
        return yWidth;
    }

    public int getNXBuckets() {
        return buckets[0].length;
    }

    public int getNYBuckets() {
        return buckets.length;
    }

    public int getBucket(int x, int y) {
        return buckets[y][x];
    }

    public String toString() {
        StringBuilder message = new StringBuilder();
        message.append("Modal matrix\n");
        for (int y = 0; y < buckets.length; y++) {
            message.append("|");
            for (int x = 0; x < buckets[0].length; x++) {
                message.append(String.format("%1$d ", buckets[y][x]));
            }
            message.append("|\n");
        }
        return message.toString();
    }

    public static Buckets createBucket(int xWidth, int yWidth, BufferedImage image) {
            Raster raster = image.getData();
            int nXBuckets = 1+ (raster.getWidth() - raster.getMinX()) / xWidth;
            int nYBuckets = 1+ (raster.getHeight() - raster.getMinY()) / yWidth;

            System.out.println(String.format("Modal matrix:\n\tWidth (%1$d wide, %2$d per bucket -> %3$d buckets)\n\tHeight (%4$d high, %5$d per bucket) -> %6$d buckets", raster.getWidth(), xWidth, nXBuckets, raster.getHeight(), yWidth, nYBuckets));

            // Initialise buckets with zero
            int[][] buckets = new int[nYBuckets][];
            for (int i = 0; i < nYBuckets; i++) {
                buckets[i] = new int[nXBuckets];
                for (int j = 0; j < nXBuckets; j++) {
                    buckets[i][j] = 0;
                }
            }

            int[] pixel = new int[4];
            for (int y = raster.getMinY(); y < raster.getHeight(); y++) {
                for (int x = raster.getMinX(); x < raster.getWidth(); x++) {
                    pixel = raster.getPixel(x, y, pixel);
                    if (pixel[0] != 0 || pixel[1] != 0 || pixel[2] != 0) {
                        int bucketx = (x - raster.getMinX()) / xWidth;
                        int buckety = (y - raster.getMinY()) / yWidth;
                        int temp = buckets[buckety][bucketx];
                        buckets[buckety][bucketx] = temp+1;
                    }
                }
            }
            return new Buckets(buckets, xWidth, yWidth);
    }
}
