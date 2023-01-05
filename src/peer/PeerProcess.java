package peer;

import message.Actual;
import message.MessageType;
import parser.CommonConfig;
import parser.PeerConfig;
import parser.meta.PeerInit;

import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

//Client Process
public class PeerProcess extends Thread {

    private static final HashMap<Integer, PeerProcess> peers = new HashMap<>();

    public static void checkForAllPeersHaveFullFile(){
        for(PeerProcess peer : peers.values()){
            if(!peer.hasFullFile){
                return;
            }
        }
        closeClients();
    }

    private void closeStreamsAndSockets(){
        try{
            sSocket.close();
        } catch (Exception e){
            System.out.println("Error closing streams/sockets in PeerProcess");
        }
    }

    private static boolean canCloseClients = false;

    public static void closeClients(){
        canCloseClients = true;
    }

    private int peerID;

    public int getPeerID(){
        return peerID;
    }

    private final PeerInit meta;
    public PeerInit getMeta(){
        return meta;
    }

    private ServerSocket sSocket;

    private final boolean startedWithFile;
    public boolean getStartedWithFile(){
        return startedWithFile;
    }

    private boolean hasFilePieces;
    public boolean getHasFilePieces(){
        return hasFilePieces;
    }
    public void updateHasFilePiecesStatus(){
        hasFilePieces = Boolean.TRUE;
    }

    private boolean hasFullFile;
    public boolean getHasFullFile(){
        return hasFullFile;
    }
    public void updateFullFileStatus(){
        hasFullFile = Boolean.TRUE;
    }

    private final Path fileLocation;

    private byte[] fileContents;
    public byte[] getFileContents(){
        return fileContents;
    }

    private final HashMap<Integer, ConnectionTo> connectedTo = new HashMap<>();
    public HashMap<Integer, ConnectionTo> getConnectedTo(){
        return connectedTo;
    }

    private final ArrayList<Boolean> bitField = new ArrayList<>();
    public ArrayList<Boolean> getBitField(){
        return bitField;
    }

    private final HashMap<Integer, ArrayList<Boolean>> neighborBitFields = new HashMap<>();
    public HashMap<Integer, ArrayList<Boolean>> getNeighborBitFields(){
        return neighborBitFields;
    }

    private HashSet<Integer> chokedBy = new HashSet<>();
    public HashSet<Integer> getChokedBy(){
        return chokedBy;
    }

    private final HashSet<Integer>  unchokedNeighbors = new HashSet<>();
    public HashSet<Integer> getUnchokedNeighbors(){
        return unchokedNeighbors;
    }

    //Those that sent me an interested message - they want pieces from me
    private final HashSet<Integer> interestedNeighbors = new HashSet<>();
    public HashSet<Integer> getInterestedNeighbors(){
        return interestedNeighbors;
    }

    //So we don't bombard the connections with interested/not interested messages
    private final HashSet<Integer> interestedIn = new HashSet<>();
    public HashSet<Integer> getInterestedIn(){
        return interestedIn;
    }

    //<pieceIndex, connectedPeerID>
    private final HashMap<Integer, Integer> requestedPieces = new HashMap<>();
    public HashMap<Integer, Integer> getRequestedPieces(){
        return requestedPieces;
    }

    //Optimistically Unchoked - if there are more peers than [not part of unchokedNeighbors list]
    private Integer optimisticallyUnchoked;

    private final PriorityQueue<DownloadRate> sortedDownloadRates = new PriorityQueue<>();
    public PriorityQueue<DownloadRate> getSortedDownloadRates() {
        return sortedDownloadRates;
    }

    private final HashMap<Integer, DownloadRate> downloadRates = new HashMap<>();
    public HashMap<Integer, DownloadRate> getDownloadRates(){
        return downloadRates;
    }

    //Spawns threads that are on timers
    private RecalculatePreferredNeighbors recalculatePreferredNeighbors;
    private RecalculateOptimisticallyUnchoked recalculateOptimisticallyUnchoked;

    //Methods
    private int calculatePieceCount(){
        int fileSize = CommonConfig.fileSize;
        int pieceSize = CommonConfig.pieceSize;
        return fileSize%pieceSize == 0 ? fileSize/pieceSize : (fileSize/pieceSize) + 1;
    }

    private void populateBitField(Boolean hasFile){
        int pieceCount = calculatePieceCount();
        for(int i = 0; i < pieceCount; i++){
            bitField.add(hasFile);
        }
    }

