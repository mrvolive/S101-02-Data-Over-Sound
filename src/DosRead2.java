/**
 * MARAVAL Olivier et KHERZA Yahia
 * S1-A1
 * Groupe 7
 */


import java.io.*;
import java.util.List;


public class DosRead2 {
    static final int FP = 1000;
    static final int BAUDS = 100;
    static final int[] START_SEQ = {1, 0, 1, 0, 1, 0, 1, 0};
    static int threshold = 11000;
    FileInputStream fileInputStream;
    static List<Integer> sampleIndexList = new java.util.ArrayList<>();
    int sampleRate = 44100;
    int bitsPerSample;
    int dataSize;
    double[] audio;
    int[] outputBits;
    char[] decodedChars;
    // higher cutoffFreq = less filtering
    static int cutoffFreq = 20; //20 - 500 - 1000
    static double n = 90; // 600 - 300 - 90
    static int readingOffset = 332; // offset de 332 pour prendre le 3/4 des symboles mais rater un pic de 1

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
        audio = new double[dataSize];
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
        // Calculate the number of samples to skip per period
        int sampleInterval = period; // Convert period from ms to samples
        System.out.println("sampleInterval = " + sampleInterval);

        // Determine the size of the outputBits array
        int numOutputBits = audio.length / sampleInterval; // There is one bit per sample
        outputBits = new int[numOutputBits]; 

