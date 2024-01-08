/** MARAVAL Olivier
 * KHERZA Yahia
 * S1-A1
 */


import java.io.*;


public class DosRead {
    static final int FP = 1000;
    static final int BAUDS = 100;
    static final int[] START_SEQ = {1, 0, 1, 0, 1, 0, 1, 0};
    FileInputStream fileInputStream;
    int sampleRate = 44100;
    int bitsPerSample;
    int dataSize;
    double[] audio;
    int[] outputBits;
    char[] decodedChars;

    /**
     * Constructor that opens the FIlEInputStream
     * and reads sampleRate, bitsPerSample and dataSize
     * from the header of the wav file
     *
     * @param path the path of the wav file to read
     */
    public void readWavHeader(String path) {
        byte[] header = new byte[44]; // The header is 44 bytes long
        try {
            fileInputStream = new FileInputStream(path);
            fileInputStream.read(header);
            sampleRate = byteArrayToInt(header, 24, 32);
            // Convert the 4 bytes (starting from index 24)
            // to int to get the sampling rate (32 bits = 4 bytes)
            bitsPerSample = byteArrayToInt(header, 34, 16);
            // Convert the 2 bytes (starting from index 34) to retrieve
            // the number of bits per sample
            dataSize = byteArrayToInt(header, 40, 32);
            // Convert the 4 bytes (starting from index 40) to retrieve
            // the size of the file
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Helper method to convert a little-endian byte array to an integer
     *
     * @param bytes  the byte array to convert
     * @param offset the offset in the byte array
     * @param fmt    the format of the integer (16 or 32 bits)
     * @return the integer value
     */
    private static int byteArrayToInt(byte[] bytes, int offset, int fmt) {
        if (fmt == 16){
            return ((bytes[offset + 1] & 0xFF) << 8) | (bytes[offset] & 0xFF);
        }
        else if (fmt == 32)
            return ((bytes[offset + 3] & 0xFF) << 24) |
                    ((bytes[offset + 2] & 0xFF) << 16) |
                    ((bytes[offset + 1] & 0xFF) << 8) |
                    (bytes[offset] & 0xFF);
        else return (bytes[offset] & 0xFF);
    }

    /**
     * Read the audio data from the wav file
     * and convert it to an array of doubles
     * that becomes the audio attribute
     */
    public void readAudioDouble() {
        byte[] audioData = new byte[dataSize];
        try {
            fileInputStream.read(audioData);
        } catch (IOException e) {
            e.printStackTrace();
        }
        audio = new double[dataSize]; // Because PCM integer (code1) is 2 bytes
        for (int i = 0; i < (dataSize / 2) - 44; i++) { // Fill the audio array with values in int coded on 2 bytes (16 bits)
            if (byteArrayToInt(audioData, 44 + 2 * i, 16) > 32767) { // Reset negative values to their original value
                audio[i] = byteArrayToInt(audioData, 44 + 2 * i, 16) - (double)65536;
            } else {
                audio[i] = byteArrayToInt(audioData, 44 + 2 * i, 16);
            }
        }
    }


    /**
     * Reverse the negative values of the audio array
     */
    public void audioRectifier() {
        for (int i = 0; i < audio.length; i++) {
            if (audio[i] < 0) {
                audio[i] = -audio[i];
            }
        }
        // Check if a value is negative; if so,
        // obtain its absolute value
    }


    /**
     * Apply a low pass filter to the audio array
     * Fc = (1/2n)*FECH
     *
     * @param n the number of samples to average
     */

    private LPFilter1 lpFilter1 = new LPFilter1();
    private LPFilter2 lpFilter2 = new LPFilter2();
    // Choix du filtre.
    public void LPFilter(int n) {
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
            if(count != 0)
                filteredAudio[i] = sum / count;
        }
        // Replacing the original audio array with the filtered one
        audio = filteredAudio;
    
    }

    /**
     * Resample the audio array and apply a threshold
     * @param period the number of audio samples per symbol
     * @param threshold the threshold that separates 0 and 1
     **/
    public void audioResampleAndThreshold(int period, double threshold) {
        double[] inputSignal = audio.clone();
        // Initialize an array to store the averages of each period.
        double[] outputSignal = new double[inputSignal.length / period];
        // Initialize an array to store binary results (0 or 1) based on the threshold.
        outputBits = new int[inputSignal.length / period];
        // Main loop iterating through each period.
        for (int i = 0; i < inputSignal.length / period; i++) {
            // Initialize the sum of samples within the period to zero.
            double sum = 0;
            // Inner loop iterating through the samples inside each sub-period.
            for (int j = 0; j < period; j++) {
                // Accumulate values of the sub-period.
                sum += inputSignal[i * period + j];
            }
            // Calculate the average of the samples in the sub-period.
            outputSignal[i] = sum / period;
            // Apply a threshold.
            if (outputSignal[i] > threshold) {
                outputBits[i] = 1;
            } else {
                outputBits[i] = 0;
            }
        }
    }



    /**
     * Decode the outputBits array to a char array
     * The decoding is done by comparing the START_SEQ with the actual beginning of outputBits.
     * The next first symbol is the first bit of the first char.
     */
    public void decodeBitsToChar() {
        boolean corresponds = true;
        for (int i = 0; i < START_SEQ.length; i++) {
            if (outputBits[i] != START_SEQ[i]) {
                corresponds = false;
                System.out.println("START_SEQ does not correspond at index " + i);
                System.out.println("outputBits[" + i + "] = " + outputBits[i]);
                System.out.println("START_SEQ[" + i + "] = " + START_SEQ[i]);
                break;  // Exit the loop as soon as a mismatch is found
            }
        }

        if (corresponds) {
            int numberOfCharacters = (outputBits.length - START_SEQ.length) / 8;
            // Subtract the length of the start sequence to get
            // the number of characters, and divide by 8 since each character is coded on 8 bits
            decodedChars = new char[numberOfCharacters + 1];
            // Create an array to store the decoded characters
            // Iterate through outputBits, starting from the second byte
            // Convert binary to decimal using the index and exponent.
            for (int i = 1; i <= numberOfCharacters; i++) {
                int character = 0;
                for (int j = 0; j < 8; j++) {
                    if (outputBits[8 * i + j] == 1) {
                        character += Math.pow(2, j);
                    }
                }
                // Use the decimal value as ASCII code
                // to retrieve the character and store it
                // in the character array decodedChars.
                decodedChars[i] = (char)character;
            }
        }
    }


    /**
     * Print the elements of an array
     * @param data the array to print
     */
    public static void printIntArray(char[] data) {
        // Display with a space between each character,
        // as seen in the example
        // Decoded Message: H e l l o   W o r l d   !
        System.out.print(" ");
        for (int i = 0; i < data.length; i++) {
            System.out.print(" ");
            System.out.print(data[i]);
        }
        System.out.print(" ");
        System.out.println("");
    }



    /**
     * Display a signal in a window
     * @param sig  the signal to display
     * @param start the first sample to display
     * @param stop the last sample to display
     * @param mode "line" or "point"
     * @param title the title of the window
     */
    public static void displaySig(double[] sig, int start, int stop, String mode, String title){
        StdDraw.enableDoubleBuffering();
        // Set up the drawing canvas
        StdDraw.setPenColor(StdDraw.BLACK);
        StdDraw.setCanvasSize(1280, 720);
        StdDraw.setXscale(start, stop);
        StdDraw.setYscale(-100000, 100000);
        StdDraw.setTitle(title + " double[]");
        // Clear the background
        StdDraw.clear();
        // Set the pen color and thickness
        StdDraw.setPenRadius(0.005);
        // Draw the signal
        for (int i = start; i < stop && i < sig.length - 1; i++) {
            if ("line".equals(mode)) {
                StdDraw.line(i, sig[i], (double)i + 1, sig[i + 1]);
            } else if ("point".equals(mode)) {
                StdDraw.point(i, sig[i]);
            }
        }
        // Show the drawing on screen
        StdDraw.show();
    }

    /**
     * Find the cutoff frequency of the low pass filter
     * @return cutoffFrequency the cutoff frequency
     */

    /**
     *  Un exemple de main qui doit pourvoir être exécuté avec les méthodes
     * que vous aurez conçues.
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java DosRead <input_wav_file>");
            return;
        }
        String wavFilePath = args[0];
        // Open the WAV file and read its header
        DosRead dosRead = new DosRead();
        dosRead.readWavHeader(wavFilePath);
        // Print the audio data properties
        System.out.println("Fichier audio: " + wavFilePath);
        System.out.println("\tSample Rate: " + dosRead.sampleRate + " Hz");
        System.out.println("\tBits per Sample: " + dosRead.bitsPerSample + " bits");
        System.out.println("\tData Size: " + dosRead.dataSize + " bytes");
        // Read the audio data
        dosRead.readAudioDouble();
        // reverse the negative values
        dosRead.audioRectifier();
        // apply a low pass filter

        // ? Choice of low-pass filter
        // Default filter
        //dosRead.LPFilter(200);

        //---FILTRE 1----
        Profiler.init();
        Profiler.analyse(dosRead.lpFilter1::lpFilter, dosRead.audio, dosRead.sampleRate, 200);
        Profiler.getGlobalTime();
        dosRead.audio = dosRead.lpFilter1.lpFilter(dosRead.audio, dosRead.sampleRate, 200);


        // //---FILTRE 2----
        // Profiler.init();
        // Profiler.analyse(dosRead.lpFilter2::lpFilter, dosRead.audio, dosRead.sampleRate, 0.02);
        // Profiler.getGlobalTime();
        // dosRead.audio = dosRead.lpFilter2.lpFilter(dosRead.audio, dosRead.sampleRate, 0.02);


        // Resample audio data and apply a threshold to output only 0 & 1
        dosRead.audioResampleAndThreshold(dosRead.sampleRate/BAUDS, 6000 ); // 12000 too high for sample - 6000 good for both
        dosRead.decodeBitsToChar();
        if (dosRead.decodedChars != null){
            System.out.print("Message décodé : ");
            printIntArray(dosRead.decodedChars);
        }
        displaySig(dosRead.audio, 0, 5000, "line", "Signal audio");
        // Close the file input stream
        try {
            dosRead.fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
