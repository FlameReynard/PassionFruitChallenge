package DataAugmentation.ImageProcessing;

import java.awt.*;

public interface Pixel {
    enum Colour {
        RED,
        GREEN,
        BLUE,
        NONE,
    }
    void fromRGB(int red, int green, int blue);
    Color toRGB();
    int getIntensity();
    void setIntensity(int intensity);
    void setColour(Colour colour);
}
