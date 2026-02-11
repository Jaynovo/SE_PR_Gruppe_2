package at.jku.se.gruppe2.infrastructure.storage;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * Utility class responsible for persisting user avatar images to the local file system.
 *
 * <p>Avatars are stored as PNG files in a user-specific directory under:</p>
 * <pre>
 *   ${user.home}/.smarthome/avatars/
 * </pre>
 *
 * <p>The filename convention is {@code user_<userId>.png}.</p>
 *
 * <p><b>Technology note:</b> This class bridges JavaFX ({@link Image}) and AWT
 * ({@link BufferedImage}) to enable writing images via {@link ImageIO}.</p>
 *
 * <p><b>Layering:</b> This class belongs to the infrastructure/storage layer and
 * is UI-adjacent due to its dependency on JavaFX image types.</p>
 */
public class AvatarStorage {

    /**
     * Resolves and creates (if necessary) the base directory for avatar storage.
     *
     * @return path to the avatar base directory
     * @throws IOException if the directory cannot be created or accessed
     */
    private static Path baseDir() throws IOException {
        Path dir = Paths.get(System.getProperty("user.home"), ".smarthome", "avatars");
        Files.createDirectories(dir);
        return dir;
    }

    /**
     * Saves the given JavaFX image as a PNG avatar file for the specified user.
     *
     * <p>If a file already exists for the user, it will be overwritten.</p>
     *
     * @param userId the id of the user the avatar belongs to
     * @param image  the JavaFX {@link Image} to persist
     * @return the absolute file system path to the stored avatar image
     * @throws IOException if writing the image fails or the directory cannot be created
     */
    public static String saveAvatarForUser(int userId, Image image) throws IOException {
        Path out = baseDir().resolve("user_" + userId + ".png");

        BufferedImage buffered = toBufferedImage(image);
        ImageIO.write(buffered, "png", out.toFile());

        return out.toAbsolutePath().toString();
    }

    /**
     * Converts a JavaFX {@link Image} into an AWT {@link BufferedImage}.
     *
     * <p>This method performs a pixel-by-pixel copy using a {@link PixelReader},
     * preserving the ARGB color model.</p>
     *
     * @param fxImage JavaFX image to convert
     * @return equivalent {@link BufferedImage}
     */
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
