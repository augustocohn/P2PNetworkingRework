package peer;

import java.io.ObjectInputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import message.Actual;
import message.MessageType;
import parser.CommonConfig;

public class ConnectionFrom extends Thread{

    private static boolean canCloseConnectionFrom = false;

    public static void closeConnectionFrom(){
        canCloseConnectionFrom = true;
    }
    private int peerID;
    private PeerProcess peer;
    private int connectedPeerID;
    private Socket connection;

    private byte[] message;
    private ObjectInputStream is;

    private void closeStreamsAndSockets(){
        try {
            is.close();
            connection.close();
        } catch (Exception e){
            System.out.println("Unable to close streams/sockets in ConnectionFrom");
        }
    }

    public ConnectionFrom(int _peerID, Socket _connection){
        peerID = _peerID;
        peer = PeerProcess.getPeer(peerID);
        connection = _connection;
    }

    private void parseHandshake(){
        if(message[0] != 'P'){
            System.out.println("Expected handshake");
            return;
        }
        ByteBuffer buff = ByteBuffer.wrap(Arrays.copyOfRange(message, 28, 32));
        connectedPeerID = buff.getInt();
    }

    private void determineIfInterested(){
        ArrayList<Boolean> bitField = new ArrayList<>(peer.getBitField());
        ArrayList<Boolean> neighborBitField = new ArrayList<>(peer.getNeighborBitFields().get(connectedPeerID));
        for(int i = 0; i < bitField.size(); i++){
            if(!bitField.get(i) && neighborBitField.get(i) && !peer.getInterestedIn().contains(connectedPeerID)){
                System.out.println(peerID + " sent interested to " + connectedPeerID);
                peer.getConnectedTo().get(connectedPeerID).sendMessage(new Actual(MessageType.interested).getBytes());
                peer.getInterestedIn().add(connectedPeerID);
                return;
            }
        }
        System.out.println(peerID + " sent not interested to " + connectedPeerID);
        peer.getConnectedTo().get(connectedPeerID).sendMessage(new Actual(MessageType.notInterested).getBytes());
        peer.getInterestedIn().remove(connectedPeerID);
    }

    private int calculateRandomPieceToRequest(){
        ArrayList<Boolean> bitField = peer.getBitField();
        ArrayList<Boolean> neighborBitField = peer.getNeighborBitFields().get(connectedPeerID);
        ArrayList<Integer> pieces = new ArrayList<>();
        for(int i = 0; i < bitField.size(); i++){
            if(!bitField.get(i) && neighborBitField.get(i)){
                pieces.add(i);
            }
        }
        Random rng = new Random();
        int request = pieces.get(rng.nextInt(pieces.size()));
        while(peer.getRequestedPieces().containsKey(request)){
            request = pieces.get(rng.nextInt(pieces.size()));
        }
        return request;
    }

    private void incrementDownloadRate(){
        if(peer.getDownloadRates().containsKey(connectedPeerID)){
            peer.getDownloadRates().get(connectedPeerID).increment();
            return;
        }
        peer.getDownloadRates().put(connectedPeerID, new DownloadRate(connectedPeerID));
    }

    private void placePiece(int index, byte[] piece){
        int pieceSize = CommonConfig.pieceSize;
        int fileSize = CommonConfig.fileSize;
        for(int i = 0; i < piece.length; i++){
            if(index*pieceSize + i >= fileSize){
                return;
            }
            peer.getFileContents()[index*pieceSize + i] = piece[i];
        }
    }

    private void updateBitField(int index){
        peer.getBitField().set(index, Boolean.TRUE);
    }

    private void sendHaveToValidNeighbors(int index){
        for(Integer neighbor : peer.getNeighborBitFields().keySet()){
            if(!peer.getNeighborBitFields().get(neighbor).get(index)){
                System.out.println(peerID + " sent have message to " + connectedPeerID);
                peer.getConnectedTo().get(neighbor).sendMessage(new Actual(MessageType.have, peerID,index).getBytes());
            }
        }
    }

    private boolean checkForEntireFile(){
        for(Boolean piece : peer.getBitField()){
            if(!piece){
                return false;
            }
        }
        System.out.println(peerID + " has entire file");
        peer.updateFullFileStatus();
        return true;
    }

