package DataAugmentation.ImageProcessing;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class ColourImageLoader extends FlipFlopImageLoader{//Here to provide extensibility to the FlipFlopImageLoader
    public ColourImageLoader(File image) throws IOException {
        super(image);
    }

    public ColourImageLoader(ColourImageLoader imageLoader) throws IOException {
        super(imageLoader);
    }

    @Override
    public ColourImageLoader clone() {
        try {
            return new ColourImageLoader(this);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected void createPixelMatrix() {
        int height = image.getHeight();
        int width = image.getWidth();

        pixels = new ColourPixel[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Color colour = new Color(image.getRGB(x, y));
                int red = colour.getRed();
                int green = colour.getGreen();
                int blue = colour.getBlue();

                ColourPixel pixel = new ColourPixel();
                pixel.fromRGB(red, green, blue);
                pixels[x][y] = pixel;
            }
        }
    }
}
