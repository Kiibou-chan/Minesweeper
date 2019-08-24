package space.kiibou.gui;

import processing.core.PImage;

import javax.imageio.ImageIO;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Objects;

public class ImageBuffer {

    private static final HashMap<String, PImage> buffer;

    static {
        buffer = new HashMap<>();
    }

    public static PImage loadImage(String path) {
        return buffer.computeIfAbsent(path, p -> {
            try {
                InputStream stream = GraphicsElement.class.getClassLoader().getResourceAsStream(path);
                Objects.requireNonNull(stream);
                return new PImage(ImageIO.read(stream));
            } catch (Exception e) {
                System.out.printf("Error loading image \"%s\"%n", path);
                return new PImage(0, 0);
            }
        });
    }

}