    private void sendAnotherRequest(){
        //if still unchoked and need more pieces, send another request for a piece
        if(peer.getChokedBy().contains(connectedPeerID) || peer.getHasFullFile()){
           return;
        }
        int pieceIndex = calculateRandomPieceToRequest();
        System.out.println(peerID + " sent request to " + connectedPeerID + " for piece " + pieceIndex);
        peer.getConnectedTo().get(connectedPeerID).sendMessage(new Actual(MessageType.request, peerID, pieceIndex).getBytes());
    }

    private void processMessage(){
        if(message == MessageType.UNPROCESSED){
            return;
        }
        //int length = ByteBuffer.wrap(Arrays.copyOfRange(message, 0, 4)).getInt();
        byte type = message[4];
        byte[] payload = Arrays.copyOfRange(message, 5, message.length);

        switch(type){
            case MessageType.choke:
                peer.getChokedBy().add(connectedPeerID);
                break;
            case MessageType.unchoke:
                peer.getChokedBy().remove(connectedPeerID);
                //we should only request another piece if a piece isn't already requested
                if(!peer.getHasFullFile()) {
                    if (!peer.getRequestedPieces().containsValue(connectedPeerID)) {
                        int pieceIndex = calculateRandomPieceToRequest();
                        peer.getRequestedPieces().put(pieceIndex, connectedPeerID);
                        System.out.println(peerID + " sent request message to " + connectedPeerID + " for piece " + pieceIndex);
                        peer.getConnectedTo().get(connectedPeerID).sendMessage(new Actual(MessageType.request, peerID, pieceIndex).getBytes());
                    }
                } else {
                    peer.getConnectedTo().get(connectedPeerID).sendMessage(new Actual(MessageType.notInterested).getBytes());
                }
                break;
            case MessageType.interested:
                peer.getInterestedNeighbors().add(connectedPeerID);
                break;
            case MessageType.notInterested:
                peer.getInterestedNeighbors().remove(connectedPeerID);
                break;
            case MessageType.have:
                peer.getNeighborBitFields().get(connectedPeerID).set(Actual.convertBytesToInt(payload), Boolean.TRUE);
                determineIfInterested();
                break;
            case MessageType.bitField:
                peer.getNeighborBitFields().put(connectedPeerID, Actual.convertBytesToBitField(payload));
                determineIfInterested();
                break;
            case MessageType.request:
                int requestIndex = ByteBuffer.wrap(payload).getInt();
                System.out.println(peerID + " sent piece message to " + connectedPeerID + " for piece " + requestIndex);
                peer.getConnectedTo().get(connectedPeerID).sendMessage(new Actual(MessageType.piece, peerID, requestIndex).getBytes());
                break;
            case MessageType.piece:
                if(!peer.getHasFilePieces()){
                    peer.updateHasFilePiecesStatus();
                }
                incrementDownloadRate();
                //removing from requested pieces since piece payload contains index of piece
                int pieceIndex = ByteBuffer.wrap(Arrays.copyOfRange(payload, 0, 4)).getInt();
                placePiece(pieceIndex, Arrays.copyOfRange(payload, 4, payload.length));
                peer.getRequestedPieces().remove(pieceIndex);
                updateBitField(pieceIndex);
                sendHaveToValidNeighbors(pieceIndex);
                if(checkForEntireFile()){
                    System.out.println(peerID + " sent not interested message to " + connectedPeerID);
                    peer.getConnectedTo().get(connectedPeerID).sendMessage(new Actual(MessageType.notInterested).getBytes());
                    PeerProcess.checkForAllPeersHaveFullFile();
                } else {
                    sendAnotherRequest();
                }
                break;
        }

    }

    private void receiveMessage(){
        try{
            message = (byte[])is.readObject();
        } catch (Exception e){
            message = MessageType.UNPROCESSED;
        }
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
        try{
            is = new ObjectInputStream(connection.getInputStream());
        } catch (Exception e){
            System.out.println("Input stream failed to initialized");
        }

        try{
            //receive handshake and set connected peer
            receiveMessage();
            parseHandshake();
        } catch (Exception e){
            e.printStackTrace();
        }

        System.out.println(peerID + " has fulled established connection to " + connectedPeerID);

        while(!canCloseConnectionFrom){
            try{
                receiveMessage();
                processMessage();
            } catch(Exception e){
                e.printStackTrace();
            }
        }

//        closeStreamsAndSockets();
//        PeerProcess.closeClients();

    }
}
