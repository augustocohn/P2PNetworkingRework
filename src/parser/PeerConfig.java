package parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

import parser.meta.PeerInit;

public class PeerConfig {

    public static ArrayList<PeerInit> peers = new ArrayList<>();

    public static void init() {
        try{
            BufferedReader in = new BufferedReader(new FileReader("PeerInfo.cfg"));
            String line = in.readLine();
            while(line != null) {
                PeerInit peer = parseLine(line);
                peers.add(peer);
                line = in.readLine();
            }
        } catch(Exception e){
            System.out.println("Failed to open PeerInfo.cfg");
        }
    }

    private static PeerInit parseLine(String line){
        int peerID_;
        String hostname_;
        int listeningPort_;
        boolean file_;

        String[] tokens = line.split(" ");
        peerID_ = Integer.parseInt(tokens[0]);
        hostname_ = tokens[1];
        listeningPort_ = Integer.parseInt(tokens[2]);
        file_ = Integer.parseInt(tokens[3]) == 1;

        return new PeerInit(peerID_, hostname_, listeningPort_, file_);
    }

    public static PeerInit getPeer(int peerID) {
        for(PeerInit peer : peers) {
            if (peer.peerID == peerID)
                return peer;
        }
        return null;
    }


}
