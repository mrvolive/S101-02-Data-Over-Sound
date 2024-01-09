import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Scanner;

public class DosSend {
    final int FECH = 44100; // fréquence d'échantillonnage
    final int FP = 1000; // fréquence de la porteuses
    final int BAUDS = 100; // débit en symboles par seconde
    final int FMT = 16; // format des données
    final int MAX_AMP = (1 << (FMT - 1)) - 1; // amplitude max en entier
    final int CHANNELS = 1; // nombre de voies audio (1 = mono)
    final int[] START_SEQ = { 1, 0, 1, 0, 1, 0, 1, 0 }; // séquence de synchro au début
    final Scanner input = new Scanner(System.in); // pour lire le fichier texte

    long taille; // nombre d'octets de données à transmettre
    double duree; // durée de l'audio
    double[] dataMod; // données modulées
    char[] dataChar; // données en char
    FileOutputStream outStream; // flux de sortie pour le fichier .wav

    /**
     * Constructor
     * 
     * @param path the path of the wav file to create
     */
    public DosSend(String path) {
        File file = new File(path);
        try {
            outStream = new FileOutputStream(file);
        } catch (Exception e) {
            System.out.println("Erreur de création du fichier");
        }
    }

    /**
     * Write a raw 4-byte integer in little endian
     * 
     * @param octets     the integer to write
     * @param destStream the stream to write in
     */
    public void writeLittleEndian(int octets, int taille, FileOutputStream destStream) {
        char poidsFaible;
        while (taille > 0) {
            poidsFaible = (char) (octets & 0xFF);
            try {
                destStream.write(poidsFaible);
            } catch (Exception e) {
                System.out.println("Erreur d'écriture");
            }
            octets = octets >> 8;
            taille--;
        }
    }

    /**
     * Create and write the header of a wav file
     *
     */
    public void writeWavHeader() {
        taille = (long) (FECH * duree);
        long nbBytes = taille * CHANNELS * FMT / 8;

        try {
            // ? Bytes positions of the header in the comments above the commands
            // 1-4 | # 0-3
            // ? Marks the file as a riff file. Characters are each 1 byte
            outStream.write(new byte[] { 'R', 'I', 'F', 'F' });

            // 5-8 | # 4-7 
            // ? Size of the overall file minus 8 bytes, in bytes (32-bit integer)
            long tailleFichier = 36 + nbBytes; // Header size 44 bytes
            writeLittleEndian((int) (tailleFichier), 4, outStream);

            // 9-12 | # 8-B 
            // ? File Type Header. For our purposes, it always equals "WAVE".
            outStream.write(new byte[] { 'W', 'A', 'V', 'E' });

            // 13-16 | # C-F 
            // ? Format chunk marker. Includes trailing null
            outStream.write(new byte[] { 'f', 'm', 't', ' ' });

            // 17-20 | # 10-13 
            // ? Length of format data 16
            writeLittleEndian(FMT, 4, outStream);

            // 21-22 | # 14-15 
            // ? Type of format (1 is PCM) - 2 byte integer
            outStream.write(new byte[] { 1, 0 });

            // 23-24 | # 16-17 
            // ? Number of Channels - 2 byte integer
            writeLittleEndian(CHANNELS, 2, outStream);

            // 25-28 | # 18-1B 
            // ? Sample Rate - 32 byte integer.
            writeLittleEndian(FECH, 4, outStream);

            // 29-32 | # 1C-1F 
            // ? (Sample Rate * BitsPerSample * Channels) / 8
            int byteRate = FECH * CHANNELS * FMT / 8;
            writeLittleEndian(byteRate, 4, outStream);

            // 33-34 | # 20-21 
            // ? (BitsPerSample * Channels) / 8.1 - 8 bit mono2 - 8 bit stereo/16 bit mono4 - 16 bit stereo
            int blockAlign = CHANNELS * FMT / 8;
            writeLittleEndian(blockAlign, 2, outStream);

            // 35-36 | # 22-23 
            // ? Bits per sample
            writeLittleEndian(FMT, 2, outStream);

            // 37-40 | # 24-27 
            // ? "data" chunk header. Marks the beginning of the data section.
            outStream.write(new byte[] { 'd', 'a', 't', 'a' });

            // 41-44 | # 28-2B 
            // ? Size of the data section.
            writeLittleEndian((int)nbBytes, 4, outStream);
        } catch (Exception e) {
            System.out.printf(e.toString());
        }
    }

    /**
     * Write the data in the wav file
     * after normalizing its amplitude to the maximum value of the format (8 bits
     * signed)
     */
    public void writeNormalizeWavData() { // ? juste ?
        try {
            for (double sample : dataMod) {
                // Normaliser l'échantillon à l'amplitude maximale
                int value = (int) (sample * MAX_AMP);
                // Écrire l'échantillon dans le flux en little endian
                writeLittleEndian(value, FMT / 8, outStream);
            }

        } catch (Exception e) {
            System.out.println("Erreur d'écriture");
        }
    }

    /**
     * Read the text data to encode and store them into dataChar
     * 
     * @return the number of characters read
     */
    public int readTextData() {
        // read text data from standard input
        while (input.hasNextLine()) {
            String line = input.nextLine();
            dataChar = line.toCharArray();
        }
        return dataChar.length;
    }

