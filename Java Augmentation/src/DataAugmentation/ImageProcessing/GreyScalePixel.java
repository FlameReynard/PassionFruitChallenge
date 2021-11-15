package DataAugmentation.ImageProcessing;

import java.awt.*;

public class GreyScalePixel implements Pixel {
    private int intensity;
    private Colour colour;

    public GreyScalePixel() {
        intensity = 0;
    }

    @Override
    public int getIntensity() {
        return intensity;
    }

    @Override
    public void setIntensity(int intensity) {
        this.intensity = intensity;
    }

    @Override
    public void setColour(Colour colour) {
        this.colour = colour;
    }

    @Override
    public void fromRGB(int red, int green, int blue) {
        intensity = (red + green + blue) / 3;
    }

    @Override
    public Color toRGB() {
        Colour colour = this.colour == null ? Colour.NONE : this.colour;
        this.colour = null;
        try {
            switch (colour) {
                case RED:
                    return new Color(255, 0, 0);
                case GREEN:
                    return new Color(0, 255, 0);
                case BLUE:
                    return new Color(0, 0, 255);
                default:
                    return new Color(intensity, intensity, intensity);
            }
        } catch (Exception e) {
            System.out.println("Bad intensity value: " + intensity);
            throw e;
        }
    }
}
