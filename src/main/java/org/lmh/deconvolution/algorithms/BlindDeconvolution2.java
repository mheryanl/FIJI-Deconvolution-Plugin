package org.lmh.deconvolution.algorithms;

import ij.ImagePlus;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import org.lmh.deconvolution.ProcessingCallback;
import org.lmh.deconvolution.utils.ComplexArray2D;
import org.lmh.deconvolution.utils.FFTUtils;

public class BlindDeconvolution2 extends DeconvolutionAlgorithm {

    public BlindDeconvolution2(ImageProcessor ip, int psfSize, int iterations) {
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

        float[] rChannel = new float[width * height];
        float[] gChannel = new float[width * height];
        float[] bChannel = new float[width * height];
        for (int i = 0; i < rChannel.length; i++) {
            rChannel[i] = (origR[i] & 0xff) / 255f;
            gChannel[i] = (origG[i] & 0xff) / 255f;
            bChannel[i] = (origB[i] & 0xff) / 255f;
        }

        // Precompute PSF FFT once
        FloatProcessor psf = createPSF(psfSize);
        ComplexArray2D psfFFT = precomputePSF(psf, width, height);

        // Process each channel
        float[] latentR = rChannel.clone();
        processChannel(latentR, rChannel, psfFFT, width, height);

        float[] latentG = gChannel.clone();
        processChannel(latentG, gChannel, psfFFT, width, height);

        float[] latentB = bChannel.clone();
        processChannel(latentB, bChannel, psfFFT, width, height);

        // Combine back to RGB
        int[] resultPixels = new int[width * height];
        for (int i = 0; i < resultPixels.length; i++) {
            int r = (int) (latentR[i] * 255);
            int g = (int) (latentG[i] * 255);
            int b = (int) (latentB[i] * 255);

            r = Math.min(255, Math.max(0, r));
            g = Math.min(255, Math.max(0, g));
            b = Math.min(255, Math.max(0, b));

            resultPixels[i] = (255 << 24) | (r << 16) | (g << 8) | b;
        }

        ColorProcessor cp = new ColorProcessor(width, height);
        cp.setPixels(resultPixels);
        new ImagePlus("Deconvolved Image - FFT", cp).show();
        callback.onFinish();
    }

    private FloatProcessor createPSF(int psfSize) {
        FloatProcessor psf = new FloatProcessor(psfSize, psfSize);
        float value = 1f / (psfSize * psfSize);
        for (int y = 0; y < psfSize; y++) {
            for (int x = 0; x < psfSize; x++) {
                psf.setf(x, y, value);
            }
        }
        return psf;
    }

    private ComplexArray2D precomputePSF(FloatProcessor psfSmall, int width, int height) {
        int paddedSize = 1;
        while (paddedSize < Math.max(width, height)) paddedSize *= 2;

        ComplexArray2D psfComplex = new ComplexArray2D(paddedSize, paddedSize);
        int psfWidth = psfSmall.getWidth();
        int psfHeight = psfSmall.getHeight();
        for (int y = 0; y < psfHeight; y++) {
            for (int x = 0; x < psfWidth; x++) {
                psfComplex.real[y][x] = psfSmall.getf(x, y);
            }
        }
        FFTUtils.fft2D(psfComplex, false);
        return psfComplex;
    }

    private void processChannel(float[] latent, float[] observed, ComplexArray2D psfFFT, int width, int height) {
        for (int iter = 0; iter < iterations; iter++) {
            float[] conv = convolve(latent, psfFFT, width, height);
            for (int i = 0; i < latent.length; i++) {
                latent[i] *= observed[i] / (conv[i] + 1e-6f);
                latent[i] = Math.min(1f, Math.max(0f, latent[i]));
            }
        }
    }

    private float[] convolve(float[] image, ComplexArray2D psfFFT, int width, int height) {
        int paddedSize = 1;
        while (paddedSize < Math.max(width, height)) paddedSize *= 2;

        ComplexArray2D imageComplex = new ComplexArray2D(paddedSize, paddedSize);

        // Fill image
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                imageComplex.real[y][x] = image[y * width + x];
            }
        }

        // Forward FFT
        FFTUtils.fft2D(imageComplex, false);

        // Multiply with precomputed PSF FFT
        for (int y = 0; y < paddedSize; y++) {
            for (int x = 0; x < paddedSize; x++) {
                double a = imageComplex.real[y][x];
                double b = imageComplex.imag[y][x];
                double c = psfFFT.real[y][x];
                double d = psfFFT.imag[y][x];

                imageComplex.real[y][x] = a * c - b * d;
                imageComplex.imag[y][x] = a * d + b * c;
            }
        }

        // Inverse FFT
        FFTUtils.fft2D(imageComplex, true);

        // Extract real part cropped back to original size
        float[] result = new float[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result[y * width + x] = (float) imageComplex.real[y][x];
            }
        }

        return result;
    }
}
