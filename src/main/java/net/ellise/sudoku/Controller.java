package net.ellise.sudoku;

import java.awt.image.BufferedImage;

public class Controller {
    private ImageProcessor processor;
    private SlideShow slideShow;

    public Controller(ImageProcessor processor, SlideShow slideShow) {
        this.processor = processor;
        this.slideShow = slideShow;
    }

    public void process(BufferedImage image, int barrier, boolean isAbove, int xWidth, int yWidth, int bucketBarrier) {
        Filtered currentFiltered = seekBestTextureFilter(image, barrier, isAbove);

        Buckets pixelBuckets = Buckets.createBucket(xWidth, yWidth, currentFiltered.getImage());
        BufferedImage pixelAnnotated = processor.filterMostDenseRowAndColumn(currentFiltered.getImage(), pixelBuckets);
        slideShow.addSlide(pixelAnnotated);
        Filtered bucketFiltered = processor.applyBucketFilter(bucketBarrier, currentFiltered.getImage(), pixelBuckets);
        slideShow.addSlide(bucketFiltered.getImage());
        Buckets bucketBuckets = Buckets.createBucket(xWidth*2, yWidth*2, bucketFiltered.getImage());
        BufferedImage bucketAnnotated = processor.filterMostDenseRowAndColumn(currentFiltered.getImage(), bucketBuckets);
        slideShow.addSlide(bucketAnnotated);
    }

    private Filtered seekBestTextureFilter(BufferedImage image, int barrier, boolean isAbove) {
        int total = image.getHeight() * image.getWidth();
        int target = total / 10; // Aim for 10% of the pixels
        int previousBarrier = barrier;
        int currentBarrier = barrier/2;
        Filtered previousFiltered = processor.getTextureFilteredImage(image, previousBarrier, isAbove);
        slideShow.addSlide(previousFiltered.getImage());
        Filtered currentFiltered = processor.getTextureFilteredImage(image, currentBarrier, isAbove);
        slideShow.addSlide(currentFiltered.getImage());
        double tolerance = 0.0001;
        for (int i = 0; i < 8 ; i++) {
            double dPixels = (double)Math.abs(target - currentFiltered.getPresent());
            System.out.println(String.format("Distance to target %1$8f; Relative distance %2$8f; Tolerance %3$6f", dPixels, dPixels/total, tolerance));
            if ((dPixels/total) < tolerance) {
                System.out.println("Reached target within tolerance");
                break;
            }
            int x1 = previousBarrier;
            int y1 = previousFiltered.getPresent();
            int x2 = currentBarrier;
            long y2 = currentFiltered.getPresent();
            long dy = y2-y1;
            long dx = x2-x1;
            /*
                Let y be the number of visible pixels post filter
                Let x be the barrier of the filter
                Assume linear relationship (untrue)

                y = mx + c
                y = (dy/dx)x + c

                Rearrange for c, substituting a known point (x2, y2)
                c = y2-(dy/dx)x2
                Substitute into the previous
                y = (dy/dx)x + y2-(dy/dx)x2

                We want to find the barrier "expected" when the filter returns the target number of pixels
                target = (dy/dx)x + y2-(dy/dx)x2
                Rearrange to find x
                x = (target - y2 + (dy/dx)x2)/(dy/dx)
             */

            int nextBarrier = (int) ((target - y2 + (dy/dx)*x2)/(dy/dx));

            if (nextBarrier == currentBarrier) {
                System.out.println("Would retry previous barrier - aborting...");
                break;
            }

            previousBarrier = currentBarrier;
            currentBarrier = nextBarrier;
            previousFiltered = currentFiltered;
            currentFiltered = processor.getTextureFilteredImage(image, currentBarrier, isAbove);
            slideShow.addSlide(currentFiltered.getImage());
        }
        return currentFiltered;
    }
}
