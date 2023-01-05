package parser.meta;

public class PeerInit {

    public int peerID;
    public String hostname;
    public int portNum;
    public boolean hasFile;

    public PeerInit(int _peerID, String _hostname, int _portNum, boolean _hasFile){
        peerID = _peerID;
        hostname = _hostname;
        portNum = _portNum;
        hasFile = _hasFile;
    }

}
