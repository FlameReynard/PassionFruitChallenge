package DataAugmentation.ImageProcessing;

import java.io.File;
import java.io.IOException;

public interface ImageProcessor {
    void loadImage(File image) throws IOException;
    void processImage() throws IOException;
    void saveImage() throws IOException;
}
