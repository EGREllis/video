package net.ellise.sudoku;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

public class ImageProcessor {
    public Filtered getTextureFilteredImage(BufferedImage image, int barrier, boolean above) {
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

                if (contrast > barrier && above) {
                    output.setPixel(x, y, pixel);
                    filtered++;
                } else if (contrast < barrier && !above) {
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


    public BufferedImage filterMostDenseRowAndColumn(BufferedImage contrast, Buckets buckets) {
        Raster raster = contrast.getData();
        WritableRaster output = raster.createCompatibleWritableRaster();

        int[] freqX = new int[buckets.getNXBuckets()];
        int[] freqY = new int[buckets.getNYBuckets()];
        for (int by = 0; by < buckets.getNYBuckets(); by++) {
            for (int bx = 0; bx < buckets.getNXBuckets(); bx++) {
                freqX[bx] += buckets.getBucket(bx, by);
                freqY[by] += buckets.getBucket(bx, by);
            }
        }


        StringBuilder rows = new StringBuilder();
        StringBuilder cols = new StringBuilder();
        rows.append("rows|");
        cols.append("cols|");
        int maxX = 0;
        int bucketX = 0;
        int maxY = 0;
        int bucketY = 0;
        for (int i = 0; i < Math.max(freqX.length, freqY.length); i++) {
            if (i < freqX.length) {
                rows.append(String.format(" %1$d |", freqX[i]));
                if (maxX < freqX[i]) {
                    maxX = freqX[i];
                    bucketX = i;
                }
            }
            if (i < freqY.length) {
                cols.append(String.format(" %1$d |", freqY[i]));
                if (maxY < freqY[i]) {
                    maxY = freqY[i];
                    bucketY = i;
                }
            }
        }
        System.out.println(rows.toString()+"\n"+cols.toString());

        int[] pixel = new int[4];
        // Most dense X bucket row
        for (int x = bucketX * buckets.getXWidth(); x < (bucketX+1) * buckets.getXWidth(); x++) {
            for (int y = raster.getMinY(); y < raster.getHeight(); y++) {
                pixel = raster.getPixel(x, y, pixel);
                output.setPixel(x, y, pixel);
            }
        }

        // Most dense Y bucket column
        for (int y = bucketY * buckets.getYWidth(); y < (bucketY+1)*buckets.getYWidth(); y++) {
            for (int x = raster.getMinX(); x < raster.getHeight(); x++) {
                pixel = raster.getPixel(x, y, pixel);
                output.setPixel(x, y, pixel);
            }
        }

        return new BufferedImage(contrast.getColorModel(), output, true, new Hashtable<>());
    }

    public Filtered applyBucketFilter(int barrier, BufferedImage contrast, Buckets buckets) {
        Raster raster = contrast.getData();
        WritableRaster output = raster.createCompatibleWritableRaster();

        int filtered = 0;
        int[] pixel = new int[4];
        for (int by = 0; by < buckets.getNYBuckets(); by++) {
            for (int bx = 0; bx < buckets.getNXBuckets(); bx++) {
                if (buckets.getBucket(bx, by) > barrier) {
                    for (int y = by*buckets.getYWidth(); y < (by+1)*buckets.getYWidth(); y++) {
                        if (y < raster.getWidth()) {
                            for (int x = bx*buckets.getXWidth(); x < (bx+1)*buckets.getYWidth(); x++) {
                                if (x < raster.getHeight()) {
                                    pixel = raster.getPixel(x, y, pixel);
                                    output.setPixel(x, y, pixel);
                                    filtered++;
                                }
                            }
                        }
                    }
                }
            }
        }

        return new Filtered(new BufferedImage(contrast.getColorModel(), output, true, new Hashtable()), filtered);
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

    public UnionFind determineRegions(BufferedImage image, int pixelDifference) {
        Raster raster = image.getData();
        UnionFind result = UnionFind.regionFor(image);
        int[] black = new int[4];
        int[] pixel = new int[4];
        int[] other = new int[4];
        for (int y = raster.getMinY(); y < raster.getHeight(); y++) {
            for (int x = raster.getMinX(); x < raster.getWidth(); x++) {
                pixel = raster.getPixel(x, y, pixel);
                if (!sameRegion(pixel, black, pixelDifference)) {
                    for (Point point : getNeighbouringPoints(x, y, raster)) {
                        other = raster.getPixel(point.x, point.y, other);
                        if (!sameRegion(other, black, pixelDifference)) {
                            result.connect(x, y, point.x, point.y);
                        }
                    }
                }
            }
        }
        result.normalise();
        return result;
    }

    private Set<Point> getNeighbouringPoints(int x, int y, Raster raster) {
        Set<Point> result = new HashSet<>();
        Set<Point> directions = new HashSet<>();
        for (int[] dir : new int[][]{{1, 1}, {1, 0}, {1, -1}, {0, 1}, {0, -1}, {-1, 1}, {-1, 0}, {-1,-1}}) {
            directions.add(new Point(dir[0], dir[1]));
        }

        Set<Point> currentRing = new HashSet<>();
        Set<Point> previousRing = new HashSet<>(directions);
        for (int i  = 0; i < 3; i++) {
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

    private boolean sameRegion(int[] pixel1, int[] pixel2, int pixelDifference) {
        boolean result = true;
        for (int i = 0; i < 3; i++) {
            if (Math.abs(pixel1[i] - pixel2[i]) > pixelDifference) {
                result = false;
                break;
            }
        }
        return result;
    }
}
