
public class LPFilter1 {
    /**
     * Apply a Low-Pass Filter to the audio signal by averaging the samples around each sample.
     * @param inputSignal
     * @param sampleFreq
     * @param cutoffFreq
     * @return
     */
    public double[] lpFilter(double[] inputSignal,double sampleFreq,double cutoffFreq) {
        // Found it by trial.
        int n = (int) cutoffFreq;
        if (inputSignal == null || inputSignal.length == 0) {
            throw new IllegalStateException("Audio data array is uninitialized or empty.");
        }

        if (n <= 0) {
            throw new IllegalArgumentException("n should be greater than 0.");
        }

        double[] filteredAudio = new double[inputSignal.length];

        // Calculating the average of 'n' samples around each sample
        for (int i = 0; i < inputSignal.length; i++) {
            double sum = 0.0;
            int count = 0;

            // Summing up 'n' samples around the current sample
            // j is initialized to max(0, i - n/2) to handle edge cases
            // j is limited to min(inputSignal.length, i + n/2) to handle edge cases
            for (int j = Math.max(0, i - n / 2); j < Math.min(inputSignal.length, i + n / 2); j++) {
                sum += inputSignal[j];
                count++;
            }

            // Calculating the average
            filteredAudio[i] = sum / count;
        }

        // Replacing the original audio array with the filtered one
        return filteredAudio;
    }
}
