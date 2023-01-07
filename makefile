SOURCES = parser/meta/PeerInit.java \
		parser/CommonConfig.java \
		parser/PeerConfig.java \
		message/Actual.java \
		message/Handshake.java \
		message/MessageType.java \
		peer/ConnectionFrom.java \
		peer/ConnectionTo.java \
		peer/DownloadRate.java \
		peer/PeerProcess.java \
		peerProcess.java

CLASSES = $(SOURCES:.java=.class)

all: $(CLASSES)

%.class: %.java
	javac $<

clean:
	rm -f $(CLASSES)