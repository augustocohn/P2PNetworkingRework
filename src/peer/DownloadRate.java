package peer;

import java.util.HashMap;

public class DownloadRate implements Comparable<DownloadRate> {

    private int downloads;
    private int peerID;
    public DownloadRate(int _peerID){
        peerID = _peerID;
        downloads = 1;
    }

    public int getPeerID(){
        return peerID;
    }

    public int getDownloads(){
        return downloads;
    }

    public void increment(){
        downloads++;
    }

    @Override
    public int compareTo(DownloadRate other){
        return Integer.compare(other.getDownloads(), downloads);
    }

}
