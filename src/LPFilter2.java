public class LPFilter2 {
    /**
     * Applies a low-pass filter to the input signal. Exponential Moving Average (EMA).
     *
     * @param inputSignal The input signal array.
     * @param sampleFreq  The sampling frequency of the input signal.
     * @param cutoffFreq  The cutoff frequency of the low-pass filter.
     * @return The filtered signal array.
     */
    public double[] lpFilter(double[] inputSignal, double sampleFreq, double cutoffFreq) {
        double rc = 1.0 / (cutoffFreq * 2 * Math.PI);
        double dt = 1.0 / sampleFreq;
        double alpha = dt / (rc + dt);
        
        double[] outputSignal = new double[inputSignal.length];
        outputSignal[0] = inputSignal[0];
        
        for (int i = 1; i < inputSignal.length; i++) {
            // Apply the low-pass filter: discrete implementation of RC low-pass filter
            outputSignal[i] = outputSignal[i - 1] + alpha * (inputSignal[i] - outputSignal[i - 1]);
        }
        
        return outputSignal;
    }
}