        // Sample the audio at regular intervals and apply threshold
        for (int i = 0; i < numOutputBits; i++) {
            // Find the sample index by skipping the necessary number of samples
            // offset to take a specific part of the signal
            int sampleIndex = ((i) * sampleInterval)+readingOffset; 
            
            // Debug affichage
            if (sampleIndex < 1000)
                System.out.println("sampleIndex" + i + " = " + sampleIndex);
            sampleIndexList.add(sampleIndex);

            // Check if the sample index is within the bounds of the audio array
            if (sampleIndex < audio.length) {
                // Apply threshold to determine if the bit is 0 or 1
                outputBits[i] = audio[sampleIndex] > threshold ? 1 : 0;
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
        for (int i = 0; i < data.length; i++) {;
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
        StdDraw.setCanvasSize(1280, 1440);
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

        // Draw the original signal below the first one
        StdDraw.setPenColor(StdDraw.RED);
        StdDraw.setPenRadius(0.005);
        StdDraw.setYscale(-100000, 100000);
        
        // Draw the signal
        for (int i = start; i < stop && i < sig.length - 1; i++) {
            if ("line".equals(mode)) {
                StdDraw.line(i, sig[i], (double)i + 1, sig[i + 1]);
            } else if ("point".equals(mode)) {
                StdDraw.point(i, sig[i]);
            }
        }
    }

    public static void displaySig(List<double[]> listOfSigs, int start, int stop, String mode, String title) {
        // Set up the drawing canvas
        StdDraw.enableDoubleBuffering();
        StdDraw.setCanvasSize(1900, 800);
        StdDraw.setXscale(start, stop);
        StdDraw.setYscale(-50000, 50000);
        StdDraw.setTitle(title + " List<double[]>");

        // Clear the background
        StdDraw.clear();

        // Dessiner l'axe des abscisses
        StdDraw.setPenColor(StdDraw.BLACK);
        StdDraw.setPenRadius(0.005);
        StdDraw.line(start, 0, stop, 0);
        StdDraw.line(-50000, threshold, 50000, threshold);

        // Marquer les valeurs temporelles sur l'axe des abscisses
        int interval = (stop - start) / 10;
        for (int i = start; i <= stop; i += interval) {
            StdDraw.text(i, -5000, String.valueOf(i));
            StdDraw.line(i, -1000, i, 1000);
        }
        StdDraw.setPenColor(StdDraw.GRAY);
        for (int i = 0; i < sampleIndexList.size(); i++) {
            StdDraw.line(sampleIndexList.get(i), -50000, sampleIndexList.get(i), 50000);
        }
        

        // Set the pen color and thickness
        StdDraw.setPenRadius(0.005);
        StdDraw.setPenColor(StdDraw.BOOK_RED);

        // Draw each signal in the list
        for (double[] sig : listOfSigs) { 
            for (int i = start; i < stop && i < sig.length - 1; i++) {
                if ("line".equals(mode)) {
                    StdDraw.line(i, sig[i], (double)i + 1, sig[i + 1]);
                } else if ("point".equals(mode)) {
                    StdDraw.point(i, sig[i]);
                }
            }
            StdDraw.setPenColor(StdDraw.BLUE);
        }

        // Calculate the delta between max and min values of the second signal between sig[start] and sig[stop]
        double min = listOfSigs.get(1)[start];
        double max = listOfSigs.get(1)[start];
        for (int i = start; i < stop && i < listOfSigs.get(1).length - 1; i++) {
            if (listOfSigs.get(1)[i] < min) {
                min = listOfSigs.get(1)[i];
            }
            if (listOfSigs.get(1)[i] > max) {
                max = listOfSigs.get(1)[i];
            }
        }

        // Draw lines along min and max
        StdDraw.setPenColor(StdDraw.GREEN);
        StdDraw.setPenRadius(0.002);
        StdDraw.line(start, min, stop, min);
        StdDraw.line(start, max, stop, max);

        // Write the delta between min and max on the graph
        StdDraw.setPenColor(StdDraw.BLACK);
        StdDraw.textRight(stop, min, String.valueOf(max - min));



        // Show the drawing on screen
        StdDraw.show();
    }

    /**
     *  Un exemple de main qui doit pourvoir être exécuté avec les méthodes
     * que vous aurez conçues.
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java DosRead2 <input_wav_file>");
            return;
        }
        String wavFilePath = args[0];
        // Open the WAV file and read its header
        DosRead2 dosRead2 = new DosRead2();
        dosRead2.readWavHeader(wavFilePath);
        // Print the audio data properties
        System.out.println("Fichier audio: " + wavFilePath);
        System.out.println("\tSample Rate: " + dosRead2.sampleRate + " Hz");
        System.out.println("\tBits per Sample: " + dosRead2.bitsPerSample + " bits");
        System.out.println("\tData Size: " + dosRead2.dataSize + " bytes");
        // Read the audio data
        dosRead2.readAudioDouble();

        // Keep a copy of the original audio data
        double[] audioOriginal = dosRead2.audio.clone();
        // Add the original audio data to the list of signals to display
        List<double[]> listOfSigs = new java.util.ArrayList<>();
        listOfSigs.add(audioOriginal);


        // reverse the negative values
        dosRead2.audioRectifier();
        // apply a low pass filter

        // ? Choice of low-pass filter

        // //---FILTRE 1----
        // Profiler.init();
        // Profiler.analyse(dosRead2.lpFilter1::lpFilter, dosRead2.audio, dosRead2.sampleRate, n);
        // Profiler.getGlobalTime();
        // dosRead2.audio = dosRead2.lpFilter1.lpFilter(dosRead2.audio, dosRead2.sampleRate, n);
        // System.out.println("Filter 1 : SMA");
        // System.out.println("n = " + n);

        //---FILTRE 2----
        Profiler.init();
        Profiler.analyse(dosRead2.lpFilter2::lpFilter, dosRead2.audio, dosRead2.sampleRate,cutoffFreq);
        Profiler.getGlobalTime();
        dosRead2.audio = dosRead2.lpFilter2.lpFilter(dosRead2.audio, dosRead2.sampleRate, cutoffFreq);
        System.out.println("Filter 2 : EMA");
        System.out.println("cutoffFreq =" + cutoffFreq);


  
        System.out.println("threshold =" + threshold);


        // Resample audio data and apply a threshold to output only 0 & 1
        dosRead2.audioResampleAndThreshold((int)(dosRead2.sampleRate)/BAUDS, threshold); // 12000 for LP1 - 12000 for LP2
        dosRead2.decodeBitsToChar();
        if (dosRead2.decodedChars != null){
            System.out.print("Message décodé : ");
            printIntArray(dosRead2.decodedChars);
        }
        listOfSigs.add(dosRead2.audio);
        displaySig(listOfSigs, 0, 5000, "line", "Signal audio");
        

        // Close the file input stream
        try {
            dosRead2.fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
