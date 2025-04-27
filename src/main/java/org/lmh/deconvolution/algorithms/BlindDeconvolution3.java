package org.lmh.deconvolution.algorithms;

import ij.ImagePlus;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import org.lmh.deconvolution.ProcessingCallback;

public class BlindDeconvolution3 extends DeconvolutionAlgorithm {

    public BlindDeconvolution3(ImageProcessor ip, int psfSize, int iterations) {
        this.ip = ip;
        this.psfSize = psfSize;
        this.iterations = iterations;
    }

    @Override
    public void run(ProcessingCallback callback) {
        int width = ip.getWidth();
        int height = ip.getHeight();
        byte[] origR = new byte[width * height];
        byte[] origG = new byte[width * height];
        byte[] origB = new byte[width * height];
        ((ColorProcessor) ip).getRGB(origR, origG, origB);

        // Process each channel separately
        float[] rChannel = new float[width * height];
        float[] gChannel = new float[width * height];
        float[] bChannel = new float[width * height];

        // Convert byte arrays to float arrays (normalized to 0-1)
        for (int i = 0; i < rChannel.length; i++) {
            rChannel[i] = (origR[i] & 0xff) / 255f;
            gChannel[i] = (origG[i] & 0xff) / 255f;
            bChannel[i] = (origB[i] & 0xff) / 255f;
        }

        float[] psf1D = new float[psfSize];
        for (int i = 0; i < psfSize; i++) psf1D[i] = 1f / psfSize;

        // Process red channel
        float[] latentR = rChannel.clone();
        processChannel(latentR, rChannel, psf1D, width, height);

        // Process green channel
        float[] latentG = gChannel.clone();
        processChannel(latentG, gChannel, psf1D, width, height);

        // Process blue channel
        float[] latentB = bChannel.clone();
        processChannel(latentB, bChannel, psf1D, width, height);

        // Convert the result back to an RGB int array for ColorProcessor
        int[] resultPixels = new int[width * height];
        for (int i = 0; i < resultPixels.length; i++) {
            int r = (int)(latentR[i] * 255);
            int g = (int)(latentG[i] * 255);
            int b = (int)(latentB[i] * 255);

            // Create RGB value with original alpha
            resultPixels[i] = (255 << 24) | (r << 16) | (g << 8) | b;
        }

        ColorProcessor cp = new ColorProcessor(width, height);
        cp.setPixels(resultPixels);
        new ImagePlus("Deconvolved Image - Separable", cp).show();
        callback.onFinish();
    }

    private void processChannel(float[] latent, float[] original, float[] psf1D, int width, int height) {
        for (int it = 0; it < iterations; it++) {
            float[] conv = convolveSeparable(latent, psf1D, width, height);
            float[] ratio = new float[original.length];
            for (int i = 0; i < original.length; i++) {
                ratio[i] = original[i] / (conv[i] + 1e-6f);
            }
            float[] update = convolveSeparable(ratio, psf1D, width, height);
            for (int i = 0; i < latent.length; i++) {
                latent[i] *= update[i];
                latent[i] = Math.min(1f, Math.max(0f, latent[i]));
            }
        }
    }

    private float[] convolveSeparable(float[] image, float[] kernel1D, int width, int height) {
        int k = kernel1D.length / 2;
        float[] temp = new float[image.length];
        float[] result = new float[image.length];

        // Horizontal convolution
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float sum = 0;
                for (int i = -k; i <= k; i++) {
                    int xi = Math.min(width - 1, Math.max(0, x + i));
                    sum += image[y * width + xi] * kernel1D[i + k];
                }
                temp[y * width + x] = sum;
            }
        }

        // Vertical convolution
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float sum = 0;
                for (int j = -k; j <= k; j++) {
                    int yj = Math.min(height - 1, Math.max(0, y + j));
                    sum += temp[yj * width + x] * kernel1D[j + k];
                }
                result[y * width + x] = sum;
            }
        }

        return result;
    }
}