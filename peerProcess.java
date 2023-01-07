import parser.CommonConfig;
import parser.PeerConfig;
import parser.meta.PeerInit;
import peer.PeerProcess;

public class peerProcess {

    public static void main(String[] args){

        int peerIDToStart = -1;
        try {
            peerIDToStart = Integer.parseInt(args[0]);
        } catch (Exception e){
            System.out.println("invalid peerID");
            return;
        }

        CommonConfig.init();
        PeerConfig.init();

        for(PeerInit peer : PeerConfig.peers){
            if(peer.peerID == peerIDToStart){
                PeerProcess p = new PeerProcess(peerIDToStart);
                p.start();
            }
        }

        if(peerIDToStart == -1){
            System.out.println("peerID not present in cfg file");
        }

    }

}
