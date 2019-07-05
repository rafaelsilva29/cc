
package TransfereCC;

import java.util.TimerTask;

/**
 *
 * @author Rafael Silva, José Ramos
 */
    
public class Temporizador extends TimerTask {
    
    private StateTable stateTable;
    private static final int DIVISIVE_FACTOR = 2;
    private int decrease;
    
    public Temporizador( StateTable st){
        this.stateTable = st;
        this.decrease = DIVISIVE_FACTOR; 
    }
    
    public Temporizador(){}

    @Override
    public void run() {
        try{
            System.out.println("> TransfereCC -> Time Out...");
            // Reseta numero de sequencia
            this.stateTable.setPacketLoss(true);
            this.stateTable.congestionControl();
            this.stateTable.setProxNumSeq(this.stateTable.getWindowDegree());
        } catch(Exception e){
            System.out.println("> TransfereCC -> Error: Leave, Time Out Exceeded...");
            System.exit(-2);
        }
    }
}
