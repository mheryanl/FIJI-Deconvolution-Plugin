package org.lmh.deconvolution.utils;

public class FFTUtils {

    public static void fft1D(double[] real, double[] imag, boolean inverse) {
        int n = real.length;
        if (n == 1) return;

        if ((n & (n - 1)) != 0)
            throw new IllegalArgumentException("FFT length must be power of 2");

        double[] evenReal = new double[n / 2];
        double[] evenImag = new double[n / 2];
        double[] oddReal = new double[n / 2];
        double[] oddImag = new double[n / 2];

        for (int i = 0; i < n / 2; i++) {
            evenReal[i] = real[2 * i];
            evenImag[i] = imag[2 * i];
            oddReal[i] = real[2 * i + 1];
            oddImag[i] = imag[2 * i + 1];
        }

        fft1D(evenReal, evenImag, inverse);
        fft1D(oddReal, oddImag, inverse);

        for (int k = 0; k < n / 2; k++) {
            double angle = 2 * Math.PI * k / n * (inverse ? 1 : -1);
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double tre = cos * oddReal[k] - sin * oddImag[k];
            double tim = sin * oddReal[k] + cos * oddImag[k];

            real[k] = evenReal[k] + tre;
            imag[k] = evenImag[k] + tim;
            real[k + n / 2] = evenReal[k] - tre;
            imag[k + n / 2] = evenImag[k] - tim;
        }

        if (inverse) {
            for (int i = 0; i < n; i++) {
                real[i] /= 2;
                imag[i] /= 2;
            }
        }
    }

    public static void fft2D(ComplexArray2D data, boolean inverse) {
        int width = data.getWidth();
        int height = data.getHeight();

        double[] realRow = new double[width];
        double[] imagRow = new double[width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                realRow[x] = data.real[y][x];
                imagRow[x] = data.imag[y][x];
            }
            fft1D(realRow, imagRow, inverse);
            for (int x = 0; x < width; x++) {
                data.real[y][x] = realRow[x];
                data.imag[y][x] = imagRow[x];
            }
        }

        double[] realCol = new double[height];
        double[] imagCol = new double[height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                realCol[y] = data.real[y][x];
                imagCol[y] = data.imag[y][x];
            }
            fft1D(realCol, imagCol, inverse);
            for (int y = 0; y < height; y++) {
                data.real[y][x] = realCol[y];
                data.imag[y][x] = imagCol[y];
            }
        }
    }
}
