package net.ellise.sudoku.controller;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import net.ellise.sudoku.image.ImageProcessor;
import net.ellise.sudoku.swing.Display;

public class Application {
    public static void main(String args[]) throws Exception {
        Webcam webcam = Webcam.getDefault();
        webcam.setViewSize(WebcamResolution.VGA.getSize());

        ImageProcessor processor = new ImageProcessor();
        Display display = new Display(webcam, processor);
        display.initialiseDisplay();
    }
}
