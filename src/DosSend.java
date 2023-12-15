import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Scanner;

import java.util.Arrays; // ! A enlever avant rendu

public class DosSend {
    final int FECH = 44100; // fréquence d'échantillonnage
    final int FP = 1000;    // fréquence de la porteuses
    final int BAUDS = 100;  // débit en symboles par seconde
    final int FMT = 16 ;    // format des données
    final int MAX_AMP = (1<<(FMT-1))-1; // amplitude max en entier
    final int CHANNELS = 1; // nombre de voies audio (1 = mono)
    final int[] START_SEQ = {1,0,1,0,1,0,1,0}; // séquence de synchro au début
    final Scanner input = new Scanner(System.in); // pour lire le fichier texte

    long taille;                // nombre d'octets de données à transmettre
    double duree ;              // durée de l'audio
    double[] dataMod;           // données modulées
    static char[] dataChar;            // données en char
    FileOutputStream outStream; // flux de sortie pour le fichier .wav


    /**
     * Constructor
     * @param path  the path of the wav file to create
     */
    public DosSend(String path){
        File file = new File(path);
        try{
            outStream = new FileOutputStream(file);
        } catch (Exception e) {
            System.out.println("Erreur de création du fichier");
        }
    }

    /**
     * Write a raw 4-byte integer in little endian
     * @param octets    the integer to write
     * @param destStream  the stream to write in
     */
    public void writeLittleEndian(int octets, int taille, FileOutputStream destStream){
        char poidsFaible;
        while(taille > 0){
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
    public void writeWavHeader(){
        taille = (long)(FECH * duree);
        long nbBytes = taille * CHANNELS * FMT / 8;

        try  {
            // ? Bytes positions of the header in the comments above the commands
            // 1-4 | # 0-3 /* Marks the file as a riff file. Characters are each 1 byte long. */
            outStream.write(new byte[]{'R', 'I', 'F', 'F'});
            // 5-8 | # 4-7 /*  Size of the overall file - 8 bytes, in bytes (32-bit integer). */
            // bit depth * sample rate * duration of audio * number of channels
            writeLittleEndian((int)(nbBytes), 4, outStream);
            // 9-12 | # 8-B /* File Type Header. For our purposes, it always equals "WAVE". */
            outStream.write(new byte[]{'W', 'A', 'V', 'E'});
            // 13-16 | # C-F /* Format chunk marker. Includes trailing null */ 
            outStream.write(new byte[]{'f', 'm', 't', ' '});
            // 17-20 | # 10-13 /* Length of format data - 16 */
            outStream.write(new byte[]{FMT, 0, 0, 0});
            // 21-22 | # 14-15 /* Type of format (1 is PCM) - 2 byte integer */
            outStream.write(new byte[]{1, 0});
            // 23-24 | # 16-17 /* Number of Channels - 2 byte integer */
            outStream.write(new byte[]{CHANNELS, 0});
            // 25-28 | # 18-1B /* Sample Rate - 32 byte integer. */
            writeLittleEndian(FECH, 4, outStream);
            // 29-32 | # 1C-1F /* (Sample Rate * BitsPerSample * Channels) / 8. */
            writeLittleEndian(FECH*FMT*CHANNELS/8, 4, outStream);
            // 33-34 | # 20-21 /* (BitsPerSample * Channels) / 8.1 - 8 bit mono2 - 8 bit stereo/16 bit mono4 - 16 bit stereo */
            outStream.write(new byte[]{CHANNELS * FMT / 8, 0});
            // 35-36 | # 22-23 /* Bits per sample */
            outStream.write(new byte[]{FMT, 0});
            // 37-40 | # 24-27 /* "data" chunk header. Marks the beginning of the data section. */
            outStream.write(new byte[]{'d', 'a', 't', 'a'}); 
            // 41-44 | # 28-2B /* Size of the data section. */
            writeLittleEndian(44, 4, outStream);
        } catch(Exception e){
            System.out.printf(e.toString());
        }
    }


    /**
     * Write the data in the wav file
     * after normalizing its amplitude to the maximum value of the format (8 bits signed)
     */
    public void writeNormalizeWavData(){
        try {
            
            
        } catch (Exception e) {
            System.out.println("Erreur d'écriture");
        }
    }

    /**
     * Read the text data to encode and store them into dataChar
     * @return the number of characters read
     */
    public int readTextData(){
        // read text data from standard input
        while (input.hasNextLine()) {
            String line = input.nextLine();
            dataChar = line.toCharArray();
        }
        input.close();
        System.out.println("dataChar: " + Arrays.toString(dataChar)); // ! A enlever avant rendu
        return 0;
    }

    /**
     * convert a char array to a bit array
     * @param chars
     * @return byte array containing only 0 & 1
     */
    public byte[] charToBits(char[] chars){
        int taille = chars.length;
        // For each character the array will be 8 times bigger
        byte[] byteArray = new byte[taille * 8];
        int nbBoucle = 0;
        while(taille > 0){
            // byteArray[nbBoucle] = (byte)(chars[nbBoucle]&1);
            for(int i=0; i<8; i++){
                // Each char is converted into bytes
                // Each byte is masked and moved to only take the next lowest bit
                // The array will be written in little endian
                // ? Change i to 7-i to write in big endian
                byteArray[i+(8*nbBoucle)] = (byte)((chars[nbBoucle]>>i) & 1);
            }

            taille--;
            nbBoucle++;
        }
        System.out.println(Arrays.toString(byteArray)); // ! A enlever avant rendu
        System.out.println(Arrays.toString(chars)); // ! A enlever avant rendu
        return byteArray;
    }

    /**
     * Modulate the data to send and apply the symbol throughput via BAUDS and FECH.
     * @param bits the data to modulate
     */
    public void modulateData(byte[] bits){ // TODO : A finir
        double[] modulatedSignal = new double[data.length() * BAUDS];

        for (int i = 0; i < bits.length; i++) {
            byte bit = bits[i];
            for (int j = 0; j < BAUDS; j++) {
                double time = j;
                // If the bit is 1, we produce a carrier wave. If it's 0, we produce a flat signal.
                modulatedSignal[i * BAUDS + j] = bit == '1' ? Math.sin(2 * Math.PI * FP * time) : 0;
            }
        }
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
      /*
          À compléter
      */
    }

    /**
     * Display signals in a window
     * @param listOfSigs  a list of the signals to display
     * @param start the first sample to display
     * @param stop the last sample to display
     * @param mode "line" or "point"
     * @param title the title of the window
     */
    public static void displaySig(List<double[]> listOfSigs, int start, int stop, String mode, String title){
      /*
          À compléter
      */
    }


    public static void main(String[] args) {
        // créé un objet DosSend
        DosSend dosSend = new DosSend("DosOok_message.wav");
        //// lit le texte à envoyer depuis l'entrée standard
        //// et calcule la durée de l'audio correspondant
        // dosSend.duree = (double)(dosSend.readTextData()+dosSend.START_SEQ.length/8)*8.0/dosSend.BAUDS;

        // // génère le signal modulé après avoir converti les données en bits
        // dosSend.modulateData(dosSend.charToBits(dosSend.dataChar));
        // écrit l'entête du fichier wav
        dosSend.writeWavHeader();
        // // écrit les données audio dans le fichier wav
        // dosSend.writeNormalizeWavData();

        // // affiche les caractéristiques du signal dans la console
        // System.out.println("Message : "+String.valueOf(dosSend.dataChar));
        // System.out.println("\tNombre de symboles : "+dosSend.dataChar.length);
        // System.out.println("\tNombre d'échantillons : "+dosSend.dataMod.length);
        // System.out.println("\tDurée : "+dosSend.duree+" s");
        // System.out.println();

        // // exemple d'affichage du signal modulé dans une fenêtre graphique
        // displaySig(dosSend.dataMod, 1000, 3000, "line", "Signal modulé");

        dosSend.charToBits(new char[]{'a', 'b', 'c', 'd'});
        dosSend.readTextData();
        dosSend.charToBits(dataChar);
    }
}