JFLAGS = -g
JC = javac
JVM= java 
FILE=

.SUFFIXES: .java .class

.java.class:
		$(JC) $(JFLAGS) $*.java

CLASSES = \
          AgenteUDP/AgenteUDP.java \
          Statistics/InfoTransfer.java \
          Statistics/StartTime.java \
          TransfereCC/TransfereCC.java \
          TransfereCC/StateTable.java \
          TransfereCC/SenderUDP.java \
          TransfereCC/ReceiverUDP.java \
          TransfereCC/Temporizador.java

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) AgenteUDP/*.class TransfereCC/*.class Statistics/*.class