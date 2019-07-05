
package TransfereCC;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

/**
 *
 * @author Rafael Silva, José Ramos
 */

public class StateTable {
    
    private static final int HEADER = 24;         // checksum:8, seqNum:4, flag:4, destinyPort:4, sourcePort:4 Bytes -> 24 Bytes Total
    private static final int PACK_SIZE = 1000;    // data <= 1000 Bytes 
    private static final int WINDOW_SIZE = 20;    // Window starting Size
    private static final int TIMER_VALUE = 300;   // 300ms until timeout
    private static final int SERVER_PORT = 7777;
    private static final int ACK_PORT = 8888;
    private static final int STARTING_WINDOW = 10;
    private static final int MAX_WINDOW_SIZE = 20;  // Max size da Window
    private static final int ADDITIVE_FACTOR = 1;   // fator aditivo de AIMD
    private static final int DIVISIVE_FACTOR = 2;   // fator divisor de AIMD
    
    private int baseWindow;     // Numero em que esta a base da janela
    private int proxNumSeq;     // Proximo numero de sequencia na janela   
    private List<byte[]> listPacks; // Lista de pacotes enviados
    private Timer timer;
    private boolean downloadComplete;   
    private String pathSource;    
    private DatagramSocket socketSender, socketReceiver;
    private String host;
    private String command;
    
    private int windowdegree ; // grau ou tamanho da janela
    private boolean packetLoss; // indicador se ocorreu packetLoss
    private int packetLossCount; // numero de pacores perdidos
    
    public StateTable(){
        this.baseWindow = 0;
        this.proxNumSeq = 0;
        this.listPacks = new ArrayList<>(WINDOW_SIZE);
        this.downloadComplete = false;
        this.windowdegree =  STARTING_WINDOW ;
        this.packetLoss = false;
        this.packetLossCount = 0;
    }
    
    public StateTable(String source, String host, String cmd, int entryPort, int destinyPort) throws SocketException{
        this.baseWindow = 0;
        this.proxNumSeq = 0;
        this.listPacks = new ArrayList<>(WINDOW_SIZE);
        this.downloadComplete = false;
        this.host = host;
        this.command = cmd;
        this.pathSource = source;
        this.socketSender = new DatagramSocket();
        this.socketReceiver = new DatagramSocket(entryPort);
        this.windowdegree =  STARTING_WINDOW ;
        this.packetLoss = false;
        this.packetLossCount = 0;
    }
  
    ///////////////////////////// Getters /////////////////////////////////////
    public synchronized int getBaseWindow(){ return this.baseWindow; }
    
    public synchronized boolean getDownloadComplete(){ return this.downloadComplete; }
    
    public synchronized int getProxNumSeq(){ return this.proxNumSeq; }
    
    public synchronized List<byte[]> getListPack(){ 
        List<byte[]> res = new ArrayList<>(WINDOW_SIZE); 
        res.addAll(this.listPacks);
        return res;
    }
    
    public synchronized String getPathSource(){ return this.pathSource; }
    
    public synchronized DatagramSocket getSocketReceiver(){ return this.socketReceiver; }
    
    public synchronized DatagramSocket getSocketSender(){ return this.socketSender; }
    
    public synchronized String getHost(){ return this.host; }
    
    public synchronized int getWindowDegree() { return this.windowdegree; }
    
    public synchronized boolean getPacketLoss() { return this.packetLoss; }
    
    public synchronized int getPacketLossCoumt() { return this.packetLossCount; }
    
    public static int getMAX_WINDOW_SIZE() { return MAX_WINDOW_SIZE; } 
    ///////////////////////////////////////////////////////////////////////////
    
    ////////////////////////////// Setters ////////////////////////////////////
    public synchronized void setProxNumSeq(int num){ this.proxNumSeq=num; }
    
    public synchronized void setDownLoadComplete(boolean b){ this.downloadComplete=b; }
    
    public synchronized void setBaseWindows(int b){ this.baseWindow=b; }
    
    public synchronized void setWindowDegree(int b) { this.windowdegree = b; }
    
    public synchronized void setPacketLoss(boolean b){ this.packetLoss=b; }
     
    public synchronized void setPacketLossCount(int b) { this.packetLossCount = b; }
    ///////////////////////////////////////////////////////////////////////////
    
    ///////////////////////// Funcoes Auxilires //////////////////////////////
    public synchronized void addPack(byte[] pack){
        this.listPacks.add(pack);
    }
 
    // Para iniciar ou parar o temporizador
    public synchronized void setTimer(boolean newTimer) {
        if(this.timer != null) {
            this.timer.cancel();
        }
        if(newTimer) {
            this.timer = new Timer();
            this.timer.schedule(new Temporizador(), TIMER_VALUE);
        }
    }
    
    public synchronized void closeSocketReceiver(){
        this.socketReceiver.close();
    }
    
    public synchronized void closeSocketSender(){
        this.socketSender.close();
    }
    
    public synchronized void addPacketLossCount(){
        setPacketLossCount(getPacketLossCoumt() + 1);
    }

    public synchronized void congestionControl(){
        if (this.packetLoss){
            setWindowDegree(getWindowDegree()/ DIVISIVE_FACTOR);
            addPacketLossCount();
            setPacketLoss(false);
        } else{
            if(getWindowDegree() < getMAX_WINDOW_SIZE()){       
                setWindowDegree(getWindowDegree()+ ADDITIVE_FACTOR);
            }
        }
    }
    ///////////////////////////////////////////////////////////////////////////
}




