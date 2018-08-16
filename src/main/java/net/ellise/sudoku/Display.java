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
    private static final long SLIDE_DELAY = 1000;
    private static final int NSLIDES = 5;
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
    private AtomicBoolean slideStarted = new AtomicBoolean(false);
    private volatile BufferedImage[] images;

    public Display(Webcam webcam, ImageProcessor processor) {
        this.webcam = webcam;
        this.processor = processor;
        this.frame = null;
        this.imageLabel = null;
        this.aboveBelowCheckBox = null;
        this.bucketWidthText = null;
        this.bucketHeightText = null;
        this.bucketBarrierText = null;
        this.images = new BufferedImage[NSLIDES];
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

            JLabel bucketWidthLabel = new JLabel("Bucket Width:");
            constraints.gridx = 1;
            constraints.gridy = 5;
            frame.add(bucketWidthLabel, constraints);

            bucketWidthText = new JTextField(INITIAL_BUCKET_WIDTH);
            constraints.gridx = 2;
            frame.add(bucketWidthText, constraints);

            JLabel bucketHeightLabel = new JLabel("Bucket Height:");
            constraints.gridx = 1;
            constraints.gridy = 6;
            frame.add(bucketHeightLabel, constraints);

            bucketHeightText = new JTextField(INITIAL_BUCKET_HEIGHT);
            constraints.gridx = 2;
            frame.add(bucketHeightText, constraints);

            JLabel bucketBarrierLabel = new JLabel("Bucket barrier:");
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

        BufferedImage image = webcam.getImage();
        BufferedImage filtered = processor.getTextureFilteredImage(image, barrier, isAbove);
        GridBagConstraints constraints = newGridBagConstraints();
        constraints.gridx = 2;

        int[][] pixelModalMatrix = processor.getModalMatrix(xWidth, yWidth, filtered);
        BufferedImage pixelAnnotated = processor.annotateModalMatrix(filtered, pixelModalMatrix, xWidth, yWidth);
        BufferedImage bucketFiltered = processor.applyBucketBarrier(bucketBarrier, filtered, pixelModalMatrix, xWidth, yWidth);
        int[][] bucketModalMatrix = processor.getModalMatrix(xWidth*2, yWidth*2, bucketFiltered);
        BufferedImage bucketAnnotated = processor.annotateModalMatrix(filtered, bucketModalMatrix, xWidth*2, yWidth *2);

        images[0] = image;
        images[1] = filtered;
        images[2] = pixelAnnotated;
        images[3] = bucketFiltered;
        images[4] = bucketAnnotated;

        if (slideStarted.compareAndSet(false, true)) {
            Thread slideShow = new Thread(new SlideShow());
            slideShow.start();
        }

        imageLabel.setIcon(new ImageIcon(bucketFiltered));
        frame.repaint();
        processor.logModalMatrix(pixelModalMatrix);
    }

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
