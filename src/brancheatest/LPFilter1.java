public class LPFilter1 {
    public double[] lpFilter(double[] inputSignal, double sampleFreq, double cutoffFreq) {
        int n = calculeLongueurFiltre(sampleFreq, cutoffFreq);
        double[] outputSignal = inputSignal.clone();
        for (int i = n; i < inputSignal.length; i++) {
            double sum = 0;
            for (int j = i - n; j <= i; j++) {
                sum += inputSignal[j];
            }
            outputSignal[i] = sum / (n + 1);
        }
        return outputSignal;
    }
    private int calculeLongueurFiltre(double sampleFreq, double cutoffFreq) {
        return (int) (sampleFreq / (2 * cutoffFreq));
    }
}