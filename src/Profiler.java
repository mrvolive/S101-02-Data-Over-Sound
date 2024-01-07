
import java.util.function.Function;

public class Profiler {

    static long globalTime;

    static long totalCalls;
    static void init(){
        globalTime = 0;
        totalCalls = 0;
    }

    @FunctionalInterface
    /**
     * IntFloat4Consumer

     void apply(int n, float xa,float ya, float xb, float yb)  */
    public interface Double4Consumer {
        void apply(double[] inputSignal, double sampleFreq, double cutoffFreq);
    }

    static void analyse(Double4Consumer oneMethod, double[] inputSignal, double sampleFreq, double cutoffFreq){
        long start = timestamp();
        oneMethod.apply(inputSignal, sampleFreq, cutoffFreq);
        globalTime += timestamp() - start;
        totalCalls++;
        //System.out.println(timestamp(start));
    }



    static void getGlobalTime(){
        String result;
        double elapsed = globalTime / 1e9;
        String unit = "s";
        if (elapsed < 1.0) {
            elapsed *= 1000.0;
            unit = "ms";
        }
        result = String.format("%.4g%s elapsed", elapsed, unit);
        System.out.println(result);
    }
    static void getTotalCalls(){
        System.out.println(totalCalls);
    }


    /**
     * Si clock0 est >0, retourne une chaîne de caractères
     * représentant la différence de temps depuis clock0.
     * @param clock0 instant initial
     * @return expression du temps écoulé depuis clock0
     */
    public static String timestamp(long clock0) {
        String result = null;

        if (clock0 > 0) {
            double elapsed = (System.nanoTime() - clock0) / 1e9;
            String unit = "s";
            if (elapsed < 1.0) {
                elapsed *= 1000.0;
                unit = "ms";
            }
            result = String.format("%.4g%s elapsed", elapsed, unit);
        }
        return result;
    }

    /**
     * retourne l'heure courante en ns.
     * @return
     */
    public static long timestamp() {
        return System.nanoTime();
    }
}