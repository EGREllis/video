package net.ellise.sudoku.image;

import net.ellise.sudoku.model.Filtered;
import net.ellise.sudoku.model.UnionFind;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

public class ImageProcessor {
    public Filtered getTextureFilteredImage(BufferedImage image, int barrier) {
        Raster raster = image.getData();

        WritableRaster output = raster.createCompatibleWritableRaster();
        int[] pixel = new int[4];
        int[] left = new int[4];
        int[] right = new int[4];
        int[] up = new int[4];
        int[] down = new int[4];

        int filtered = 0;
        for (int y = raster.getMinY(); y < raster.getHeight(); y++) {
            for (int x = raster.getMinX(); x < raster.getWidth(); x++) {
                pixel = raster.getPixel(x, y, pixel);

                int contrast = 0;
                if (x > raster.getMinX()) {
                    left = raster.getPixel(x-1, y, left);
                    contrast += diffPixel(pixel, left);
                }
                if (x < raster.getWidth()-1) {
                    right = raster.getPixel(x+1, y, right);
                    contrast += diffPixel(pixel, right);
                }
                if (y > raster.getMinY()) {
                    up = raster.getPixel(x, y-1, up);
                    contrast += diffPixel(pixel, up);
                }
                if (y < raster.getHeight()-1) {
                    down = raster.getPixel(x, y+1, down);
                    contrast += diffPixel(pixel, down);
                }

                if (contrast > barrier) {
                    output.setPixel(x, y, pixel);
                    filtered++;
                }
            }
        }

        return new Filtered(new BufferedImage(image.getColorModel(), output, true, new Hashtable()), filtered);
    }

    private static int diffPixel(int[] pixel, int[] other) {
        int result = 0;
        for (int i = 0; i < 4; i++) {
            result += (int)Math.pow(pixel[i]-other[i],2);
        }
        return result;
    }

    public Filtered applyRegionFilter(BufferedImage image, UnionFind regions, int region) {
        Raster raster = image.getData();
        WritableRaster output = raster.createCompatibleWritableRaster();

        int filtered = 0;
        int[] pixel = new int[4];
        for (int y = raster.getMinY(); y < raster.getHeight(); y++) {
            for (int x = raster.getMinX(); x < raster.getWidth(); x++) {
                if (region == regions.getRegion(x, y)) {
                    pixel = raster.getPixel(x, y, pixel);
                    output.setPixel(x, y, pixel);
                    filtered++;
                }
            }
        }

        return new Filtered(new BufferedImage(image.getColorModel(), output, true, new Hashtable<>()), filtered);
    }

    public Filtered applyRegionsFilter(BufferedImage image, UnionFind regions, Set<Integer> regionIds) {
        Raster raster = image.getData();
        WritableRaster output = raster.createCompatibleWritableRaster();

        int filtered = 0;
        int[] pixel = new int[4];
        for (int y = raster.getMinY(); y < raster.getHeight(); y++) {
            for (int x = raster.getMinX(); x < raster.getWidth(); x++) {
                for (int regionId : regionIds) {
                    if (regionId == regions.getRegion(x, y)) {
                        pixel = raster.getPixel(x, y, pixel);
                        output.setPixel(x, y, pixel);
                        filtered++;
                        break;
                    }
                }
            }
        }

        return new Filtered(new BufferedImage(image.getColorModel(), output, true, new Hashtable<>()), filtered);
    }

    public UnionFind determineRegions(BufferedImage image, int neighbourRange) {
        Raster raster = image.getData();
        UnionFind result = UnionFind.regionFor(image);
        int[] pixel = new int[4];
        int[] other = new int[4];
        for (int y = raster.getMinY(); y < raster.getHeight(); y++) {
            for (int x = raster.getMinX(); x < raster.getWidth(); x++) {
                pixel = raster.getPixel(x, y, pixel);
                if (!isNotBlack(pixel)) {
                    for (Point point : getNeighbouringPoints(x, y, raster, neighbourRange)) {
                        other = raster.getPixel(point.x, point.y, other);
                        if (!isNotBlack(other)) {
                            result.connect(x, y, point.x, point.y);
                        }
                    }
                }
            }
        }
        result.normalise();
        return result;
    }

    private Set<Point> getNeighbouringPoints(int x, int y, Raster raster, int range) {
        Set<Point> result = new HashSet<>();
        Set<Point> directions = new HashSet<>();
        for (int[] dir : new int[][]{{1, 1}, {1, 0}, {1, -1}, {0, 1}, {0, -1}, {-1, 1}, {-1, 0}, {-1,-1}}) {
            directions.add(new Point(dir[0], dir[1]));
        }

        Set<Point> currentRing = new HashSet<>();
        Set<Point> previousRing = new HashSet<>(directions);
        for (int i  = 0; i < range; i++) {
            currentRing = new HashSet<>();
            for (Point prev : previousRing) {
                for (Point dir : directions) {
                    currentRing.add(new Point(prev.x+dir.x, prev.y+dir.y));
                }
            }
            currentRing.remove(new Point(0,0));
            previousRing = currentRing;
        }

        for (Point dir : currentRing) {
            Point point = new Point(x+dir.x, y+dir.y);
            if (    point.x >= raster.getMinX() &&
                    point.x < raster.getWidth() &&
                    point.y >= raster.getMinY() &&
                    point.y < raster.getHeight()) {
                result.add(point);
            }
        }
        return result;
    }

