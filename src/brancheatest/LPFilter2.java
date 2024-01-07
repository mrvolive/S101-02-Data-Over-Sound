public class LPFilter2 {
     /**
     * Apply an Exponential Moving Average Low-Pass Filter to the audio signal.
     * 
     * @param cutOffFrequency The smoothing factor used in the EMA, between 0 and 1. A smaller alpha means more smoothing (lower cutoff frequency).
     */
    public double[] lpFilter(double[] audio, double cutOffFrequency) {
        if (audio == null || audio.length == 0) {
            throw new IllegalStateException("Audio data array is uninitialized or empty.");
        }

        if (cutOffFrequency < 0 || cutOffFrequency > 1) {
            throw new IllegalArgumentException("cutOffFrequency should be between 0 and 1.");
        }

        double[] filteredAudio = new double[audio.length];
        // Start with the first value as the initial condition for the filtered signal.
        filteredAudio[0] = audio[0];

        // Apply the Exponential Moving Average filter to the rest of the audio samples.
        for (int i = 1; i < audio.length; i++) {
            // EMA formula: filteredValue = cutOffFrequency * currentValue + (1 - cutOffFrequency) * previousFilteredValue
            filteredAudio[i] = cutOffFrequency * audio[i] + (1 - cutOffFrequency) * filteredAudio[i - 1];
        }

        // Replace the original audio array with the filtered one.
        return filteredAudio;
    }
}