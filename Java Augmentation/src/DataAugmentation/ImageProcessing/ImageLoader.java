package DataAugmentation.ImageProcessing;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public abstract class ImageLoader {
    private final File file;
    private final String originalFileName;
    private String folderName;
    protected BufferedImage image;
    protected Pixel[][] pixels;

    public ImageLoader(ImageLoader other) throws IOException {
        this.file = other.file;
        this.image = ImageIO.read(file);
        this.pixels = other.pixels;
        this.originalFileName = other.originalFileName;
        this.folderName = other.folderName;
    }

    public ImageLoader(File image) throws IOException {
        this.file = image;
        this.image = ImageIO.read(image);
        this.originalFileName = image.getName();
        this.folderName = "ProcessedImages";
        createPixelMatrix();
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public String getName() {
        return originalFileName;
    }

    public void scale(double scale) {
        int width = (int)(image.getWidth()*scale);
        int height = (int)(image.getHeight()*scale);
        Image scaled = image.getScaledInstance(width, height, Image.SCALE_DEFAULT);
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        image.getGraphics().drawImage(scaled, 0, 0, null);
        createPixelMatrix();
    }

    public void resize(int x, int width, int y, int height) {
        image = image.getSubimage(x, y, width, height);
        createPixelMatrix();
    }

    public Pixel[][] getPixels() {
        return pixels;
    }

    public double[][] getIntensities() {
        double[][] intensities = new double[pixels.length][pixels[0].length];
        for (int x = 0; x < pixels.length; x++) {
            for (int y = 0; y < pixels[0].length; y++) {
                intensities[x][y] = pixels[x][y].getIntensity();
            }
        }
        return intensities;
    }

    public void saveImage() throws IOException {
        save(originalFileName);
    }

    public void saveImage(String filename) throws IOException {
        save(filename + ".jpg");
    }

    public void saveProcessedImage() throws IOException {
        String filename = "Processed.jpg";
        save(filename);
    }

    public void setPixels(Pixel[][] pixels) {
        this.pixels = pixels;
    }

    private void save(String filename) throws IOException {
        File folder = new File(folderName);
        if (!folder.exists()) {
            folder.mkdir();
        }

        saveToFile(folderName + "/" + filename);

        try {
            ImageIO.write(image, "jpg", file);
            System.out.println("Successfully saved image as \"" + filename + "\"");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveToFile(String filename) throws IOException {
        for (int x = 0; x < pixels.length; x++) {
            for (int y = 0; y < pixels[x].length; y++) {
                Color colour = pixels[x][y].toRGB();
                image.setRGB(x, y, colour.getRGB());
            }
        }
        File file = new File(filename);
        file.createNewFile();

        try {
            ImageIO.write(image, "jpg", file);
            System.out.println("Successfully saved image as \"" + filename + "\"");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int[] getHistogram() {
        int[] histogram = new int[256];

        for (Pixel[] pixelArray : pixels) {
            for (Pixel pixel : pixelArray) {
                int intensity = pixel.getIntensity();
                histogram[intensity]++;
            }
        }

        return histogram;
    }

    public int getNumberOfPixels() {
        if (pixels.length == 0) {
            return 0;
        }
        return pixels.length*pixels[0].length;
    }

    protected abstract void createPixelMatrix();
    public abstract ImageLoader clone();
}
