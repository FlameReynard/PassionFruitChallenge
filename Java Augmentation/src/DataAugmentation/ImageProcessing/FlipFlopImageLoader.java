package DataAugmentation.ImageProcessing;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class FlipFlopImageLoader extends ImageLoader{
    public FlipFlopImageLoader(File image) throws IOException {
        super(image);
    }

    public FlipFlopImageLoader(FlipFlopImageLoader imageLoader) throws IOException {
        super(imageLoader);
    }

    @Override
    public FlipFlopImageLoader clone() {
        try {
            return new FlipFlopImageLoader(this);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected void createPixelMatrix() {
        int height = image.getHeight();
        int width = image.getWidth();

        pixels = new FlipFlopPixel[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Color colour = new Color(image.getRGB(x, y));
                int red = colour.getRed();
                int green = colour.getGreen();
                int blue = colour.getBlue();

                FlipFlopPixel pixel = new FlipFlopPixel();
                pixel.fromRGB(red, green, blue);
                pixels[x][y] = pixel;
            }
        }
    }
}
