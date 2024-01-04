/** MARAVAL Olivier
 * KHERZA Yahia
 * S1-A1
 */

import java.io.*;
import java.util.Arrays;


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
            // on convertit les 4 octets (à partir de l'indice 24)
            // en int pour avoir le taux d'échantillonnage ( 32 bits = 4 octets)
            bitsPerSample = byteArrayToInt(header, 34, 16);
            // on convertit les 2 octets (à partir de l'indice 34) permettant de récupérer
            // le nombre de bits par échantillon
            dataSize = byteArrayToInt(header, 40, 32);
            // on convertit les 4 octets (à partir de l'indice 40) permettant de récupérer
            // la taille du fichier
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
        if (fmt == 16)
            return ((bytes[offset + 1] & 0xFF) << 8) | (bytes[offset] & 0xFF);
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
        audio = new double[dataSize / 2]; // Car PCM entier(code1) sur 2 octets
        for (int i = 0; i < (dataSize / 2); i++) {
            audio[i] = (double) byteArrayToInt(audioData, 2 * i, 16);
            // on remplit le tableau audio avec les valeurs en int codées sur de 2 octets (16 bits)

        }

    }

    /**
     * Reverse the negative values of the audio array
     */
    public void audioRectifier() {
        for (int i = 0; i < audio.length; i++) {
            if (audio[i] < 0) {
                audio[i] = -audio[i];
                System.out.print("caca");

            }
        }
        // on vérifie si une valeur est négative, si oui
        // on récupère son opposé
    }

    /**
     * Apply a low pass filter to the audio array
     * Fc = (1/2n)*FECH
     * f_c = (1/2n)*44100
     *
     * @param n the number of samples to average
     */

    //private LPFilter1 lpFilter1 = new LPFilter1();
    //private LPFilter2 lpFilter2 = new LPFilter2();
    // Choix du filtre.
    public void audioLPFilter(int n) {
        //double cutoffFreq = 0.5 / n * sampleRate;
        double[] inputSignal = audio.clone();
        double Gmax = Double.NEGATIVE_INFINITY;
        double Gmax_3dB = 0;
        double cutoffFrequency = 0;
        for (int i = 0; i < inputSignal.length; i++) {
            double s_f = audio[i]; // Amplitude de la sinusoïde en sortie du filtre
            double e_f = Math.abs(inputSignal[i]); // Amplitude de la sinusoïde en entrée du filtre
            double currentGain = 20 * Math.log10(s_f / e_f);
            if (currentGain > Gmax) {
                Gmax = currentGain;
            }
            if (Gmax - currentGain <= 3 && Gmax_3dB == 0) {
                Gmax_3dB = currentGain;
                cutoffFrequency = i;
            }
        }
        // Apply low-pass filter
        for (int i = n; i < audio.length; i++) {
            double sum = 0;
            // Calculate the average of the previous n samples and the current sample
            for (int j = i - n; j <= i; j++) {
                sum += audio[j];
            }
            // Calculate the average
            double average = sum / (n + 1);
            // Replace the current sample with the average only if the frequency is below the cutoff frequency
            //double currentFreq = (i > 0) ? Math.abs(audio[i] - audio[i - 1]) * sampleRate : 0;
            if (audio[i] >= cutoffFrequency) {
                audio[i] = average;
            }
        }
    }
    private LPFilter1 lpFilter1 = new LPFilter1();
    //private LPFilter2 lpFilter2 = new LPFilter2();
    // Choix du filtre.
    public double LPFilteraa(int n) {
        double[] inputSignal = audio.clone();
        double Gmax = Double.NEGATIVE_INFINITY;
        double Gmax_3dB = 0;
        double cutoffFrequency = 0;
        for (int i = 0; i < inputSignal.length; i++) {
            double s_f = audio[i]; // Amplitude de la sinusoïde en sortie du filtre
            double e_f = Math.abs(inputSignal[i]); // Amplitude de la sinusoïde en entrée du filtre
            double currentGain = 20 * Math.log10(s_f / e_f);
            if (currentGain > Gmax) {
                Gmax = currentGain;
            }
            if (Gmax - currentGain <= 3 && Gmax_3dB == 0) {
                Gmax_3dB = currentGain;
                cutoffFrequency = i;
            }
        }
        //On récupère la fréquence de coupure
        return cutoffFrequency;
    }

    /**
     * Resample the audio array and apply a threshold
     * @param period the number of audio samples per symbol
     * @param threshold the threshold that separates 0 and 1
     **/
    public void audioResampleAndThreshold(int period, double threshold) {
        double[] inputSignal = audio.clone();
        // Initialise un tableau pour stocker les moyennes de chaque période.
        double[] outputSignal = new double[inputSignal.length / period];
        // Initialise un tableau pour stocker les résultats binaires (0 ou 1) en fonction du seuil.
        outputBits = new int[inputSignal.length / period];
        // Boucle principale parcourant chaque période.
        for (int i = 0; i < inputSignal.length / period; i++) {
            // Initialise la somme des échantillons de la période à zéro.
            double sum = 0;
            // Boucle interne parcourant les échantillons à l'intérieur de chaque sous-période.
            for (int j = i * period; j < (i + 1) * period; j++) {
                // Accumule les valeurs de la sous-période.
                sum += inputSignal[j];
            }
            // Calcule la moyenne des échantillons de la sous-période.
            outputSignal[i] = sum / period;
            // Applique un seuil.
            if (outputSignal[i] > threshold) {
                outputBits[i] = 1;
            } else {
                outputBits[i] = 0;
            }
        }
        //System.out.println("Contenu du tableau outputBitsAAAA : " + Arrays.toString(outputSignal));
    }


    /**
     * Decode the outputBits array to a char array
     * The decoding is done by comparing the START_SEQ with the actual beginning of outputBits.
     * The next first symbol is the first bit of the first char.
     */
    public void decodeBitsToChar() {
        boolean corresp = true;
        //System.out.println("Contenu du tableau outputBits : " + Arrays.toString(outputBits));
        for (int i = 0; i < START_SEQ.length; i++) {
            if (outputBits[i] != START_SEQ[i]) {
                corresp = false;
                System.out.println("START_SEQ ne correspond pas " + i);
                System.out.println("outputBits[" + i + "] = " + outputBits[i]);
                System.out.println("START_SEQ[" + i + "] = " + START_SEQ[i]);
                //break;  // Sortir de la boucle dès qu'une non-correspondance est trouvée
            }
        }
        if (corresp) {
            //System.out.println("Contenu du tableau outputBits : " + Arrays.toString(outputBits));
            int nbDeCaractere = (outputBits.length - START_SEQ.length) / 8;
            //on retire la longueur de la séquence du début pour avoir
            // le nombre de caractères et on divise par 8 car chaque caractère est codé sur 8 bits
            decodedChars = new char[nbDeCaractere+1];
            // Création du tableau contenant les caractères décodés
            // On parcourt outPutsBits, en commencant par le deuxièmes octets
            //On fait alors la conversion de binaire à décimal en utilisant
            // l'indice et l'exposant.
            for (int i = 1; i <= nbDeCaractere; i++) {
                int caractere = 0;
                for (int j = 0; j < 8; j++) {
                    if (outputBits[8 * i + j] == 1) {
                        caractere += Math.pow(2, j);
                    }
                }
                //On utilie la valeur décimal comme code ASCII
                // pour récupérer le caractère et le stocker
                // dans le tableau de caractère decodedChars.
                decodedChars[i] = (char)caractere;
            }
        }
    }

    /**
     * Print the elements of an array
     * @param data the array to print
     */
    public static void printIntArray(char[] data) {
        // On fait un affichage avec un espace entre chaque
        //caractère comme vu dans l'exemple
        //Message décodé : H e l l o   W o r l d   !
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
        // Set up the drawing canvas
        StdDraw.setPenColor(StdDraw.BLACK);
        StdDraw.setCanvasSize(1280, 720);
        StdDraw.setXscale(start, stop);
        StdDraw.setYscale(-10000, 50000);
        StdDraw.setTitle(title + " double[]");
        // Clear the background
        StdDraw.clear();
        // Set the pen color and thickness
        StdDraw.setPenRadius(0.005);
        // Draw the signal
        StdDraw.enableDoubleBuffering();
        for (int i = start; i < stop && i < sig.length - 1; i++) {
            if ("line".equals(mode)) {
                StdDraw.line(i, sig[i], i + 1, sig[i + 1]);
            } else if ("point".equals(mode)) {
                StdDraw.point(i, sig[i]);
            }
        }
        // Show the drawing on screen
        StdDraw.show();
    }


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
        //Choix du filtre passe-bas
        //dosRead.audioLPFilter(44);
        //---FILTRE 1----
        double cutoffFrequency = dosRead.LPFilteraa(44);
        dosRead.lpFilter1.lpFilter(dosRead.audio, dosRead.sampleRate, cutoffFrequency);
        //---FILTRE 1----
        //---FILTRE 2----
        //double cutoffFrequency = dosRead.LPFilter(44);
        //dosRead.lpFilter2.lpFilter(dosRead.audio, dosRead.sampleRate, cutoffFrequency);
        //---FILTRE 2----
        // Resample audio data and apply a threshold to output only 0 & 1
        dosRead.audioResampleAndThreshold(dosRead.sampleRate/BAUDS, 12000 ); //12000
        dosRead.decodeBitsToChar();
        if (dosRead.decodedChars != null){
            System.out.print("Message décodé : ");
            printIntArray(dosRead.decodedChars);
        }
        displaySig(dosRead.audio, 0, dosRead.audio.length-1, "line", "Signal audio");
        // Close the file input stream
        try {
            dosRead.fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}