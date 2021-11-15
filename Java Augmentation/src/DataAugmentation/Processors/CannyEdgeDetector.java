package DataAugmentation.Processors;

import DataAugmentation.ImageProcessing.GreyScaleImageLoader;
import DataAugmentation.ImageProcessing.ImageLoader;
import DataAugmentation.ImageProcessing.ImageProcessor;
import DataAugmentation.ImageProcessing.Pixel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class CannyEdgeDetector implements ImageProcessor {
    ImageLoader imageLoader;
    Pixel[][] pixels;
    double[][] oldIntensities;
    double minThreshold = 8;
    double maxThreshold = 18;
    boolean showIntermediateImages = false;

    public void setParameters(double minThreshold, double maxThreshold, boolean showIntermediateImages) {
        this.minThreshold = minThreshold;
        this.maxThreshold = maxThreshold;
        this.showIntermediateImages = showIntermediateImages;
    }

    @Override
    public void loadImage(File image) throws IOException {
        imageLoader = new GreyScaleImageLoader(image);
    }

    @Override
    public void processImage() throws IOException {
        pixels = imageLoader.getPixels();

        oldIntensities = imageLoader.getIntensities();
        applyGaussianBlur();
        if (showIntermediateImages) {
            imageLoader.saveImage("GaussianBlur.jpg");
        }

        double[][] angles = applySobelOperator();
        if (showIntermediateImages) {
            imageLoader.saveImage("SobelEdgeStrengths.jpg");
            for (int x = 0; x < angles.length; x++) {
                for (int y = 0; y < angles[0].length; y++) {
                    pixels[x][y].setIntensity((int) Math.round(angles[x][y] / 360 * 255));
                }
            }
            imageLoader.saveImage("SobelEdgeDirections.jpg");
        }

        applyNonMaximumSuppression(angles);
        if (showIntermediateImages) {
            imageLoader.saveImage("NonMaximumSuppression.jpg");
        }

        ArrayList<String> vertexQueue = new ArrayList<>();
        applyDoubleThresholding(minThreshold, maxThreshold, vertexQueue);
        if (showIntermediateImages) {
            imageLoader.saveImage("DoubleThresholding.jpg");
        }

        applyHysteresis(vertexQueue);
        if (showIntermediateImages) {
            imageLoader.saveImage("Hysteresis.jpg");
        }
    }

    @Override
    public void saveImage() throws IOException {
        imageLoader.saveProcessedImage();
    }

    private void applyGaussianBlur() {
        double[][] kernel = getGaussianKernel();
        oldIntensities = applyKernelFilter(kernel);
        for (int x = 0; x < oldIntensities.length; x++) {
            for (int y = 0; y < oldIntensities[0].length; y++) {
                pixels[x][y].setIntensity((int) Math.round(oldIntensities[x][y]));
            }
        }
    }

    private double[][] applySobelOperator() {
        double[][][] kernels = getSobelKernels();
        double[][] xKernel = kernels[0];
        double[][] yKernel = kernels[1];

        double[][] xFiltered = applyKernelFilter(xKernel);
        double[][] yFiltered = applyKernelFilter(yKernel);

        double[][] angles = new double[xFiltered.length][xFiltered[0].length];
        for (int x = 0; x < oldIntensities.length; x++) {
            for (int y = 0; y < oldIntensities[0].length; y++) {
                double intensity = Math.sqrt(Math.pow(xFiltered[x][y], 2) + Math.pow(yFiltered[x][y], 2));
                double gradient = yFiltered[x][y] / xFiltered[x][y];
                double angle = getAngle(gradient);

                oldIntensities[x][y] = intensity;
                angles[x][y] = angle;
                pixels[x][y].setIntensity((int) Math.round(intensity));
            }
        }

        return angles;
    }

    private void applyNonMaximumSuppression(double[][] angles) {
        double[][] intensities = new double[oldIntensities.length][oldIntensities[0].length];
        for (int x = 0; x < oldIntensities.length; x++) {
            for (int y = 0; y < oldIntensities[0].length; y++) {
                double angle = angles[x][y];
                double intensity = oldIntensities[x][y];
                int mxLow = x;
                int mxHigh = x;
                int myLow = y;
                int myHigh = y;
                if ((angle > 337.5 || angle <= 22.5) || (angle > 157.5 && angle <= 202.5)) { //vertical line
                    myLow = Math.max(y - 1, 0);
                    myHigh = Math.min(y + 1, oldIntensities[0].length - 1);
                } else if ((angle > 22.5 && angle <= 67.5) || (angle > 202.5 && angle <= 247.5)) { //bottom left to top right
                    mxLow = Math.min(x + 1, oldIntensities.length - 1);
                    mxHigh = Math.max(x - 1, 0);
                    myLow = Math.max(y - 1, 0);
                    myHigh = Math.min(y + 1, oldIntensities[0].length - 1);
                } else if ((angle > 67.5 && angle <= 112.5) || (angle > 247.5 && angle <= 292.5)) { //horisontal line
                    mxLow = Math.max(x - 1, 0);
                    mxHigh = Math.min(x + 1, oldIntensities.length - 1);
                } else if ((angle > 112.5 && angle <= 157.5) || (angle > 292.5 && angle <= 337.5)) { //top left to bottom right
                    mxLow = Math.max(x - 1, 0);
                    mxHigh = Math.min(x + 1, oldIntensities.length - 1);
                    myLow = Math.max(y - 1, 0);
                    myHigh = Math.min(y + 1, oldIntensities[0].length - 1);
                } else {
                    System.out.println("Invalid angle: " + angle);
                }

                if ((intensity < oldIntensities[mxLow][myLow]) || (intensity < oldIntensities[mxHigh][myHigh])) {
                    intensity = 0;
                }

                intensities[x][y] = intensity;
                pixels[x][y].setIntensity((int) Math.round(intensity));
            }
        }

        oldIntensities = intensities;
    }

    private void applyDoubleThresholding(double minThreshold, double maxThreshold, ArrayList<String> vertexQueue) {
        double[][] intensities = new double[oldIntensities.length][oldIntensities[0].length];
        for (int x = 0; x < oldIntensities.length; x++) {
            for (int y = 0; y < oldIntensities[0].length; y++) {
                double intensity = oldIntensities[x][y];
                if (intensity < minThreshold) {
                    intensities[x][y] = 0;
                } else if (intensity > maxThreshold) {
                    intensities[x][y] = 255;
                    String vertex = x + "-" + y;
                    vertexQueue.add(vertex);
                } else {
                    intensities[x][y] = intensity;
                }
                pixels[x][y].setIntensity((int) Math.round(intensities[x][y]));
            }
        }
        oldIntensities = intensities;
    }

    private void applyHysteresis(ArrayList<String> vertexQueue) {
        int index = 0;
        while (index < vertexQueue.size()) {
            String coordinates = vertexQueue.get(index);
            int xCoordinate = Integer.parseInt(coordinates.substring(0, coordinates.indexOf("-")));
            int yCoordinate = Integer.parseInt(coordinates.substring(coordinates.indexOf("-") + 1));
            for (int x = xCoordinate - 1; x <= xCoordinate + 1; x++) {
                for (int y = yCoordinate - 1; y <= yCoordinate + 1; y++) {
                    if ((x >= 0 && y >= 0) && (x < oldIntensities.length && y < oldIntensities[0].length)) { //index bounds check
                        if (oldIntensities[x][y] == 0) {
                            continue;
                        }
                        String newCoordinate = x + "-" + y;
                        if (oldIntensities[x][y] != 255) {
                            vertexQueue.add(newCoordinate);
                        }
                        oldIntensities[x][y] = 255;
                    }
                }
            }
            index++;
        }

        for (int x = 0; x < oldIntensities.length; x++) {
            for (int y = 0; y < oldIntensities[0].length; y++) {
                if (oldIntensities[x][y] != 255) {
                    oldIntensities[x][y] = 0;
                }
                pixels[x][y].setIntensity((int) Math.round(oldIntensities[x][y]));
            }
        }
    }

    private double getAngle(double gradient) {
        if (Double.isNaN(gradient)) {
            gradient = 0;
        } else if (Double.isInfinite(gradient)) {
            gradient = Double.MAX_VALUE;
        }
        double direction = Math.pow(Math.atan(gradient), 1);
        double angle = Math.toDegrees(direction);
        while (angle < 0) {
            angle += 360;
        }
        while (angle > 360) {
            angle -= 360;
        }

        return angle;
    }

    private double[][] getGaussianKernel() {
        double[][] kernel = new double[3][3];
        kernel[0][0] = 1;
        kernel[0][1] = 2;
        kernel[0][2] = 1;
        kernel[1][0] = 2;
        kernel[1][1] = 4;
        kernel[1][2] = 2;
        kernel[2][0] = 1;
        kernel[2][1] = 2;
        kernel[2][2] = 1;

        return kernel;
    }

    private double[][][] getSobelKernels() {
        double[][] xKernel = new double[3][3];
        double[][] yKernel = new double[3][3];
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                int multiplier = x % 2 + 1;
                xKernel[x][y] = multiplier * (y - 1);

                multiplier = y % 2 + 1;
                yKernel[x][y] = multiplier * (x - 1);
            }
        }

        double[][][] kernels = new double[2][][];
        kernels[0] = xKernel;
        kernels[1] = yKernel;

        return kernels;
    }

    private double[][] applyKernelFilter(double[][] kernel) {
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
                newIntensities[x][y] = (int) Math.round(sum / total);
            }
        }

        return newIntensities;
    }

    public void sourceImageLoader(ImageLoader imageLoader) {
        this.imageLoader = imageLoader;
    }
}
