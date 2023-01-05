package parser;

import java.io.BufferedReader;
import java.io.FileReader;

public class CommonConfig {

    public static int numPreferredNeighbors;
    public static double unchokingInterval;
    public static double optimisticallyUnchokeInterval;
    public static String fileName;
    public static int fileSize;
    public static int pieceSize;

    public static void init() {
        try{
            BufferedReader in = new BufferedReader(new FileReader("Common.cfg"));
            String[] tokens;

            String preferredNeighbors = in.readLine();
            tokens = preferredNeighbors.split(" ");
            numPreferredNeighbors = Integer.parseInt(tokens[1]);

            String unchoking = in.readLine();
            tokens = unchoking.split(" ");
            unchokingInterval = Double.parseDouble(tokens[1]);

            String optimisticallyUnchoking = in.readLine();
            tokens = optimisticallyUnchoking.split(" ");
            optimisticallyUnchokeInterval = Double.parseDouble(tokens[1]);

            String file = in.readLine();
            tokens = file.split(" ");
            fileName = tokens[1];

            String fSize = in.readLine();
            tokens = fSize.split(" ");
            fileSize = Integer.parseInt(tokens[1]);

            String pSize = in.readLine();
            tokens = pSize.split(" ");
            pieceSize = Integer.parseInt(tokens[1]);

        } catch(Exception e){
            System.out.println("Failed to open Common.cfg");
        }
    }

}
