package net.ellise.sudoku;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;

public class ImageProcessor {
    public BufferedImage getTextureFilteredImage(BufferedImage image, int barrier, boolean above) throws Exception {
        Raster raster = image.getData();

        WritableRaster output = raster.createCompatibleWritableRaster();
        int[] pixel = new int[4];
        int[] left = new int[4];
        int[] right = new int[4];
        int[] up = new int[4];
        int[] down = new int[4];
        Map<Integer,Integer> contrasts = new TreeMap<>();
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
}