    private boolean isNotBlack(int[] pixel) {
        boolean result = false;
        for (int i = 0; i < 3; i++) {
            if (Math.abs(pixel[i]) < 30) {
                result = true;
                break;
            }
        }
        return result;
    }

    public BufferedImage copyArea(BufferedImage image, Rectangle from, Rectangle to) {
        Raster data = image.getData();
        WritableRaster raster = data.createCompatibleWritableRaster();
        /*
        int[] pixel = new int[4];
        for (int y = 0; y <= from.getHeight(); y++) {
            for (int x = 0; x <= from.getWidth(); x++) {
                data.getPixel(from.x + x, from.y + y, pixel);
                raster.setPixel(x + to.x, y + to.x, pixel);
            }
        }
        */

        copyAreaInvert(data, raster, from, to);
        //Rectangle next = new Rectangle(to.width+1, 0, to.width, to.height);
        /*
        copyAreaBrightest(data, raster, from, next);

        Rectangle second = new Rectangle( to.width*2 +2, 0, to.width, to.height);
        copyAreaNotBlack(data, raster, from, second);

        Rectangle third = new Rectangle(to.width*3+3, 0, to.width, to.height);
        copyAreaAndFill(data, raster, from, third);
        */

        return new BufferedImage(image.getColorModel(), raster, image.isAlphaPremultiplied(), new Hashtable<>());
    }

    private long getBrightness(int[] pixel) {
        long sum = 0;
        for (int i = 0; i < 3; i++) {
            sum += pixel[i];
        }
        return sum;
    }

    private int[] getBrightestPixel(Raster data, Rectangle from) {
        int[] pixel = new int[4];
        int[] brightest = null;
        long bright = 0;

        for (int y = 0; y <= from.getHeight(); y++) {
            for (int x = 0; x <= from.getWidth(); x++) {
                data.getPixel(x + from.x, y + from.y, pixel);
                long currentBright = getBrightness(pixel);
                if (y == 0 && x == 0) {
                    bright = currentBright;
                    brightest = Arrays.copyOf(pixel, 4);
                } else if (bright < currentBright) {
                    bright = currentBright;
                    brightest = Arrays.copyOf(pixel, 4);
                }
            }
        }
        return brightest;
    }

    private void copyAreaBrightest(Raster data, WritableRaster raster, Rectangle from, Rectangle to) {
        int[] pixel = new int[4];
        int[] brightest = getBrightestPixel(data, from);

        for (int y = 0; y <= from.height; y++) {
            for (int x = 0; x <= from.width; x++) {
                data.getPixel(x + from.x, y + from.y, pixel);
                raster.setPixel(x + to.x, y + to.y, brightest);
            }
        }
    }

    private void copyAreaInvert(Raster data, WritableRaster raster, Rectangle from, Rectangle to) {
        int[] pixel = new int[4];
        int[] brightest = getBrightestPixel(data, from);

        for (int y = 0; y <= from.height; y++) {
            for (int x = 0; x <= from.width; x++) {
                data.getPixel(x + from.x, y + from.y, pixel);
                if (!isNotBlack(pixel)) {
                    raster.setPixel(x+to.x, y+to.y, brightest);
                }
            }
        }
    }

    private void copyAreaNotBlack(Raster data, WritableRaster raster, Rectangle from, Rectangle to) {
        int[] pixel = new int[4];
        int[] brightest = getBrightestPixel(data, from);

        for (int y = 0; y <= from.height; y++) {
            for (int x = 0; x <= from.width; x++) {
                data.getPixel(x + from.x, y + from.y, pixel);
                if (isNotBlack(pixel)) {
                    raster.setPixel(x + to.x, y + to.y, brightest);
                }
            }
        }
    }

    private void copyAreaAndFill(Raster data, WritableRaster raster, Rectangle from, Rectangle to) {
        int[] pixel = new int[4];
        int[] colouredPixel = getBrightestPixel(data, from);

        for (int y = 0; y <= from.getHeight(); y++) {
            boolean inside = false;
            boolean wasBlack = true;
            boolean isColoured;
            for (int x = 0; x <= from.getWidth(); x++) {
                data.getPixel(from.x + x, from.y + y, pixel);
                isColoured = isNotBlack(pixel);


                // We only need to change state when we leave a barrier
                if (!wasBlack && !isColoured) {
                    // Was not black, now black -> left a barrier
                    inside = !inside;
                    wasBlack = true;
                } else if (wasBlack && isColoured) {
                    wasBlack = false;
                }

                if (inside) {
                    raster.setPixel(x + to.x, y+ to.y, colouredPixel);
                    System.out.print("X");
                } else {
                    raster.setPixel(x + to.x, y + to.y, pixel);
                    System.out.print(" ");
                }
            }
            System.out.println();
        }
        System.out.println();
    }
}
