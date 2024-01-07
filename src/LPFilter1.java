public class LPFilter1 {
    public double[] lpFilter(double[] audio, int n) {
        if (audio == null || audio.length == 0) {
            throw new IllegalStateException("Audio data array is uninitialized or empty.");
        }

        if (n <= 0) {
            throw new IllegalArgumentException("n should be greater than 0.");
        }
        
        double[] filteredAudio = new double[audio.length];
        
        // Calculating the average of 'n' samples around each sample
        for (int i = 0; i < audio.length; i++) {
            double sum = 0.0;
            int count = 0;
            
            // Summing up 'n' samples around the current sample
            for (int j = Math.max(0, i - n / 2); j < Math.min(audio.length, i + n / 2); j++) {
                sum += audio[j];
                count++;
            }
            
            // Calculating the average
            filteredAudio[i] = sum / count;
        }
        
        // Replacing the original audio array with the filtered one
        return filteredAudio;
    }
}