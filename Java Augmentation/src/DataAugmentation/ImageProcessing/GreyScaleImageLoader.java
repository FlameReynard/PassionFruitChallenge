package DataAugmentation.ImageProcessing;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class GreyScaleImageLoader extends ImageLoader {
    public GreyScaleImageLoader(GreyScaleImageLoader imageLoader) throws IOException {
        super(imageLoader);
    }

    public GreyScaleImageLoader(File image) throws IOException {
        super(image);
    }

    public ImageLoader clone() {
        try {
            return new GreyScaleImageLoader(this);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected void createPixelMatrix() {
        int height = image.getHeight();
        int width = image.getWidth();

        pixels = new GreyScalePixel[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Color colour = new Color(image.getRGB(x, y));
                int red = colour.getRed();
                int green = colour.getGreen();
                int blue = colour.getBlue();

                GreyScalePixel pixel = new GreyScalePixel();
                pixel.fromRGB(red, green, blue);
                pixels[x][y] = pixel;
            }
        }
    }
}
