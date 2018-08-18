package net.ellise.sudoku;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;

public class Display {
    private static final String INITIAL_CONTRAST_SETTING = "2000";
    private static final String INITIAL_BUCKET_WIDTH = "20";
    private static final String INITIAL_BUCKET_HEIGHT = "20";
    private static final String INITIAL_BUCKET_BARRIER = "75";
    private ImageProcessor processor;
    private Webcam webcam;
    private JFrame frame;
    private JLabel imageLabel;
    private JTextField pixelBarrierText;
    private JTextField bucketWidthText;
    private JTextField bucketHeightText;
    private JTextField bucketBarrierText;
    private JToggleButton aboveBelowCheckBox;
    private AtomicBoolean started = new AtomicBoolean(false);
    private SlideShow slideShow;

    public Display(Webcam webcam, ImageProcessor processor) {
        this.webcam = webcam;
        this.processor = processor;
        this.frame = null;
        this.imageLabel = null;
        this.aboveBelowCheckBox = null;
        this.bucketWidthText = null;
        this.bucketHeightText = null;
        this.bucketBarrierText = null;
    }

    public void initialiseDisplay() throws Exception {
        if (started.compareAndSet(false, true)) {
            frame = new JFrame("Sudoku processor");
            frame.setLayout(new GridBagLayout());

            GridBagConstraints constraints = newGridBagConstraints();

            WebcamPanel webcamPanel = new WebcamPanel(webcam);
            frame.add(webcamPanel, constraints);

            JSeparator separator = new JSeparator();
            constraints.gridx = 1;
            constraints.gridy = 2;
            constraints.gridwidth = 2;
            frame.add(separator, constraints);

            JLabel barrierLabel = new JLabel("Pixel contrast barrier:");
            constraints.gridy = 3;
            constraints.gridwidth = 1;
            frame.add(barrierLabel, constraints);

            pixelBarrierText = new JTextField(INITIAL_CONTRAST_SETTING);
            constraints.gridx = 2;
            frame.add(pixelBarrierText, constraints);

            JLabel aboveBelowLabel = new JLabel("Above/Below barrier (tick for above):");
            constraints.gridy=4;
            constraints.gridx=1;
            frame.add(aboveBelowLabel, constraints);

            aboveBelowCheckBox = new JToggleButton("Above barrier", true);
            aboveBelowCheckBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JToggleButton button = (JToggleButton)e.getSource();
                    boolean mode = button.getModel().isSelected();
                    System.out.println("Above/below: "+mode);
                    if (mode) {
                        button.setText("Above barrier");
                    } else {
                        button.setText("Below barrier");
                    }
                }
            });
            constraints.gridx = 2;
            frame.add(aboveBelowCheckBox, constraints);

            JLabel bucketWidthLabel = new JLabel("Buckets Width:");
            constraints.gridx = 1;
            constraints.gridy = 5;
            frame.add(bucketWidthLabel, constraints);

            bucketWidthText = new JTextField(INITIAL_BUCKET_WIDTH);
            constraints.gridx = 2;
            frame.add(bucketWidthText, constraints);

            JLabel bucketHeightLabel = new JLabel("Buckets Height:");
            constraints.gridx = 1;
            constraints.gridy = 6;
            frame.add(bucketHeightLabel, constraints);

            bucketHeightText = new JTextField(INITIAL_BUCKET_HEIGHT);
            constraints.gridx = 2;
            frame.add(bucketHeightText, constraints);

            JLabel bucketBarrierLabel = new JLabel("Buckets barrier:");
            constraints.gridx = 1;
            constraints.gridy = 7;
            frame.add(bucketBarrierLabel, constraints);

            bucketBarrierText = new JTextField(INITIAL_BUCKET_BARRIER);
            constraints.gridx = 2;
            frame.add(bucketBarrierText, constraints);

            JButton process = new JButton("Process");
            process.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Display.this.updateDisplay();
                }
            });
            constraints.gridx = 1;
            constraints.gridy = 8;
            constraints.gridwidth = 2;
            frame.add(process, constraints);

            BufferedImage image = webcam.getImage();
            imageLabel = new JLabel(new ImageIcon(image));
            constraints.gridy = 1;
            constraints.gridx = 2;
            frame.add(imageLabel, constraints);

            slideShow = new SlideShow(imageLabel);
            updateDisplay();

            frame.setResizable(true);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack();
            frame.setVisible(true);
        }
    }

    private void updateDisplay() {
        int barrier = Integer.valueOf(pixelBarrierText.getText());
        int xWidth = Integer.valueOf(bucketWidthText.getText());
        int yWidth = Integer.valueOf(bucketHeightText.getText());
        int bucketBarrier = Integer.valueOf(bucketBarrierText.getText());
        boolean isAbove = aboveBelowCheckBox.getModel().isSelected();

        slideShow.clear();

        BufferedImage image = webcam.getImage();
        slideShow.addSlide(image);

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
            System.out.println(String.format("Distance to target %1$f; Relative distance %2$f; Tolerance %3$f", dPixels, dPixels/total, tolerance));
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

        Buckets pixelBuckets = Buckets.createBucket(xWidth, yWidth, currentFiltered.getImage());
        BufferedImage pixelAnnotated = processor.filterMostDenseRowAndColumn(currentFiltered.getImage(), pixelBuckets);
        slideShow.addSlide(pixelAnnotated);
        Filtered bucketFiltered = processor.applyBucketFilter(bucketBarrier, currentFiltered.getImage(), pixelBuckets);
        slideShow.addSlide(bucketFiltered.getImage());
        Buckets bucketBuckets = Buckets.createBucket(xWidth*2, yWidth*2, bucketFiltered.getImage());
        BufferedImage bucketAnnotated = processor.filterMostDenseRowAndColumn(currentFiltered.getImage(), bucketBuckets);
        slideShow.addSlide(bucketAnnotated);

        slideShow.start();

        frame.repaint();
    }

    /*
    private class SlideShow implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    for (int counter = 0; counter < NSLIDES; counter++) {
                        Display.this.imageLabel.setIcon(new ImageIcon(images[counter]));
                        Thread.sleep(SLIDE_DELAY);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
    */

    private GridBagConstraints newGridBagConstraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.fill = GridBagConstraints.BOTH;
        return constraints;
    }
}
