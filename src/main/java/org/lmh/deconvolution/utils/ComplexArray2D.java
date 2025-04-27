package org.lmh.deconvolution.utils;

public class ComplexArray2D {
    public double[][] real;
    public double[][] imag;

    public ComplexArray2D(int width, int height) {
        real = new double[height][width];
        imag = new double[height][width];
    }

    public int getWidth() {
        return real[0].length;
    }

    public int getHeight() {
        return real.length;
    }
}
