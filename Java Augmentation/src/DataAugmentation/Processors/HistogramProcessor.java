package DataAugmentation.Processors;

import DataAugmentation.ImageProcessing.GreyScaleImageLoader;
import DataAugmentation.ImageProcessing.ImageLoader;
import DataAugmentation.ImageProcessing.ImageProcessor;
import DataAugmentation.ImageProcessing.Pixel;

import java.io.File;
import java.io.IOException;

public class HistogramProcessor implements ImageProcessor {
    ImageLoader imageLoader;
    GreyScaleImageLoader targetImageLoader;

    public void loadTargetImage(File image) throws IOException {
        targetImageLoader = new GreyScaleImageLoader(image);
    }

    @Override
    public void loadImage(File image) throws IOException {
        imageLoader = new GreyScaleImageLoader(image);
    }

    @Override
    public void saveImage() throws IOException {
        imageLoader.saveImage();
    }

    @Override
    public void processImage() {
        int[] sourceHistogram = imageLoader.getHistogram();
        int[] histogram = targetImageLoader.getHistogram();

        assert histogram.length == sourceHistogram.length;

        int intensityLevels = histogram.length;
        int totalPixels = targetImageLoader.getNumberOfPixels();
        int totalSourcePixels = imageLoader.getNumberOfPixels();

        int[] equalisedHistogram = equaliseHistogram(histogram, intensityLevels, totalPixels);
        int[] sourceEqualised = equaliseHistogram(sourceHistogram, intensityLevels, totalSourcePixels);

        Pixel[][] pixels = imageLoader.getPixels();

        for (Pixel[] pixel : pixels) {
            for (int y = 0; y < pixels[0].length; y++) {
                int intensity = pixel[y].getIntensity();
                int equalisedIntensity = sourceEqualised[intensity];
                int newIntensity = mapIntensity(equalisedHistogram, equalisedIntensity, intensity, intensityLevels);

                pixel[y].setIntensity(newIntensity);
            }
        }

//        imageLoader.saveProcessedImage();
    }

    private int[] equaliseHistogram(int[] histogram, int intensityLevels, int totalPixels) {
        double[] cumulativeHistogram = cumulative(histogram, intensityLevels, totalPixels);
        int[] equalised = new int[intensityLevels];

        for (int i = 0; i < intensityLevels; i++) {
            equalised[i] = -1;
        }

        for (int i = 0; i < intensityLevels; i++) {
            equalised[i] = (int) ((intensityLevels - 1) * cumulativeHistogram[i]);
        }

        return equalised;
    }

    private double[] cumulative(int[] histogram, int intensityLevels, int totalPixels) {
        double[] cumulative = new double[intensityLevels];
        double previousProbability = 0;

        for (int i = 0; i < intensityLevels; i++) {
            cumulative[i] = previousProbability + histogram[i] / (double) totalPixels;
            previousProbability = cumulative[i];
        }

        return cumulative;
    }

    private int mapIntensity(int[] equalisedHistogram, int intensity, int originalIntensity, int intensityLevels) {
        int closestIntensity = equalisedHistogram[intensity];
        int closestIndex = intensity;

        for (int i = 0; i < intensityLevels; i++) {
            int compareIntensity = equalisedHistogram[i];
            int compareDistance = Math.abs(compareIntensity - intensity);
            int closestDistance = Math.abs(closestIntensity - intensity);

            if (compareDistance <= closestDistance && compareIntensity != 1) {
                if (compareDistance == closestDistance) {
                    int compareIndex = Math.abs(i - originalIntensity);
                    int closeIndex = Math.abs(closestIndex - originalIntensity);

                    if (compareIndex < closeIndex) {
                        closestIndex = i;
                        closestIntensity = compareIntensity;
                    }
                }
                else {
                    closestIndex = i;
                    closestIntensity = compareIntensity;
                }
            }
        }

        return equalisedHistogram[closestIndex];
    }

    public void sourceImageLoader(ImageLoader imageLoader) {
        this.imageLoader = imageLoader;
    }
}
