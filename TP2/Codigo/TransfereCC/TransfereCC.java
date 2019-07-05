
package TransfereCC;

/**
 *
 * @authors Rafael Silva, José Ramos
 */

import java.net.UnknownHostException;
import java.util.Scanner;
import java.net.SocketException;
 
public class TransfereCC {
 
    private static final int HEADER = 24;       // checksum:8, seqNum:4, flag:4, destinyPort:4, sourcePort:4 Bytes -> 24 Bytes Total
    private static final int PACK_SIZE = 1024;  // checksum:8, seqNum:4, flag:4, destinyPort:4, sourcePort:4, data<=1000 Bytes -> 1024 Bytes total
    private static final int WINDOW_SIZE = 20;
    private static final int TIMER_VALUE = 300;
    private static final int SERVER_PORT = 7777;
    private static final int ACK_PORT = 8888;
 
    private StateTable stateTable;
    
    public TransfereCC() {
       this.stateTable = new StateTable();
    }

    public TransfereCC(int portDestiny, int portEntry, String host, String cmd, String source) throws SocketException{
        this.stateTable = new StateTable(source,host,cmd,portEntry,portDestiny);
        try {
            System.out.println("\n> TransfereCC -> Destiny Port: " + portDestiny + ", Entry Port: " + portEntry+ ", IPAdress: "+host);
            System.out.println("> TransfereCC -> Command: " + cmd + ", Path Source: " + source);
            // criando threads para processar os dados
            startTransfereCC();
        } catch (UnknownHostException e) {
            System.out.println("> TransfereCC -> Error: " + e.getMessage());
            System.exit(-1);
        }
    }
    
    private void startTransfereCC() throws UnknownHostException{
        // Thread MonitorSender
        Thread monitorSender = new Thread(new SenderUDP(this.stateTable));
        monitorSender.start();
        System.out.println("> TransfereCC -> SenderUDP Thread Created..");
        // Thread MonitorReceiver
        Thread monitorReceiver = new Thread(new ReceiverUDP(this.stateTable));
        monitorReceiver.start();
        System.out.println("> TransfereCC -> ReceiverUDP Thread Created...\n");
    }

    public static void clearScreen() {  
        System.out.print("\033[H\033[2J");  
        System.out.flush();  
    }  
    
    public static void main(String[] args) throws SocketException{
        clearScreen();
        System.out.println("<----------------------> TransfereCC  <---------------------->");
        System.out.println(">> get <file_path_source>\n");
        Scanner in = new Scanner(System.in);
        System.out.print("IP Adress to send: ");
        String ip = in.nextLine();
        System.out.print("Enter command: ");
        String cmd = in.nextLine();
        String cmds[] = cmd.split(" ");
        TransfereCC transfereCC = new TransfereCC(SERVER_PORT, ACK_PORT, ip, cmds[0], cmds[1]); 
    }
}