    /**
     * convert a char array to a bit array
     * 
     * @param chars
     * @return byte array containing only 0 & 1
     */
    public byte[] charToBits(char[] chars) {
        int tailleChar = chars.length;
        // For each character the array will be 8 times bigger
        byte[] byteArray = new byte[(tailleChar * 8)+START_SEQ.length];

        // Add the start sequence
        for (int i = 0; i < START_SEQ.length; i++) {
            byteArray[i] = (byte) START_SEQ[i];
        }

        int nbBoucle = 0;
        while (tailleChar > 0) {
            for (int i = START_SEQ.length; i < 8 + START_SEQ.length; i++) {
                // Each char is converted into bytes
                // Each byte is masked and moved to only take the next lowest bit
                // The array will be written in little endian
                // ? Change i to 7-i to write in big endian
                byteArray[i + (8 * nbBoucle)] = (byte) ((chars[nbBoucle] >> i - START_SEQ.length) & 1);
            }

            tailleChar--;
            nbBoucle++;
        }

        // Affichage des caractères lus
        System.out.print("[ ");
        for (int i = 0; i < chars.length; i++) {
            System.out.print(chars[i] + " ");
        }
        System.out.println("]");

        // Affichage des bits lus
        System.out.print("[ ");
        for (int i = 0; i < byteArray.length; i++) {
            System.out.print(byteArray[i] + " ");
        }
        System.out.println("]");
        return byteArray;
    }

    /**
     * Modulate the data to send and apply the symbol throughput via BAUDS and FECH.
     * 
     * @param bits the data to modulate
     */
    public void modulateData(byte[] bits) {
        // Il y aura 100 symboles par secondes soit 100 bits par secondes (BAUDS)
        // la fréquence de la porteuse est de 1kHz (FP)
        // La fréquence d'échantillonnage est de 44100Hz (FECH)

        int samplesPerBit = FECH / BAUDS;
        dataMod = new double[bits.length * samplesPerBit];
        double powerFactorOut = 4.0; // Facteur pour rendre la transition plus brusque
        double powerFactorIn = 0.1; // Facteur pour rendre la transition plus brusque
        
        for (int i = 0; i < bits.length; i++) {
            for (int j = 0; j < samplesPerBit; j++) {
                double amplitude;
                if (bits[i] == 1) {
                    // Fade-in brusque sur les bits à 1
                    amplitude = Math.pow(((double)j / samplesPerBit), powerFactorIn);
                } else if (bits[i] == 0 && bits[i-1] == 1) {
                    // Fade-out doux sur les bits à 0 après un bit à 1
                    amplitude = Math.pow(((double)(samplesPerBit - j) / samplesPerBit), powerFactorOut);
                }
                else {
                    // Pas de signal sur les bits à 0 n'étant pas précédés d'un bit à 1
                    amplitude = 0.0;
                }
                // Générer l'échantillon en appliquant l'amplitude calculée précédemment
                // La formule est celle d'une sinusoïde de fréquence FP
                dataMod[(i * samplesPerBit) + j] = amplitude * Math.sin(2 * Math.PI * FP * j / FECH);
            }
        }

    }

    /**
     * Display a signal in a window
     * 
     * @param sig   the signal to display
     * @param start the first sample to display
     * @param stop  the last sample to display
     * @param mode  "line" or "point"
     * @param title the title of the window
     */
    public static void displaySig(double[] sig, int start, int stop, String mode, String title) {
        // Set up the drawing canvas
        StdDraw.enableDoubleBuffering();
        StdDraw.setCanvasSize(1800, 720);
        StdDraw.setXscale(start, stop);
        StdDraw.setYscale(-1, 1);
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
     * Display signals in a window
     * 
     * @param listOfSigs a list of the signals to display
     * @param start      the first sample to display
     * @param stop       the last sample to display
     * @param mode       "line" or "point"
     * @param title      the title of the window
     */
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

        // Marquer les valeurs temporelles sur l'axe des abscisses
        int interval = (stop - start) / 10;
        for (int i = start; i <= stop; i += interval) {
            StdDraw.text(i, -5000, String.valueOf(i));
            StdDraw.line(i, -1000, i, 1000);
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

        StdDraw.show();

        //! We never generate multiple signals
    }

    public static void main(String[] args) {
        // créé un objet DosSend
        DosSend dosSend = new DosSend("DosOok_message.wav");
        // lit le texte à envoyer depuis l'entrée standard
        // et calcule la durée de l'audio correspondant
        dosSend.duree = (double) (dosSend.readTextData() + dosSend.START_SEQ.length / 8) * 8.0 / dosSend.BAUDS;

        // génère le signal modulé après avoir converti les données en bits
        dosSend.modulateData(dosSend.charToBits(dosSend.dataChar));
        // écrit l'entête du fichier wav
        dosSend.writeWavHeader();
        // écrit les données audio dans le fichier wav
        dosSend.writeNormalizeWavData();

        // affiche les caractéristiques du signal dans la console
        System.out.println("Message : " + String.valueOf(dosSend.dataChar));
        System.out.println("\tNombre de symboles : " + dosSend.dataChar.length);
        System.out.println("\tNombre d'échantillons : " + dosSend.dataMod.length);
        System.out.println("\tDurée : " + dosSend.duree + " s");
        System.out.println();

        // exemple d'affichage du signal modulé dans une fenêtre graphique
        displaySig(dosSend.dataMod, 0, 3000, "line", "Signal modulé");
    }
}
