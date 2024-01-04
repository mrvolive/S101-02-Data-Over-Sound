public class LPFilter1 {

    public double[] lpFilter(double[] inputSignal, double sampleFreq, double cutoffFreq) {
        double[] outputSignal = new double[inputSignal.length];
        int n = (int) (sampleFreq / 1000);
        for (int i = n; i < inputSignal.length; i++) {
            double sum = 0;
            // Calculate the average of the previous n samples and the current sample
            for (int j = i - n; j <= i; j++) {
                sum += inputSignal[j];
            }
            // Calculate the average
            double average = sum / (n + 1);
            // Replace the current sample with the average only if the frequency is below the cutoff frequency
            //double currentFreq = (i > 0) ? Math.abs(audio[i] - audio[i - 1]) * sampleRate : 0;
            if (inputSignal[i] >= cutoffFreq) {
                inputSignal[i] = average;
            }
        }

        return outputSignal;
    }
}
