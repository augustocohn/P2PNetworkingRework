package message;

import parser.CommonConfig;
import peer.PeerProcess;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class Actual {

    private byte[] length;
    private final byte type;
    private byte[] payload;

    private byte[] convertIntToBytes(int val){
        return ByteBuffer.allocate(4).putInt(val).array();
    }

    public static int convertBytesToInt(byte[] bytes){ return ByteBuffer.wrap(bytes).getInt(); }

    //for messages with no payload
    public Actual(byte _type){
        type = _type;
        createMessage();
    }

    //for messages with a payload
    public Actual(byte _type, int peerID){
        type = _type;
        createMessage(peerID);
    }

    //for messages with a payload dependent on index
    public Actual(byte _type, int peerID, int index){
        type = _type;
        createMessage(peerID, index);
    }

    private void createMessage(){
        //choke unchoke interested uninterested
        length = new byte[4];
        payload = new byte[0];
    }

    private void createMessage(int peerID){
        //bitfield
        payload = convertToBitFieldToBytes(peerID);
        length = convertIntToBytes(payload.length);
    }

    private void createMessage(int peerID, int index){
        //have request piece
        switch(type){
            case MessageType.have:
            case MessageType.request:
                payload = convertIntToBytes(index);
                length = convertIntToBytes(4);
                break;
            case MessageType.piece:
                payload = calculatePiecePayload(peerID, index);
                length = convertIntToBytes(payload.length);
                break;
        }
    }

    private int calculatePieceOffset(int index){
        int pieceSize = CommonConfig.pieceSize;
        return pieceSize*index;
    }

    private int calculatePieceLength(int index){
        int fileSize = CommonConfig.fileSize;
        int pieceSize = CommonConfig.pieceSize;
        return pieceSize*index < fileSize ? pieceSize : fileSize-pieceSize*index;
    }

    private byte[] calculatePiecePayload(int peerID, int index){
        int pieceLength = calculatePieceLength(index);
        //piece payload contains 4 byte index number and file contents
        ByteBuffer buff = ByteBuffer.allocate(4 + pieceLength);
        buff.put(convertIntToBytes(index));
        byte[] fileContents = PeerProcess.getPeer(peerID).getFileContents();
        int from = calculatePieceOffset(index);
        int to = from + pieceLength;
        buff.put(Arrays.copyOfRange(fileContents, from, to));
        return buff.array();
    }

    public byte[] getBytes(){
        //4 for length field, 1 for type field, then length of payload
        ByteBuffer buff = ByteBuffer.allocate(4 + 1 + payload.length);
        buff.put(length);
        buff.put(type);
        buff.put(payload);
        return buff.array();
    }

    //Bitfield helpers
    private static final byte[] builder = {-128, 64, 32, 16, 8, 4, 2, 1};
    private static int calcSizeOfBitField(int length){
        return length%8 == 0 ? length/8 : length/8+1;
    }

    public static byte[] convertToBitFieldToBytes(int peerID){
        ArrayList<Boolean> bitField = PeerProcess.getPeer(peerID).getBitField();
        byte[] payload = new byte[calcSizeOfBitField(bitField.size())];
        int bitPos = 0;
        int bytePos = 0;
        for(Boolean bit : bitField){
            if(bitPos > 7) {
                bitPos = 0;
                bytePos++;
            }
            payload[bytePos] = bit ? (byte)(builder[bitPos] | payload[bytePos]) : payload[bytePos];
            bitPos++;
        }
        return payload;
    }

    private static int calculatePieceCount(){
        int fileSize = CommonConfig.fileSize;
        int pieceSize = CommonConfig.pieceSize;
        return fileSize%pieceSize == 0 ? fileSize/pieceSize : (fileSize/pieceSize) + 1;
    }

    public static ArrayList<Boolean> convertBytesToBitField(byte[] payload){
        ArrayList<Boolean> bitField = new ArrayList<>();
        for (byte piece : payload) {
            for (int bitPos = 0; bitPos < 8; bitPos++) {
                if ((byte)(piece | builder[bitPos]) == piece) {
                    bitField.add(Boolean.TRUE);
                } else {
                    bitField.add(Boolean.FALSE);
                }
            }

        }
        return bitField;
    }

}
