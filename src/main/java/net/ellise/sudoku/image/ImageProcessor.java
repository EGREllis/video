package net.ellise.sudoku.image;

import net.ellise.sudoku.model.Filtered;
import net.ellise.sudoku.model.UnionFind;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
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
            if (Math.abs(pixel[i]) < 20) {
                result = true;
                break;
            }
        }
        return result;
    }
}
