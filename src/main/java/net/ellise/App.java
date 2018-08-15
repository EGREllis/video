package net.ellise;

import com.github.sarxos.webcam.Webcam;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.*;

/**
 * Hello world!
 */
public class App 
{
    public static void main( String[] args ) throws Exception
    {
        System.out.println( "Hello World!" );

        Webcam webcam = Webcam.getDefault();
        webcam.open();

        /*
        ImageIO.write(webcam.getImage(), "PNG", new File("hello-world.png"));
        ImageIO.write(webcam.getImage(), "BMP", new File("hello-world.bmp"));
        ImageIO.write(webcam.getImage(), "WBMP", new File("hello-world-windows.bmp"));
        ImageIO.write(webcam.getImage(), "GIF", new File("hello-world.gif"));
        ImageIO.write(webcam.getImage(), "JPEG", new File("hello-world.jpeg"));
        */

        BufferedImage shot = webcam.getImage();

        ImageIO.write(shot, "PNG", new File("sudoku.png"));
        BufferedImage image = ImageIO.read(new File("./sudoku.png"));
        int minX = image.getMinX();
        int nX = image.getNumXTiles();
        int minY = image.getMinY();
        int nY = image.getNumYTiles();

        System.out.println(String.format("MinX: %1$d\tnX: %2$d\tMinY: %3$d\tnY: %4$d", minX, nX, minY, nY));

        Raster raster = image.getData();
        System.out.println(String.format("Height: %1$d\tWidth: %2$d\tX: %3$d\tY: %4$d", raster.getHeight(), raster.getWidth(), raster.getMinX(), raster.getMinY()));

        WritableRaster low = raster.createCompatibleWritableRaster();
        WritableRaster medium = raster.createCompatibleWritableRaster();
        WritableRaster high = raster.createCompatibleWritableRaster();
        int[] pixel = new int[4];
        int[] left = new int[4];
        int[] right = new int[4];
        int[] up = new int[4];
        int[] down = new int[4];
        Map<Integer,Integer> contrasts = new TreeMap<>();
        for (int y = raster.getMinY(); y < raster.getHeight(); y++) {
            for (int x = raster.getMinX(); x < raster.getWidth(); x++) {
                System.out.print(String.format("Pixel at [%1$d,%2$d] ", x, y));
                System.out.flush();
                pixel = raster.getPixel(x, y, pixel);
                System.out.print(String.format("[%1$d,%2$d,%3$d,%4$d] ", pixel[0], pixel[1], pixel[2], pixel[3]));

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
                System.out.println(String.format(" contrast: %1$d", contrast));
                if (!contrasts.containsKey(contrast)) {
                    contrasts.put(contrast, 1);
                } else {
                    int count = contrasts.get(contrast);
                    contrasts.put(contrast, ++count);
                }

                if (contrast > 8000) {
                    high.setPixel(x, y, pixel);
                } else if (contrast < 600) {
                    low.setPixel(x, y, pixel);
                } else {
                    medium.setPixel(x, y, pixel);
                }
            }
        }

        for (Map.Entry<Integer, Integer> entry : contrasts.entrySet()) {
            System.out.println(String.format("[ %1$d, %2$d ]", entry.getKey(), entry.getValue()));
        }

        Hashtable properties = null;
        if (image.getPropertyNames() != null) {
            properties = new Hashtable();
            for (String key : image.getPropertyNames()) {
                properties.put(key, image.getProperty(key));
            }
        }

        ImageIO.write(new BufferedImage(image.getColorModel(), low, true, properties), "PNG", new File("low.png"));
        ImageIO.write(new BufferedImage(image.getColorModel(), medium, true, properties), "PNG", new File("medium.png"));
        ImageIO.write(new BufferedImage(image.getColorModel(), high, true, properties), "PNG", new File("high.png"));
    }

    private static int diffPixel(int[] pixel, int[] other) {
        int result = 0;
        for (int i = 0; i < 4; i++) {
            result += (int)Math.pow(pixel[i]-other[i],2);
        }
        return result;
    }
}
