package net.ellise.sudoku.image;

import com.asprise.ocr.Ocr;

import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;

public class OpticalCharacter {
    private static AtomicBoolean ocrStarted = new AtomicBoolean(false);

    public static String characterRecognition(BufferedImage digits) {
        if (ocrStarted.compareAndSet(false, true)) {
            Ocr.setUp();
        }
        Ocr ocr = new Ocr();
        ocr.startEngine("eng", Ocr.SPEED_SLOW);
        String text = ocr.recognize(digits, Ocr.RECOGNIZE_TYPE_TEXT, Ocr.OUTPUT_FORMAT_PLAINTEXT);
        ocr.stopEngine();
        return text;
    }
}
