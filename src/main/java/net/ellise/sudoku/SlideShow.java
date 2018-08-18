package net.ellise.sudoku;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class SlideShow implements Runnable {
    private static final long DELAY = 1000;
    private final JLabel label;
    private final ConcurrentLinkedQueue<BufferedImage> slides;
    private final AtomicBoolean started = new AtomicBoolean(false);

    public SlideShow(JLabel jLabel) {
        this.label = jLabel;
        slides = new ConcurrentLinkedQueue<>();
    }

    public void addSlide(BufferedImage image) {
        slides.offer(image);
    }

    public void clear() {
        slides.clear();
    }

    public void start() {
        if (started.compareAndSet(false, true)) {
            Thread slideThread = new Thread(this);
            slideThread.start();
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                for (BufferedImage image : slides) {
                    label.setIcon(new ImageIcon(image));
                    Thread.sleep(DELAY);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
