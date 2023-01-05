package message;

import java.nio.ByteBuffer;

public class Handshake {

    private int peerID;
    private byte[] header = { 'P', '2', 'P', 'F', 'I', 'L', 'E', 'S', 'H', 'A', 'R', 'I', 'N', 'G', 'P', 'R', 'O', 'J'};
    private byte[] zeros = new byte[10];

    public Handshake(int _peerID){
        peerID = _peerID;
    }

    public byte[] getBytes(){
        ByteBuffer buff = ByteBuffer.allocate(32);
        buff.put(header);
        buff.put(zeros);
        buff.put(ByteBuffer.allocate(4).putInt(peerID).array());
        return buff.array();
    }


}
