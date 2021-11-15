import DataAugmentation.Processors.CannyEdgeDetector;
import DataAugmentation.Processors.HistogramProcessor;
import DataAugmentation.Processors.OctaveProcessor;
import DataAugmentation.ImageProcessing.FlipFlopImageLoader;
import DataAugmentation.ImageProcessing.ImageLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Objects;
import java.util.TreeMap;

public class Main {
    public static void main(String[] args) throws IOException {
        System.out.println("Augmenting the data...");
        BufferedReader reader = new BufferedReader(new FileReader("ProcessingParameters.txt"));
        TreeMap<String, String> parameters = new TreeMap<>();

        String line = reader.readLine();
        while (line != null) {
            String key = line.substring(0, line.indexOf(":"));
            String value = line.substring(line.indexOf(":") + 2);
            parameters.put(key, value);
            line = reader.readLine();
        }

        double sigma = Double.parseDouble(parameters.get("Sigma"));
        double k = Math.sqrt(Double.parseDouble(parameters.get("K2")));
        double intensityThreshold = Double.parseDouble(parameters.get("IntensityThreshold"));
        int numberOfOctaves = Integer.parseInt(parameters.get("NumberOfOctaves"));
        int numberOfScales = Integer.parseInt(parameters.get("NumberOfScales"));

        File folder = new File("Train_Images");
        HistogramProcessor histogramProcessor = new HistogramProcessor();
        histogramProcessor.loadTargetImage(new File("Train_Images/ID_LG4BLEN9.jpg"));
        CannyEdgeDetector cannyEdgeDetector = new CannyEdgeDetector();
        OctaveProcessor octaveProcessor = new OctaveProcessor(sigma, k, intensityThreshold, numberOfOctaves, numberOfScales);

        for (File image: Objects.requireNonNull(folder.listFiles())) {
            if (image.getName().contains(".jpg")) {
                ImageLoader imageLoader = new FlipFlopImageLoader(image);
                imageLoader.setFolderName("AugmentedImages");
                histogramProcessor.sourceImageLoader(imageLoader);
                histogramProcessor.processImage();
                ImageLoader edge = imageLoader.clone();
                cannyEdgeDetector.sourceImageLoader(edge);
                cannyEdgeDetector.processImage();
                ImageLoader octave = imageLoader.clone();
                octaveProcessor.setImageLoader(octave);
                octaveProcessor.setEdgeImageLoader(edge);
                octaveProcessor.processImage();
                octaveProcessor.saveImage();
            }
        }
    }
}

