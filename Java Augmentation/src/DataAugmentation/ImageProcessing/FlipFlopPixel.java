package DataAugmentation.ImageProcessing;

import java.awt.*;

public class FlipFlopPixel implements Pixel {
    private final GreyScalePixel greyScalePixel;
    private int red;
    private int green;
    private int blue;

    public FlipFlopPixel() {
        greyScalePixel = new GreyScalePixel();
        red = 0;
        green = 0;
        blue = 0;
    }

    @Override
    public int getIntensity() {
        return greyScalePixel.getIntensity();
    }

    @Override
    public void setIntensity(int intensity) {
        greyScalePixel.setIntensity(intensity);
    }

    @Override
    public void setColour(Colour colour) {
    }

    @Override
    public void fromRGB(int red, int green, int blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.greyScalePixel.fromRGB(red, green, blue);
    }

    @Override
    public Color toRGB() {
        if (this.greyScalePixel.getIntensity() == 0) {
            return new Color(0, 0, 0);
        }
        return new Color(red, green, blue);
    }
}
