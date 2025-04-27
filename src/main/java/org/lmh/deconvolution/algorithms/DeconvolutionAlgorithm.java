package org.lmh.deconvolution.algorithms;

import ij.process.ImageProcessor;
import org.lmh.deconvolution.ProcessingCallback;

public abstract class DeconvolutionAlgorithm {
    ImageProcessor ip;
    int psfSize;
    int iterations;
    public abstract void run(ProcessingCallback callback);
}
