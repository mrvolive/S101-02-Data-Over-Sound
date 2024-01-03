import java.util.Arrays;

public class LPFilter2 {
    public double[] lpFilter(double[] inputSignal, double sF, double cutoff) {
        int N = inputSignal.length;
        int M = N / 2;
        double xm1 = 0;
        double[] outputSignal = new double[N];
        xm1 = simplp(inputSignal, outputSignal, M, xm1);
        return outputSignal;
    }
    private double simplp(double[] x, double[] y, int M, double xm1) {
        y[0] = x[0] + xm1;
        for (int n = 1; n < M; n++) {
            y[n] = x[n] + x[n - 1];
        }
        return x[M - 1];
    }
}
