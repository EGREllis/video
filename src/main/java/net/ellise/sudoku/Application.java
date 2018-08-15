package net.ellise.sudoku;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;

public class Application {
    public static void main(String args[]) throws Exception {
        Webcam webcam = Webcam.getDefault();
        webcam.setViewSize(WebcamResolution.VGA.getSize());

        ImageProcessor processor = new ImageProcessor();
        Display display = new Display(webcam, processor);
        display.initialiseDisplay();
    }
}
