package net.ellise.sudoku;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Hashtable;

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
}
