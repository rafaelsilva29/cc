
package TransfereCC;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import static java.lang.Thread.sleep;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.nio.ByteBuffer;

/**
 *
 * @author Rafael Silva, José Ramos
 */

public class SenderUDP implements Runnable {
     
    private static final int HEADER = 24;       // checksum:8, seqNum:4, flag:4, destinyPort:4, sourcePort:4 Bytes -> 24 Bytes Total
    private static final int PACK_SIZE = 1000;  // data <= 1000 Bytes
    private static final int WINDOW_SIZE = 20;
    private static final int TIMER_VALUE = 300;
    private static final int SERVER_PORT = 7777;
    private static final int ACK_PORT = 8888;
    
    private final StateTable stateTable;
    private final DatagramSocket socketSender;
    private final InetAddress adressIP;
    private final String pathSource;

    public SenderUDP(StateTable st) throws UnknownHostException{
        this.socketSender = st.getSocketSender();
        this.pathSource = st.getPathSource();
        this.adressIP = InetAddress.getByName(st.getHost());
        this.stateTable = st;
    }
    
    private byte[] generatePack(int seqNum, byte[] dataBytes, int flag){
        byte[] seqNumBytes = ByteBuffer.allocate(4).putInt(seqNum).array(); // Seq num (4 bytes)
        byte[] flagBytes = ByteBuffer.allocate(4).putInt(flag).array(); // Seq num (4 bytes)
        byte[] destinyBytes = ByteBuffer.allocate(4).putInt(SERVER_PORT).array(); // 
        byte[] sourceBytes = ByteBuffer.allocate(4).putInt(ACK_PORT).array(); // 
        // generate checksum 
        CRC32 checksum = new CRC32();
        checksum.update(seqNumBytes);
        checksum.update(flagBytes);
        checksum.update(destinyBytes);
        checksum.update(sourceBytes);
        checksum.update(dataBytes);
        byte[] checksumBytes = ByteBuffer.allocate(8).putLong(checksum.getValue()).array(); // CheckSum (8 bytes)
        // generate packet
        ByteBuffer pktBuf = ByteBuffer.allocate(HEADER + dataBytes.length);
        pktBuf.put(checksumBytes);
        pktBuf.put(seqNumBytes);
        pktBuf.put(flagBytes);
        pktBuf.put(destinyBytes);
        pktBuf.put(sourceBytes);
        pktBuf.put(dataBytes);
        return pktBuf.array();
    }
    
    @Override
    public void run() {
        try {
            FileInputStream fis = new FileInputStream(new File(this.pathSource));
            try {
                while(!this.stateTable.getDownloadComplete()){
                    // Enviar os pacotes se a janela nao estiver cheia
                    if(this.stateTable.getProxNumSeq() < (this.stateTable.getBaseWindow() + this.stateTable.getWindowDegree())){
                        
                        // Time Out
                        if(this.stateTable.getBaseWindow() == this.stateTable.getProxNumSeq()){
                            this.stateTable.setTimer(true);
                        }	

                        byte[] sendData = new byte[HEADER];
                        boolean isFinalSeqNum = false;

                        if(this.stateTable.getProxNumSeq() < this.stateTable.getListPack().size()){
                           sendData = this.stateTable.getListPack().get(this.stateTable.getProxNumSeq());
                        } else {
                            if(this.stateTable.getProxNumSeq() == 0){
                                // Se for o primeiro
                                byte[] dataBuffer = new byte[PACK_SIZE];
                                int dataLength = fis.read(dataBuffer, 0,  PACK_SIZE);
                                byte[] dataBytes = Arrays.copyOfRange(dataBuffer, 0, dataLength);
                                sendData = generatePack(this.stateTable.getProxNumSeq(), dataBytes, 4);    
                            } else {
                                byte[] dataBuffer = new byte[PACK_SIZE];
                                int dataLength = fis.read(dataBuffer, 0, PACK_SIZE);
                                // Se nao tiver mais nada a enviar
                                if (dataLength == -1){
                                    isFinalSeqNum = true;
                                    sendData = generatePack(this.stateTable.getProxNumSeq(), "TransfereCC".getBytes(), 1);
                                }
                                else{
                                    byte[] dataBytes = Arrays.copyOfRange(dataBuffer, 0, dataLength);
                                    sendData = generatePack(this.stateTable.getProxNumSeq(), dataBytes, 0);  
                                }
                            }
                            this.stateTable.addPack(sendData);
                    }
                    this.socketSender.send(new DatagramPacket(sendData, sendData.length, this.adressIP, SERVER_PORT));
                    System.out.println("> SenderUDP -> Sent seqNum " + this.stateTable.getProxNumSeq());

                    if (!isFinalSeqNum)
                        this.stateTable.setProxNumSeq(this.stateTable.getProxNumSeq()+1);
                    }
                    sleep(5);
                }
            } catch (IOException e) {
                System.out.println("> SenderUDP -> Error: " + e.getMessage());
            } catch (InterruptedException ex) {
                System.out.println("> SenderUDP -> Error: " + ex.getMessage());
            }finally {
                this.stateTable.setTimer(false);
                this.stateTable.closeSocketSender();
                fis.close();
                System.out.println("> SenderUDP -> Socket Sender closed...");
                System.out.println("> SenderUDP -> Transfer Complete...");
            }
        } catch (IOException e) {
             System.out.println("> SenderUDP -> Error: " + e.getMessage());
             System.exit(-1);
        }
    }
}  