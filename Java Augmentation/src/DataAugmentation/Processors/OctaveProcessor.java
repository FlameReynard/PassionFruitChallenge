package DataAugmentation.Processors;

import DataAugmentation.ImageProcessing.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class OctaveProcessor implements ImageProcessor {
    private ImageLoader imageLoader;
    private ImageLoader edgeImageLoader;
    private String imageName;
    protected final double sigma;
    protected final double k;
    protected final double intensityThreshold;
    protected final int numberOfOctaves;
    protected final int numberOfScales;

    public OctaveProcessor(double sigma, double k, double intensityThreshold, int numberOfOctaves, int numberOfScales) {
        this.sigma = sigma;
        this.k = k;
        this.intensityThreshold = intensityThreshold;
        this.numberOfOctaves = numberOfOctaves;
        this.numberOfScales = numberOfScales;
    }

    @Override
    public void loadImage(File image) throws IOException {
        this.imageLoader = new GreyScaleImageLoader(image);
        this.imageName = image.getName();
        this.imageName = this.imageName.substring(0, this.imageName.indexOf("."));
    }

    public void setImageLoader(ImageLoader imageLoader) {
        this.imageLoader = imageLoader;
        this.imageName = imageLoader.getName();
        this.imageName = this.imageName.substring(0, this.imageName.indexOf("."));
    }

    public void setEdgeImageLoader(ImageLoader imageLoader) {
        edgeImageLoader = imageLoader;
    }

    @Override
    public void processImage() {
        ArrayList<ArrayList<double[][]>> octaves = calculateOctaves(imageLoader);
        octaves = getDifferences(octaves);
        octaves = subtractInverse(octaves);
        octaves = matchWithEdges(octaves);

        ArrayList<double[][]> octave = octaves.get(octaves.size() - 1);
        double[][] scale = octave.get(0);
        scale = normaliseIntensities(scale, 0, 255);
        int numberOfX = scale.length;
        int numberOfY = scale[0].length;
        Pixel[][] pixels = imageLoader.getPixels();
        for (int x = 0; x < numberOfX; x++) {
            for (int y = 0; y < numberOfY; y++) {
                pixels[x][y].setIntensity((int) Math.round(scale[x][y]));
            }
        }
    }

    private ArrayList<ArrayList<double[][]>> matchWithEdges(ArrayList<ArrayList<double[][]>> octaves) {
        double[][] edges = edgeImageLoader.getIntensities();
        ArrayList<ArrayList<double[][]>> vals = new ArrayList<>();
        for (ArrayList<double[][]> octave : octaves) {
            ArrayList<double[][]> octaveVals = new ArrayList<>();
            for (double[][] scale : octave) {
                int numberOfX = scale.length;
                int numberOfY = scale[0].length;
                double[][] newVals = new double[numberOfX][numberOfY];
                for (int x = 0; x < numberOfX; x++) {
                    for (int y = 0; y < numberOfY; y++) {
                        newVals[x][y] = edges[x][y] == 0 ? scale[x][y] : 0;
                    }
                }
                octaveVals.add(newVals);
            }
            vals.add(octaveVals);
        }
        return vals;
    }

    private ArrayList<ArrayList<double[][]>> subtractInverse(ArrayList<ArrayList<double[][]>> differenceOfGaussian) {
        ArrayList<ArrayList<double[][]>> vals = new ArrayList<>();
        for (ArrayList<double[][]> octave : differenceOfGaussian) {
            ArrayList<double[][]> octaveVals = new ArrayList<>();
            for (double[][] scale : octave) {
                int numberOfX = scale.length;
                int numberOfY = scale[0].length;
                double[][] newVals = new double[numberOfX][numberOfY];
                for (int x = 0; x < numberOfX; x++) {
                    for (int y = 0; y < numberOfY; y++) {
                        newVals[x][y] = Math.abs(1 - 2 * scale[x][y]);
                    }
                }
                octaveVals.add(newVals);
            }
            vals.add(octaveVals);
        }
        return vals;
    }

    @Override
    public void saveImage() throws IOException {
        imageLoader.saveImage();
    }

    private ArrayList<ArrayList<double[][]>> calculateOctaves(ImageLoader imageLoader) {
        ArrayList<ArrayList<double[][]>> octaves = new ArrayList<>();

        double nextSigma = sigma;
        for (int octaveNum = 0; octaveNum < numberOfOctaves; octaveNum++) {
            ImageLoader octaveLoader = imageLoader.clone();
            octaveLoader.scale(Math.pow(0.5, octaveNum)); //Each octave is half the size in x and y of the previous one

            double[][] oldIntensities = octaveLoader.getIntensities();
            ArrayList<double[][]> octave = new ArrayList<>();
            double scaledSigma = nextSigma;
            for (int scale = 0; scale < numberOfScales; scale++) {
                double[][] newIntensities = applyGaussianBlur(oldIntensities, scaledSigma);
                octave.add(newIntensities);
                oldIntensities = newIntensities;
                if (scale == numberOfScales / 2) {
                    nextSigma = scaledSigma;
                }
                scaledSigma *= k;
            }
            octaves.add(octave);
        }

        return octaves;
    }

    private ArrayList<ArrayList<double[][]>> getDifferences(ArrayList<ArrayList<double[][]>> blurs) {
        ArrayList<ArrayList<double[][]>> differences = new ArrayList<>();
        for (ArrayList<double[][]> octaveBlurs : blurs) {
            ArrayList<double[][]> scaleSubtracted = new ArrayList<>();

            for (int scaleNum = 0; scaleNum < octaveBlurs.size() - 1; scaleNum++) {
                double[][] scale = octaveBlurs.get(scaleNum);
                double[][] nextScale = octaveBlurs.get(scaleNum + 1);

                int numberOfX = scale.length;
                int numberOfY = scale[0].length;

                double[][] scaleDifference = new double[numberOfX][numberOfY];
                for (int x = 0; x < numberOfX; x++) {
                    for (int y = 0; y < numberOfY; y++) {
                        double diff = nextScale[x][y] - scale[x][y];
                        scaleDifference[x][y] = diff;
                    }
                }
                scaleDifference = normaliseIntensities(scaleDifference, 0, 1);
                scaleSubtracted.add(scaleDifference);
            }
            differences.add(scaleSubtracted);
        }

        return differences;
    }

    private ArrayList<ArrayList<double[][]>> getMaximaAndMinima(ArrayList<ArrayList<double[][]>> differenceOfGaussian) {
        ArrayList<ArrayList<double[][]>> maximaAndMinima = new ArrayList<>();
        for (ArrayList<double[][]> octave : differenceOfGaussian) {
            ArrayList<double[][]> octaveMaximaAndMinima = new ArrayList<>();
            for (int scaleNum = 1; scaleNum < octave.size() - 1; scaleNum++) {
                double[][] prevScale = octave.get(scaleNum);
                double[][] currentScale = octave.get(scaleNum);
                double[][] nextScale = octave.get(scaleNum + 1);

                int numberOfX = currentScale.length;
                int numberOfY = currentScale[0].length;
                double[][] scaleMaximaAndMinima = new double[numberOfX][numberOfY];
                for (int x = 0; x < numberOfX; x++) {
                    for (int y = 0; y < numberOfY; y++) {
                        boolean isMax = prevScale[x][y] < currentScale[x][y] && nextScale[x][y] < currentScale[x][y];
                        boolean isMin = prevScale[x][y] > currentScale[x][y] && nextScale[x][y] > currentScale[x][y];
                        scaleMaximaAndMinima[x][y] = (isMax || isMin) ? 1 : 0; //make sure it is the max/min between it and its neighbours
                    }
                }
                octaveMaximaAndMinima.add(scaleMaximaAndMinima);
            }
            maximaAndMinima.add(octaveMaximaAndMinima);
        }

        return maximaAndMinima;
    }

    private ArrayList<ArrayList<double[][]>> performOpening(ArrayList<ArrayList<double[][]>> octaves) {
        ArrayList<ArrayList<double[][]>> openings = new ArrayList<>();
        for (ArrayList<double[][]> octave : octaves) {
            ArrayList<double[][]> octaveOpenings = new ArrayList<>();
            for (double[][] scale : octave) {
                double[][] opening = scale;
                opening = performErosion(opening);
                opening = performDilation(opening);
                octaveOpenings.add(opening);
            }
            openings.add(octaveOpenings);
        }
        return openings;
    }

    private ArrayList<ArrayList<double[][]>> performClosing(ArrayList<ArrayList<double[][]>> octaves) {
        ArrayList<ArrayList<double[][]>> closings = new ArrayList<>();
        for (ArrayList<double[][]> octave : octaves) {
            ArrayList<double[][]> octaveClosings = new ArrayList<>();
            for (double[][] scale : octave) {
                double[][] closing = scale;
                closing = performDilation(closing);
                closing = performErosion(closing);
                octaveClosings.add(closing);
            }
            closings.add(octaveClosings);
        }
        return closings;
    }

    private double[][] performErosion(double[][] intensities) {
        int numberOfX = intensities.length;
        int numberOfY = intensities[0].length;
        double[][] erosion = new double[numberOfX][numberOfY];

        for (int x = 0; x < numberOfX; x++) {
            for (int y = 0; y < numberOfY; y++) {
                if (intensities[x][y] > 0) {
                    int lowerX = Math.max(x - 1, 0);
                    int upperX = Math.min(x + 1, numberOfX - 1);
                    int lowerY = Math.max(y - 1, 0);
                    int upperY = Math.min(y + 1, numberOfX - 1);

                    double sum = 0;
                    for (int erosionX = lowerX; erosionX <= upperX; erosionX++) {
                        for (int erosionY = lowerY; erosionY <= upperY; erosionY++) {
                            if (erosionX == x || erosionY == y) {
                                sum += (intensities[erosionX][erosionY] > 0) ? 1 : 0;
                            }
                        }
                    }
                    if (sum >= 5) {
                        erosion[x][y] = 255;
                    }
                }
            }
        }
        return erosion;
    }

    private double[][] performDilation(double[][] intensities) {
        int numberOfX = intensities.length;
        int numberOfY = intensities[0].length;
        double[][] dilation = new double[numberOfX][numberOfY];

        for (int x = 0; x < numberOfX; x++) {
            for (int y = 0; y < numberOfY; y++) {
                dilation[x][y] = intensities[x][y];
                int lowerX = Math.max(x - 1, 0);
                int upperX = Math.min(x + 1, numberOfX - 1);
                int lowerY = Math.max(y - 1, 0);
                int upperY = Math.min(y + 1, numberOfX - 1);

                if (intensities[x][y] > 0) {
                    for (int dilationX = lowerX; dilationX <= upperX; dilationX++) {
                        for (int dilationY = lowerY; dilationY <= upperY; dilationY++) {
                            if (dilationX == x || dilationY == y) {
                                dilation[dilationX][dilationY] = 255;
                            }
                        }
                    }
                }
            }
        }
        return dilation;
    }

    private double[][] applyGaussianBlur(double[][] oldIntensities, double sigma) {
        double[][] kernel = getGaussianKernel(3, sigma);
        return applyKernelFilter(kernel, oldIntensities);
    }

    private double[][] applyKernelFilter(double[][] kernel, double[][] oldIntensities) {
        double[][] newIntensities = new double[oldIntensities.length][oldIntensities[0].length];

        double total = 0;
        for (double[] row : kernel) {
            for (double weight : row) {
                total += Math.abs(weight);
            }
        }

        for (int x = 0; x < oldIntensities.length; x++) {
            for (int y = 0; y < oldIntensities[0].length; y++) {
                double sum = 0;
                for (int kx = 0; kx < 3; kx++) {
                    for (int ky = 0; ky < 3; ky++) {
                        int indexX = x - 1 + kx;
                        int indexY = y - 1 + ky;
                        if ((indexX >= 0 && indexY >= 0) && (indexX < oldIntensities.length && indexY < oldIntensities[0].length)) {
                            sum += kernel[kx][ky] * oldIntensities[indexX][indexY];
                        }
                    }
                }
                newIntensities[x][y] = sum / total;
            }
        }

        return newIntensities;
    }

    private double[][] getGaussianKernel(int size, double sigma) {
        double[][] kernel = new double[size][size];

        double sigmaSquared = Math.pow(sigma, 2);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                kernel[x][y] = (1 / (2 * Math.PI * sigmaSquared)) * Math.exp(-(Math.pow(x, 2) + Math.pow(y, 2)) / (2 * sigmaSquared));
            }
        }

        return kernel;
    }

    private void printImages(ArrayList<ArrayList<double[][]>> octaves) throws IOException {
        File base = new File(imageLoader.getFolderName());
        base.mkdir();
        String baseFolder = imageLoader.getFolderName() + "/";
        for (int octave = 0; octave < octaves.size(); octave++) {
            ImageLoader octaveLoader = imageLoader.clone();
            octaveLoader.scale(Math.pow(0.5, octave));
            ArrayList<double[][]> scales = octaves.get(octave);
            Pixel[][] pixels = octaveLoader.getPixels();

            String octaveFolder = "Octave" + octave;
            File folder = new File(baseFolder + octaveFolder);
            folder.mkdir();

            String filename = octaveFolder + "/Scale" + imageName;
            for (int scale = 0; scale < scales.size(); scale++) {
                double[][] intensities = normaliseIntensities(scales.get(scale), 0, 255);
                for (int x = 0; x < intensities.length; x++) {
                    for (int y = 0; y < intensities[0].length; y++) {
                        pixels[x][y].setIntensity((int) Math.round(intensities[x][y]));
                    }
                }
                octaveLoader.saveImage(filename + scale);
            }
        }
    }

    private double[][] normaliseIntensities(double[][] intensities, double newMin, double newMax) {
        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;
        for (double[] intensityRow : intensities) {
            for (double intensity : intensityRow) {
                if (intensity > max) {
                    max = intensity;
                }
                if (intensity < min) {
                    min = intensity;
                }
            }
        }

        double[][] rescaled = new double[intensities.length][intensities[0].length];
        for (int x = 0; x < rescaled.length; x++) {
            for (int y = 0; y < rescaled[0].length; y++) {
                double oldIntensity = intensities[x][y];
                rescaled[x][y] = (oldIntensity - min) / (max - min) * (newMax - newMin) + newMin; //scale the value to the new range
            }
        }

        return rescaled;
    }
}
