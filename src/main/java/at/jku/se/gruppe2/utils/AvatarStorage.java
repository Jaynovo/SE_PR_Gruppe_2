package at.jku.se.gruppe2.utils;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AvatarStorage {

    private static Path baseDir() throws IOException {
        Path dir = Paths.get(System.getProperty("user.home"), ".smarthome", "avatars");
        Files.createDirectories(dir);
        return dir;
    }

    public static String saveAvatarForUser(int userId, Image image) throws IOException {
        Path out = baseDir().resolve("user_" + userId + ".png");

        BufferedImage buffered = toBufferedImage(image);
        ImageIO.write(buffered, "png", out.toFile());

        return out.toAbsolutePath().toString();
    }

    private static BufferedImage toBufferedImage(Image fxImage) {
        int w = (int) fxImage.getWidth();
        int h = (int) fxImage.getHeight();

        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        PixelReader pr = fxImage.getPixelReader();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                image.setRGB(x, y, pr.getArgb(x, y));
            }
        }
        return image;
    }
}
