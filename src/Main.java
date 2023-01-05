import parser.CommonConfig;
import parser.PeerConfig;
import parser.meta.PeerInit;
import peer.PeerProcess;

public class Main {
    public static void main(String[] args) {

        new startup().start();

    }

    public static class startup extends Thread{

        private void stop(long ms){
            try{
                Thread.sleep(ms);
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        public void run(){
            PeerConfig.init();
            CommonConfig.init();

            for(PeerInit peer : PeerConfig.peers){
                PeerProcess p = new PeerProcess(peer.peerID);
                p.start();
                stop(800);
            }
        }

    }
}