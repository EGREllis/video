package net.ellise.sudoku;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Hashtable;

public class ImageProcessor {
    public BufferedImage getTextureFilteredImage(BufferedImage image, int barrier, boolean above) {
        Raster raster = image.getData();

        WritableRaster output = raster.createCompatibleWritableRaster();
        int[] pixel = new int[4];
        int[] left = new int[4];
        int[] right = new int[4];
        int[] up = new int[4];
        int[] down = new int[4];

        int total = 0;
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
                total++;
            }
        }
        System.out.println(String.format("Filtered at barrier %1$d; total pixels: %2$d; filtered pixels: %3$d [%4$d, %5$d]", barrier, total, filtered, raster.getWidth(), raster.getHeight()));

        Hashtable properties = null;
        if (image.getPropertyNames() != null) {
            properties = new Hashtable();
            for (String key : image.getPropertyNames()) {
                properties.put(key, image.getProperty(key));
            }
        }

        return new BufferedImage(image.getColorModel(), output, true, properties);
    }

    private static int diffPixel(int[] pixel, int[] other) {
        int result = 0;
        for (int i = 0; i < 4; i++) {
            result += (int)Math.pow(pixel[i]-other[i],2);
        }
        return result;
    }

    public int[][] getModalMatrix(int xWidth, int yWidth, BufferedImage image) {
        Raster raster = image.getData();
        int nXBuckets = 1+ (raster.getWidth() - raster.getMinX()) / xWidth;
        int nYBuckets = 1+ (raster.getHeight() - raster.getMinY()) / yWidth;

        System.out.println(String.format("Modal matrix:\n\tWidth (%1$d wide, %2$d per bucket -> %3$d buckets)\n\tHeight (%4$d high, %5$d per bucket) -> %6$d buckets", raster.getWidth(), xWidth, nXBuckets, raster.getHeight(), yWidth, nYBuckets));

        // Initialise buckets with zero
        int[][] output = new int[nYBuckets][];
        for (int i = 0; i < nYBuckets; i++) {
            output[i] = new int[nXBuckets];
            for (int j = 0; j < nXBuckets; j++) {
                output[i][j] = 0;
            }
        }

        int[] pixel = new int[4];
        for (int y = raster.getMinY(); y < raster.getHeight(); y++) {
            for (int x = raster.getMinX(); x < raster.getWidth(); x++) {
                pixel = raster.getPixel(x, y, pixel);
                if (pixel[0] != 0 || pixel[1] != 0 || pixel[2] != 0) {
                    int bucketx = (x - raster.getMinX()) / xWidth;
                    int buckety = (y - raster.getMinY()) / yWidth;
                    int temp = output[buckety][bucketx];
                    output[buckety][bucketx] = temp+1;
                }
            }
        }
        return output;
    }

    public BufferedImage annotateMostDenseRowColumn(BufferedImage contrast, Buckets buckets) {
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

        int maxX = 0;
        int bucketX = 0;
        int maxY = 0;
        int bucketY = 0;
        for (int i = 0; i < Math.max(freqX.length, freqY.length); i++) {
            if (i < freqX.length && maxX < freqX[i]) {
                maxX = freqX[i];
                bucketX = i;
            }
            if (i < freqY.length && maxY < freqY[i]) {
                maxY = freqY[i];
                bucketY = i;
            }
        }

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

    public BufferedImage applyBucketBarrier(int barrier, BufferedImage contrast, Buckets buckets) {
        Raster raster = contrast.getData();
        WritableRaster output = raster.createCompatibleWritableRaster();

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
                                }
                            }
                        }
                    }
                }
            }
        }

        return new BufferedImage(contrast.getColorModel(), output, true, new Hashtable());
    }
}
