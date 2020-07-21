package net.ellise.sudoku.swing;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import net.ellise.sudoku.controller.Controller;
import net.ellise.sudoku.image.ImageProcessor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;

public class Display {
    private ImageProcessor processor;
    private Webcam webcam;
    private JFrame frame;
    private JLabel imageLabel;
    private AtomicBoolean started = new AtomicBoolean(false);
    private SlideShow slideShow;
    private Controller controller;

    public Display(Webcam webcam, ImageProcessor processor) {
        this.webcam = webcam;
        this.processor = processor;
        this.frame = null;
        this.imageLabel = null;
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

            JButton process = new JButton("Process");
            process.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Display.this.updateDisplay(true);
                }
            });
            constraints.gridx = 1;
            constraints.gridy = 3;
            constraints.gridwidth = 2;
            frame.add(process, constraints);

            BufferedImage image = webcam.getImage();
            imageLabel = new JLabel(new ImageIcon(image));
            constraints.gridy = 1;
            constraints.gridx = 2;
            frame.add(imageLabel, constraints);

            slideShow = new SlideShow(imageLabel);
            controller = new Controller(processor, slideShow);
            updateDisplay(false);

            frame.setResizable(true);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack();
            frame.setVisible(true);
        }
    }

    private void updateDisplay(boolean process) {
        slideShow.clear();

        BufferedImage image = webcam.getImage();
        slideShow.addSlide(image);

        if (process) {
            controller.process(image);
        }

        slideShow.start();
        frame.repaint();
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
