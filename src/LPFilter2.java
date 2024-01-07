
public class LPFilter2 {
     /**
     * Apply an Exponential Moving Average Low-Pass Filter to the audio signal.
     * 
     * @param cutOffFrequency The smoothing factor used in the EMA, between 0 and 1. A smaller alpha means more smoothing (lower cutoff frequency).
     */
     public double[] lpFilter(double[] inputSignal,double sampleFreq,double cutoffFreq) {
        if (inputSignal == null || inputSignal.length == 0) {
            throw new IllegalStateException("Audio data array is empty.");
        }
        if (cutoffFreq < 0 || cutoffFreq > 1) {
            throw new IllegalArgumentException("cutoffFreq should be between 0 and 1.");
        }
        double[] filteredAudio = new double[inputSignal.length];
        // Start with the first value as the initial condition for the filtered signal.
        filteredAudio[0] = inputSignal[0];
        // Apply the Exponential Moving Average filter to the rest of the audio samples.
        for (int i = 1; i < inputSignal.length; i++) {
            // EMA formula: filteredValue = cutOffFrequency * currentValue + (1 - cutOffFrequency) * previousFilteredValue
            filteredAudio[i] = cutoffFreq * inputSignal[i] + (1 - cutoffFreq) * filteredAudio[i - 1];
        }
        // Replace the original audio array with the filtered one.
        return filteredAudio;
    }
}
