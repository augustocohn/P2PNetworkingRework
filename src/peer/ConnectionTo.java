package peer;

import message.Actual;
import message.Handshake;
import message.MessageType;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ConnectionTo extends Thread {

    private static boolean canCloseConnectionTo = false;

    public static void closeConnectionTo(){
        canCloseConnectionTo = true;
    }
    private int peerID;

    private PeerProcess peer;
    private String hostname;
    private int port;

   private Socket connection;
    private ObjectOutputStream os;

    private void closeStreamsAndSockets(){
        try{
            os.close();
        } catch (Exception e){
            System.out.println("Unable to close streams/sockets in ConnectionTo");
        }
    }

    public ConnectionTo(int _peerID, String _hostname, int _port, PeerProcess _peer){
        peerID = _peerID;
        hostname = _hostname;
        port = _port;
        peer = _peer;
    }

    private void sendHandshake(){
        sendMessage(new Handshake(peerID).getBytes());
    }

    public void sendMessage(byte[] message){
        try{
            os.writeObject(message);
        } catch(Exception e){
            System.out.println("Failed to send message");
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

        while(true) {
            try {
                connection = new Socket(hostname, port);
                System.out.println("Connection established");
                os = new ObjectOutputStream(connection.getOutputStream());
                break;
            } catch (Exception e) {
                System.out.println("Failed to initialized outputstream");
            }
        }

        //Handshake
        sendHandshake();
        //Bitfield if has any part of the file
        if(peer.getStartedWithFile() || peer.getHasFilePieces()) {
            System.out.println(peerID + " sent bitfield message");
            sendMessage(new Actual(MessageType.bitField, peerID).getBytes());
        }

        while(!canCloseConnectionTo){
            //sending controlled via message action/response
            stop(100);
        }

//        closeStreamsAndSockets();
//        ConnectionFrom.closeConnectionFrom();


    }

}
