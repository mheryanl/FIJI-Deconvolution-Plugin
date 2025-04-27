package org.lmh.deconvolution.algorithms;

import ij.ImagePlus;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import org.lmh.deconvolution.ProcessingCallback;

public class BlindDeconvolution1 extends DeconvolutionAlgorithm {

    public BlindDeconvolution1(ImageProcessor ip, int psfSize, int iterations) {
        this.ip = ip;
        this.psfSize = psfSize;
        this.iterations = iterations;
    }

    @Override
    public void run(ProcessingCallback callback) {
        int width = ip.getWidth();
        int height = ip.getHeight();

        // Extract RGB channels
        byte[] R = new byte[width * height];
        byte[] G = new byte[width * height];
        byte[] B = new byte[width * height];
        ((ColorProcessor) ip).getRGB(R, G, B);

        // Convert to grayscale float
        float[] gray = new float[width * height];
        for (int i = 0; i < gray.length; i++) {
            gray[i] = ((R[i] & 0xff) + (G[i] & 0xff) + (B[i] & 0xff)) / 3f / 255f;
        }

        // Create PSF
        float[] psf = new float[psfSize * psfSize];
        for (int i = 0; i < psf.length; i++) psf[i] = 1f / psf.length;

        // Deconvolve grayscale image
        float[] latent = gray.clone();
        for (int it = 0; it < iterations; it++) {
            float[] conv = convolve(latent, psf, width, height, psfSize);
            float[] ratio = new float[gray.length];
            for (int i = 0; i < gray.length; i++) {
                ratio[i] = gray[i] / (conv[i] + 1e-6f);
            }
            float[] update = convolve(ratio, psf, width, height, psfSize);
            for (int i = 0; i < latent.length; i++) {
                latent[i] *= update[i];
                latent[i] = Math.min(1f, Math.max(0f, latent[i]));
            }
        }

        // Reapply to each channel
        byte[] resultR = deconvolveChannel(R, psf, width, height, psfSize);
        byte[] resultG = deconvolveChannel(G, psf, width, height, psfSize);
        byte[] resultB = deconvolveChannel(B, psf, width, height, psfSize);

        ColorProcessor cp = new ColorProcessor(width, height);
        cp.setRGB(resultR, resultG, resultB);
        new ImagePlus("Deconvolved Image - Direct", cp).show();
        callback.onFinish();
    }

    private byte[] deconvolveChannel(byte[] channel, float[] psf, int width, int height, int psfSize) {
        float[] src = new float[channel.length];
        for (int i = 0; i < src.length; i++) src[i] = (channel[i] & 0xff) / 255f;
        float[] latent = src.clone();

        for (int it = 0; it < 10; it++) {
            float[] conv = convolve(latent, psf, width, height, psfSize);
            float[] ratio = new float[src.length];
            for (int i = 0; i < src.length; i++) ratio[i] = src[i] / (conv[i] + 1e-6f);
            float[] update = convolve(ratio, psf, width, height, psfSize);
            for (int i = 0; i < latent.length; i++) {
                latent[i] *= update[i];
                latent[i] = Math.min(1f, Math.max(0f, latent[i]));
            }
        }

        byte[] result = new byte[latent.length];
        for (int i = 0; i < result.length; i++) result[i] = (byte) (latent[i] * 255);
        return result;
    }

    private float[] convolve(float[] image, float[] kernel, int width, int height, int kernelSize) {
        int k = kernelSize / 2;
        float[] result = new float[image.length];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float sum = 0;
                for (int j = -k; j <= k; j++) {
                    for (int i = -k; i <= k; i++) {
                        int xi = Math.min(width - 1, Math.max(0, x + i));
                        int yj = Math.min(height - 1, Math.max(0, y + j));
                        int ki = i + k;
                        int kj = j + k;
                        sum += image[yj * width + xi] * kernel[kj * kernelSize + ki];
                    }
                }
                result[y * width + x] = sum;
            }
        }
        return result;
    }
}

