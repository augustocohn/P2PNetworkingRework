# P2PNetworkingRework

This project was a complete rework of my CNT5106C final project. My group couldn't get some functionality ironed out during our initial submission and I was unhappy with the result. I took what I learned from the submission and made some more informed design decisions and in the end, I produced a more stable and working project. <br>

There were many conditions where the torrent would fail to transfer files to peers, such as when more than 2 peers started with the file and if the peers were started in long intervals of one another. Most of these situations were solved. <br>

The optimistically unchoked functionality was not working at all in the first iteration and we could not confirm that peers that acquired pieces mid torrent would begin sharing those pieces with others. Both these situations have been solved. <br>

## How to  run

1) Change PeerInfo.cfg so that the hostname is localhost (if running locally) or any host. Format is [peerID] [hostname] [port number]
2) Change Common.cfg to any parameters you like
3) Run `make` to compile
4) Run `javac RemotePeerInfo.java` and `javac StartRemotePeers.java` to compile the kick off process
5) Run `java StartRemotePeers` to start all peers
ALT) You can start peers individually by using `java peerProcess [peerID]`
