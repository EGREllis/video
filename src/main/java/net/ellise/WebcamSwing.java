package net.ellise;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;

public class WebcamSwing {
    public static void main(String[] args) throws Exception {
        Webcam webcam = Webcam.getDefault();
        webcam.setViewSize(WebcamResolution.VGA.getSize());

        WebcamPanel panel = new WebcamPanel(webcam);
        panel.setMirrored(false);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;

        BufferedImage image = ImageIO.read(new File("./sudoku.png"));
        JLabel imageDisplay = new JLabel(new ImageIcon(image));

        JButton capture = new JButton("Capture");
        capture.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Processing...");
                System.out.flush();
            }
        });

        JButton process = new JButton("Process");
        process.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Capture....");
                System.out.flush();
            }
        });

        JFrame window = new JFrame("Sudoku capture");
        window.setLayout(new GridBagLayout());
        window.add(panel, constraints);
        constraints.gridx = 2;
        window.add(imageDisplay, constraints);
        constraints.gridx = 1;
        constraints.gridy = 2;
        window.add(capture, constraints);
        constraints.gridx = 2;
        window.add(process, constraints);

        window.setResizable(true);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.pack();
        window.setVisible(true);
    }
}
