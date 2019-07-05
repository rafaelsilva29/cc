
package AgenteUDP;

/**
 *
 * @authors Rafael Silva, José Ramos
 */

import Statistics.InfoTransfer;
import Statistics.StartTime;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Scanner;
import java.util.zip.CRC32;
  
public class AgenteUDP { 
    
    private static final int HEADER = 24;                  // checksum:8, seqNum:4, flag:4, destinyPort:4, sourcePort:4 Bytes -> 24 Bytes Total
    private static final int PACK_SIZE = 1000 + HEADER;    // checksum:8, seqNum:4, flag:4, destinyPort:4, sourcePort:4, data<=1000 Bytes -> 1024 Bytes total
    private static final int SERVER_PORT = 7777;
    private static final int ACK_PORT = 8888;
    
    private final String pathDestiny;
    private final InfoTransfer info;
    private StartTime time;
 
    public AgenteUDP(String path) {
        this.pathDestiny = path;
        this.info = new InfoTransfer();
        System.out.println("> AgenteUDP -> Entry Port: " + SERVER_PORT + ", " + "Destiny Port: " + ACK_PORT);
    }

    // Gerar pacote de ACK e Flags
    private byte[] generatePack(int ackNum, int flag){
        byte[] ackNumBytes = ByteBuffer.allocate(4).putInt(ackNum).array();
        byte[] flagBytes = ByteBuffer.allocate(4).putInt(flag).array();
        byte[] destinyBytes = ByteBuffer.allocate(4).putInt(ACK_PORT).array();
        byte[] sourceBytes = ByteBuffer.allocate(4).putInt(SERVER_PORT).array();
        // Calculate checksum
        CRC32 checksum = new CRC32();
        checksum.update(ackNumBytes);
        checksum.update(flagBytes);
        checksum.update(destinyBytes);
        checksum.update(sourceBytes);
        // Construct Ack packet
        ByteBuffer pktBuf = ByteBuffer.allocate(HEADER);
        pktBuf.put(ByteBuffer.allocate(8).putLong(checksum.getValue()).array());
        pktBuf.put(ackNumBytes);
        pktBuf.put(flagBytes);
        pktBuf.put(destinyBytes);
        pktBuf.put(sourceBytes);
        return pktBuf.array();
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

    public static void clearScreen() {  
        System.out.print("\033[H\033[2J");  
        System.out.flush();  
    }  
    
    private void startAgenteUDP() throws Exception {
        DatagramSocket socketReceiver, socketSender;
        int lastNumSeq = -1;
        int proxNumSeq = 0;                      // Proximo numero de sequencia
        boolean downloadComplete = false;        // Flag caso a transferencia nao for completa
        long totalTransfer = 0;
        
        // Criando sockets
        try {
            socketReceiver = new DatagramSocket(SERVER_PORT);
            socketSender = new DatagramSocket();
            System.out.println("> AgenteUDP -> AgenteUDP Connected, waiting...");
            
            try {
                FileOutputStream fos = null;
                boolean start = true;
              
                while (!downloadComplete){
                    
                    byte[] receiveData = new byte[PACK_SIZE];
                    DatagramPacket receivePack = new DatagramPacket(receiveData, receiveData.length);
                    
                    socketReceiver.receive(receivePack);
                    InetAddress adressIP = receivePack.getAddress(); 
                    
                    if(start){
                        // Criar time
                        start = false;
                        this.time = new StartTime();  
                    }
                    
                    int flag = getFlag(receiveData);
                    int destiny = getDestiny(receiveData);
                    int source = getSource(receiveData);
                    
                    byte[] received_checksum = Arrays.copyOfRange(receiveData, 0, 8);
                    CRC32 checksum = new CRC32();
                    checksum.update(Arrays.copyOfRange(receiveData, 8, receivePack.getLength()));
                    byte[] calculated_checksum = ByteBuffer.allocate(8).putLong(checksum.getValue()).array();

                    // Se o pacote nao esta corrompido
                    if(Arrays.equals(received_checksum, calculated_checksum) && destiny==SERVER_PORT && source==ACK_PORT){
                        int seqNum = ByteBuffer.wrap(Arrays.copyOfRange(receiveData, 8, 12)).getInt();
                        System.out.println("> AgenteUDP -> Received ACK: " + seqNum);

                        long fileSize =  receivePack.getLength();
                        totalTransfer += + fileSize - HEADER;

                        // Se o pacote foi recebido por ordem
                        if(seqNum == proxNumSeq){
                            // Se for o ultimo pacote mandar ACK -2
                            if(flag==1){
                                // Verificar Nome
                                byte[] received_final = Arrays.copyOfRange(receiveData, HEADER, receivePack.getLength());
                                String msg = new String(received_final, "UTF8");
                                if(msg.equals("TransfereCC")){
                                   this.info.setSignature(true);
                                }
                                byte[] ackPkt = generatePack(-2,1);	
                                // Mandar 20 ACK caso o ultimo nao tenha sido enviado
                                for (int i=0; i<20; i++) {
                                    socketSender.send(new DatagramPacket(ackPkt, ackPkt.length, adressIP, ACK_PORT));
                                }
                                downloadComplete = true;			
                                System.out.println("> AgenteUDP -> All packets received... File Created...");
                                continue;  
                            } else {
                                // Send ack
                                byte[] ackPkt = generatePack(seqNum,0);
                                socketSender.send(new DatagramPacket(ackPkt, ackPkt.length, adressIP, ACK_PORT));
                                System.out.println("> AgenteUDP -> Sent ACK: " + seqNum);
                            }
                            // Se for o primeiro pacote
                            if(seqNum==0 && lastNumSeq==-1 && flag==4){
                                // Se for o primeiro pacote da transferencia 
                                // Cria arquivo    
                                File file = new File(this.pathDestiny);
                                if (!file.exists()) {
                                    file.createNewFile();
                                }
                                fos = new FileOutputStream(file);
                                
                                // Escreve dados no arquivo
                                fos.write(receiveData, HEADER, receivePack.getLength() - HEADER);
                            } else {
                                if(flag==0){
                                    // Se nao for o primeiro pacote
                                    fos.write(receiveData, HEADER, receivePack.getLength() - HEADER);
                                }
                            }
                            proxNumSeq++; 
                            lastNumSeq = seqNum; // Atualiza o ultimo numero de sequencia enviado	
                            
                            } else { 
                                // Se o pacote tiver fora de ordem mandar duplicado
                                byte[] ackPkt = generatePack(lastNumSeq,2);
                                socketSender.send(new DatagramPacket(ackPkt, ackPkt.length, adressIP, ACK_PORT));
                                System.out.println("> AgenteUDP -> Sent duplicate ACK: " + proxNumSeq);
                            }
                    } else if(destiny!=SERVER_PORT || source!=ACK_PORT){
                          System.out.println("> AgenteUDP -> Destiny/Source unknown source... ");
                          // Mandar pacote a cancelar transferencia
                          byte[] ackPkt = generatePack(-3,5);
                          socketSender.send(new DatagramPacket(ackPkt, ackPkt.length, adressIP, ACK_PORT));
                    } else {
                        // O pacote esta corrompido
                        System.out.println("> AgenteUDP -> Corrupt packet dropped...");
                        byte[] ackPkt = generatePack(lastNumSeq,3);
                        socketSender.send(new DatagramPacket(ackPkt, ackPkt.length, adressIP, ACK_PORT));
                        System.out.println("> AgenteUDP -> Sent duplicate ACK: " + lastNumSeq);
                    }
                }
                if(fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                System.out.println("> AgenteUDP -> Error: " + e.getMessage());
                System.exit(-1);
            } finally {
                socketReceiver.close();
                socketSender.close();
                System.out.println("> AgenteUDP -> Socket Receiver closed...");
                System.out.println("> AgenteUDP -> Socket Sender closed...");
                this.info.setTotalTransfer(totalTransfer);
                this.info.printInfoTransfer(this.time, 0);
            }
        } catch (SocketException e1) {
            System.out.println("> AgenteUDP -> Error: " + e1.getMessage());
        }
    }
    
    public static void main(String[] args) throws Exception{
        clearScreen();
        System.out.println("<----------------------> AgenteUDP  <---------------------->");
        Scanner in = new Scanner(System.in);
        System.out.print("Enter path destiny: ");
        String path = in.nextLine();
        AgenteUDP agenteUDP = new AgenteUDP(path);
        agenteUDP.startAgenteUDP();
    }
}