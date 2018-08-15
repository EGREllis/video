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
    private ImageProcessor processor;
    private Webcam webcam;
    private JFrame frame;
    private JLabel imageLabel;
    private JTextField barrierText;
    private JToggleButton aboveBelowCheckBox;
    private AtomicBoolean started = new AtomicBoolean(false);

    public Display(Webcam webcam, ImageProcessor processor) {
        this.webcam = webcam;
        this.processor = processor;
        this.frame = null;
        this.imageLabel = null;
        this.aboveBelowCheckBox = null;
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

            JLabel barrierLabel = new JLabel("Barrier:");
            constraints.gridy = 3;
            constraints.gridwidth = 1;
            frame.add(barrierLabel, constraints);

            barrierText = new JTextField(INITIAL_CONTRAST_SETTING);
            constraints.gridx = 2;
            frame.add(barrierText, constraints);

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

            JButton process = new JButton("Process");
            process.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Display.this.updateDisplay();
                }
            });
            constraints.gridx = 1;
            constraints.gridy = 5;
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
        System.out.println("Updating...");
        int barrier = Integer.valueOf(barrierText.getText());
        BufferedImage image = webcam.getImage();
        BufferedImage filtered = processor.getTextureFilteredImage(image, barrier, aboveBelowCheckBox.getModel().isSelected());
        GridBagConstraints constraints = newGridBagConstraints();
        constraints.gridx = 2;
        imageLabel.setIcon(new ImageIcon(filtered));
        frame.repaint();
        System.out.println("Updated...");
        int[][] modalMatrix = processor.getModalMatrix(20, 20, filtered);
        processor.logModalMatrix(modalMatrix);
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
