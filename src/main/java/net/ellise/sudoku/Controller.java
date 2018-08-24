package net.ellise.sudoku;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

public class Controller {
    private static final int INITIAL_TEXTURE_BARRIER = 2000;
    private ImageProcessor processor;
    private SlideShow slideShow;

    public Controller(ImageProcessor processor, SlideShow slideShow) {
        this.processor = processor;
        this.slideShow = slideShow;
    }

    public void process(BufferedImage image) {
        Filtered currentFiltered = seekBestTextureFilter(image);
        slideShow.addSlide(currentFiltered.getImage());

        int neighbourRange = 2;
        UnionFind regions = processor.determineRegions(currentFiltered.getImage(), neighbourRange);
        System.out.println(String.format("NRegions %1$d with pixel difference %2$d", regions.getNumberOfRegions(), neighbourRange));
        int biggestRegion = regions.getBiggestGroup();
        slideShow.addSlide(processor.applyRegionFilter(currentFiltered.getImage(), regions, biggestRegion).getImage());

        Rectangle board = getBoundsOfPoints(regions.getPointsForRegion(biggestRegion));
        int regionBarrier = 20;
        Map<Integer,Rectangle> areas = getAreasOfInterest(regions, board, biggestRegion, regionBarrier);
        System.out.println(String.format("Identified %1$d areas of interest inside board", areas.size()));
        slideShow.addSlide(processor.applyRegionsFilter(currentFiltered.getImage(), regions, areas.keySet()).getImage());
    }

    private Map<Integer,Rectangle> getAreasOfInterest(UnionFind regions, Rectangle board, int boardId, int regionBarrier) {
        Map<Integer, Integer> regionSize = regions.getSizeOfRegions();
        Map<Integer, Rectangle> areasOfInterest = new HashMap<>();
        for (Integer regionId : regions.getSizeOfRegions().keySet()) {
            if (regionId == boardId) {
                // the board;
                continue;
            } else if (regionSize.get(regionId) < regionBarrier) {
                // To small;
                continue;
            }
            Rectangle area = getBoundsOfPoints(regions.getPointsForRegion(regionId));
            if (    area.x >= board.x && area.x + area.getWidth() <= board.x + board.width &&
                    area.y >= board.y && area.y + area.getHeight() <= board.y + board.getHeight()) {
                areasOfInterest.put(regionId, area);
            }
        }
        return areasOfInterest;
    }

    private Rectangle getBoundsOfPoints(Set<Point> points) {
        boolean first = true;
        int minX = 0;
        int minY = 0;
        int maxX = 0;
        int maxY = 0;
        for (Point point : points) {
            if (first) {
                minX = point.x;
                maxX = point.x;
                minY = point.y;
                maxY = point.y;
                first = false;
            } else {
                if (point.x < minX) {
                    minX = point.x;
                } else if (point.x > maxX) {
                    maxX = point.x;
                }
                if (point.y < minY) {
                    minY = point.y;
                } else if (point.y > maxY) {
                    maxY = point.y;
                }
            }
        }
        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    private Filtered seekBestTextureFilter(BufferedImage image) {
        int total = image.getHeight() * image.getWidth();
        int target = total / 10; // Aim for 10% of the pixels
        int previousBarrier = INITIAL_TEXTURE_BARRIER;
        int currentBarrier = INITIAL_TEXTURE_BARRIER/2;
        Filtered previousFiltered = processor.getTextureFilteredImage(image, previousBarrier);
        Filtered currentFiltered = processor.getTextureFilteredImage(image, currentBarrier);
        double tolerance = 0.0001;
        for (int i = 0; i < 8 ; i++) {
            double dPixels = (double)Math.abs(target - currentFiltered.getPresent());
            System.out.println(String.format("Distance to target %1$8f; Relative distance %2$8f; Tolerance %3$6f", dPixels, dPixels/total, tolerance));
            if ((dPixels/total) < tolerance) {
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
            currentFiltered = processor.getTextureFilteredImage(image, currentBarrier);
        }
        return currentFiltered;
    }
}