    public PeerProcess(int _peerID){
        peerID = _peerID;
        peers.put(peerID, this);

        meta = PeerConfig.getPeer(peerID);

        fileLocation = Paths.get(System.getProperty("user.dir") + "\\peer_" + peerID + "\\" + CommonConfig.fileName);

        startedWithFile = PeerConfig.getPeer(_peerID).hasFile;

        if(meta.hasFile){
            populateBitField(Boolean.TRUE);
            hasFullFile = true;
            hasFilePieces = true;
            try {
                fileContents = Files.readAllBytes(fileLocation);
            }catch(Exception e){
                System.out.println("Failed to read file contents");
            }
        } else {
            populateBitField(Boolean.FALSE);
            fileContents = new byte[CommonConfig.fileSize];
        }

        //will never be null since peers are created from PeerInit objects
    }

    public static PeerProcess getPeer(int peerID){
        return peers.get(peerID);
    }

    private void stop(long ms){
        try{
            Thread.sleep(ms);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void run(){
        //A bit of padding
        stop(500);

        new ConnectToOtherPeerServers().start();
        new ListenForPeerClients().start();

        //more padding
        stop(100);

        //might not need to store
        RecalculatePreferredNeighbors pN = new RecalculatePreferredNeighbors();
        pN.start();
        RecalculateOptimisticallyUnchoked oU = new RecalculateOptimisticallyUnchoked();
        oU.start();

        while(!canCloseClients){
            stop(100);
        }

//        pN.closeThread = true;
//        oU.closeThread = true;
//
//        int waitForThreadsToDie = Math.max(CommonConfig.unchokingInterval, CommonConfig.optimisticallyUnchokeInterval);
//        stop(waitForThreadsToDie);
//
//        closeStreamsAndSockets();

        if(!startedWithFile){
            try{
                Files.write(fileLocation, fileContents);
            } catch (Exception e){
                System.out.println("Failed to write to file");
            }
        }

    }

    public class ListenForPeerClients extends Thread {

        public void run(){
            try{
                sSocket = new ServerSocket(meta.portNum);
                int connectionCount = 0;
                int otherPeerCount = PeerConfig.peers.size()-1;
                while(connectionCount < otherPeerCount){
                    ConnectionFrom connection = new ConnectionFrom(peerID, sSocket.accept());
                    connection.start();
                    connectionCount++;
                }
            } catch(Exception e){
                System.out.println("ConnectionFrom broken");
            }

        }

    }

    public class ConnectToOtherPeerServers extends Thread {

        @Override
        public void run(){

            for(PeerInit peer : PeerConfig.peers){
                if(peer.peerID == peerID) {
                    continue;
                }
                else {
                    ConnectionTo connection = new ConnectionTo(peerID, peer.hostname, peer.portNum, PeerProcess.getPeer(peerID));
                    connectedTo.put(peer.peerID, connection);
                    connection.start();
                }
            }

        }


    }

    public class Recalculate extends Thread{

        protected boolean closeThread = false;

        protected void stop(long ms){
            try{
                Thread.sleep(ms);
            } catch (Exception e){
                e.printStackTrace();
            }
        }

    }

    public class RecalculatePreferredNeighbors extends Recalculate {
        //FIXME verify logic
        private void calculateWithoutFullFile(){
            ArrayList<Integer> interestedNeighbors = new ArrayList<>(getInterestedNeighbors());
            if(downloadRates.isEmpty()){
                for(Integer connectedPeer : connectedTo.keySet()){
                    if(unchokedNeighbors.contains(connectedPeer) && !Objects.equals(connectedPeer, optimisticallyUnchoked)) {
                        unchokedNeighbors.remove(connectedPeer);
                        System.out.println(peerID + " sent choke message to " + connectedPeer);
                        connectedTo.get(connectedPeer).sendMessage(new Actual(MessageType.choke).getBytes());
                    }
                }
                return;
            }
            //sorted download rates
            for(DownloadRate peer : downloadRates.values()){
                if(interestedNeighbors.contains(peer.getPeerID())){
                    sortedDownloadRates.add(peer);
                }
            }
            int k = CommonConfig.numPreferredNeighbors;
            int eligibleNeighbors = Math.min(sortedDownloadRates.size(), k);
            for(int i = 0; i < eligibleNeighbors; i++){
                Integer selectedPreferredNeighbor = sortedDownloadRates.poll().getPeerID();
                //don't send repeat messages to neighbors that are already unchoked
                if(!unchokedNeighbors.contains(selectedPreferredNeighbor)) {
                    unchokedNeighbors.add(selectedPreferredNeighbor);
                    System.out.println(peerID + " sent unchoke message to " + selectedPreferredNeighbor);
                    connectedTo.get(selectedPreferredNeighbor).sendMessage(new Actual(MessageType.unchoke).getBytes());
                }
            }
            //loop through all connected neighbors, send choke to those that haven't been unchoked
            for(Integer notPreferredNeighbor : connectedTo.keySet()){
                if(!unchokedNeighbors.contains(notPreferredNeighbor) && !Objects.equals(notPreferredNeighbor, optimisticallyUnchoked)){
                    System.out.println(peerID + " sent choke message to " + notPreferredNeighbor);
                    connectedTo.get(notPreferredNeighbor).sendMessage(new Actual(MessageType.choke).getBytes());
                }
            }
            downloadRates.clear();
        }

        //FIXME verify logic
        private void calculateWithFullFile(){
            if(interestedNeighbors.isEmpty()){
                for(Integer connectedPeer : connectedTo.keySet()){
                    if(unchokedNeighbors.contains(connectedPeer) && !Objects.equals(connectedPeer, optimisticallyUnchoked)) {
                        unchokedNeighbors.remove(connectedPeer);
                        System.out.println(peerID + " sent choke message to " + connectedPeer);
                        connectedTo.get(connectedPeer).sendMessage(new Actual(MessageType.choke).getBytes());
                    }
                }
                return;
            }
            int k = CommonConfig.numPreferredNeighbors;
            int eligibleNeighbors = Math.min(interestedNeighbors.size(), k);
            ArrayList<Integer> interestedNeighborsList = new ArrayList<>(interestedNeighbors);
            Random rng = new Random();
            for(int i = 0; i < eligibleNeighbors; i++){
                int randomNeighborIndex = rng.nextInt(interestedNeighborsList.size());
                Integer interestedNeighbor = interestedNeighborsList.get(randomNeighborIndex);
                if(!unchokedNeighbors.contains(interestedNeighbor)){
                    unchokedNeighbors.add(interestedNeighbor);
                    System.out.println(peerID + " sent unchoke message to " + interestedNeighbor);
                    connectedTo.get(interestedNeighbor).sendMessage(new Actual(MessageType.unchoke).getBytes());
                }
                interestedNeighborsList.remove(interestedNeighbor);
            }
            for(Integer notPreferredNeighbor : connectedTo.keySet()){
                if(!unchokedNeighbors.contains(notPreferredNeighbor) && !Objects.equals(notPreferredNeighbor, optimisticallyUnchoked)){
                    System.out.println(peerID + " sent choke message to " + notPreferredNeighbor);
                    connectedTo.get(notPreferredNeighbor).sendMessage(new Actual(MessageType.choke).getBytes());
                }
            }
//            interestedNeighbors.clear();
            downloadRates.clear();
        }

        @Override
        public void run(){
            int granularity = 1;
            while(true){
                stop((long)(CommonConfig.unchokingInterval*1000));
                if(canCloseClients){
                    break;
                }
                if(hasFullFile){
                    calculateWithFullFile();
                } else {
                    calculateWithoutFullFile();
                }
            }
        }


    }

    public class RecalculateOptimisticallyUnchoked extends Recalculate {
        Random rng = new Random();

        @Override
        public void run(){
            while(true){
                stop((long)(CommonConfig.unchokingInterval * 1000));
                if(canCloseClients){
                    break;
                }
                if(interestedNeighbors.isEmpty()){
                    optimisticallyUnchoked = null;
                    return;
                }
                ArrayList<Integer> possibleOptimisticallyUnchoked = new ArrayList<>();
                for(Integer neighbors : connectedTo.keySet()){
                    if(interestedNeighbors.contains(neighbors) && !unchokedNeighbors.contains(neighbors)){
                        possibleOptimisticallyUnchoked.add(neighbors);
                    }
                }
                if(!possibleOptimisticallyUnchoked.isEmpty()) {
                    optimisticallyUnchoked = possibleOptimisticallyUnchoked.get(rng.nextInt(possibleOptimisticallyUnchoked.size()));
                    System.out.println(peerID + " optimistically unchoked " + optimisticallyUnchoked);
                    System.out.println(peerID + " sent unchoke message to " + optimisticallyUnchoked);
                    connectedTo.get(optimisticallyUnchoked).sendMessage(new Actual(MessageType.unchoke).getBytes());
                } else {
                    optimisticallyUnchoked = null;
                    System.out.println("Could not assign an optimistically unchoked neighbor");
                }
            }

        }



    }

}
