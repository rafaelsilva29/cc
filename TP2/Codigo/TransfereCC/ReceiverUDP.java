
package TransfereCC;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;

/**
 *
 * @author Rafael Silva, José Ramos
 */

public class ReceiverUDP implements Runnable {
    
    private static final int HEADER = 24;        // checksum:8, seqNum:4, flag:4, destinyPort:4, sourcePort:4 Bytes -> 24 Bytes Total
    private static final int PACK_SIZE = 1000;   // data <= 1000 Bytes
    private static final int WINDOW_SIZE = 20;
    private static final int TIMER_VALUE = 300;
    private static final int SERVER_PORT = 7777;
    private static final int ACK_PORT = 8888;

    private final DatagramSocket socketReceiver;
    private final StateTable stateTable;

    public ReceiverUDP(StateTable st) {
       this.stateTable = st;
       this.socketReceiver = st.getSocketReceiver();
    }
    
    // Returns -1 se estiver corrompido, caso contrario retorna ACK
    private int getNumAck(byte[] pkt){
        byte[] received_checksumBytes = Arrays.copyOfRange(pkt, 0, 8);
        byte[] ackNumBytes = Arrays.copyOfRange(pkt, 8, 12);
        byte[] flagBytes = Arrays.copyOfRange(pkt, 12, 16);
        byte[] destinyBytes = Arrays.copyOfRange(pkt, 16, 20);
        byte[] sourceBytes = Arrays.copyOfRange(pkt, 20, 24);
        CRC32 checksum = new CRC32();
        checksum.update(ackNumBytes);
        checksum.update(flagBytes);
        checksum.update(destinyBytes); 
        checksum.update(sourceBytes);
        byte[] calculated_checksumBytes = ByteBuffer.allocate(8).putLong(checksum.getValue()).array(); // checksum (8 bytes)
        if (Arrays.equals(received_checksumBytes, calculated_checksumBytes)) return ByteBuffer.wrap(ackNumBytes).getInt();
        else return -1;
    }
    
    private int getFlag(byte[] dataPack){
        byte[] flagBytes = Arrays.copyOfRange(dataPack, 12, 16);
        return ByteBuffer.wrap(flagBytes).getInt();
    }
    
    private int getDestiny(byte[] dataPack){
        byte[] flagBytes = Arrays.copyOfRange(dataPack, 16, 20);
        return ByteBuffer.wrap(flagBytes).getInt();
    }
    
    private int getSource(byte[] dataPack){
        byte[] flagBytes = Arrays.copyOfRange(dataPack, 20, 24);
        return ByteBuffer.wrap(flagBytes).getInt();
    }

    @Override
    public void run() {
        try {
            byte[] receiveData = new byte[HEADER];  // Pacote ACK sem dados
            DatagramPacket receivePack = new DatagramPacket(receiveData, receiveData.length);
            try {
                while(!this.stateTable.getDownloadComplete()){
                    
                    this.socketReceiver.receive(receivePack);
                    int numAck = getNumAck(receiveData);
                    int flag = getFlag(receiveData);
                    int destiny = getDestiny(receiveData);
                    int source = getSource(receiveData);

                    System.out.println("> ReceiverUDP -> ACK received: " + numAck);
                    
                    if(flag!=5){
                        // Se o ACK nao estiver corrompido
                        if(numAck!=-1){
                            // ACK duplicado
                            if ((this.stateTable.getBaseWindow()==numAck + 1) && (flag==2)){
                                    this.stateTable.setTimer(false);
                                    this.stateTable.setProxNumSeq(this.stateTable.getBaseWindow());		
                            }
                            // Final ACK
                            else if(numAck==-2 && flag==1){ 
                                this.stateTable.setDownLoadComplete(true);
                            }
                            else{
                                // Normal ACK
                                this.stateTable.setBaseWindows(numAck + 1);
                                this.stateTable.congestionControl();
                                if (this.stateTable.getBaseWindow() == this.stateTable.getProxNumSeq()) {
                                    this.stateTable.setTimer(false);}


                                else {
                                     this.stateTable.setTimer(true); 
                                }						
                            }
                        }
                    } else if(flag==5 || destiny!=ACK_PORT || source!=SERVER_PORT){
                        // Cancelar download
                        System.out.println("> ReceiverUDP -> Download Cancel...");
                        this.stateTable.setDownLoadComplete(true);
                    }
                }
            } catch (IOException e) {
                System.out.println("> Receiver -> Error: " + e.getMessage());
            } finally {
                this.stateTable.closeSocketReceiver();
                System.out.println("> ReceiverUDP -> Socket Receiver closed...");
            }
        } catch (Exception e) {
            System.out.println("> ReceiverUDP -> Error: " + e.getMessage());
            System.exit(-1);
        }
    }
 }
 

