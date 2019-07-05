package Statistics;

/**
 *
 * @authors Rafael Silva, José Ramos
 */

public class InfoTransfer {
    
    private long totalTransfer;
    private double time;
    private boolean signature;

    public InfoTransfer(){
        this.totalTransfer = 0;
        this.time = 0;
        this.signature = false;
    }

    public void printInfoTransfer(StartTime time , int packetLossCount) {
        this.time = time.getTimeElapsed() / 1000; // Em segundos
        System.out.println("\n\n<------------> Information Transfer <------------>");
        if(this.totalTransfer <= 1024){
             System.out.println("> Total Transfer: " + this.totalTransfer + " Bytes");
        } else {
             System.out.println("> Total Transfer: " + this.totalTransfer/1024 + " Kb");
        }
        System.out.println("> Total Time to Transfer: " + this.time + " Seconds");
        System.out.printf("> Speed of Transfer: %.3f Kb/s\n",  ((this.totalTransfer/1024)/this.time));
        System.out.println("> Number of Packets Lost: "+ packetLossCount); 
        System.out.println("> Loss percentage: AAA%");                    
        System.out.println("> Digital Signature(DSA): " + this.signature);
        System.out.println("<------------------------------------------------>\n\n");
    }

    public long getTotalTransfer() {
        return this.totalTransfer;
    }

    public void setTotalTransfer(long i) {
        this.totalTransfer = i;
    }
    
    public void setSignature(boolean b){
        this.signature = b;
    }
}
