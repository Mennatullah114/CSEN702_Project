public class ReservationStation {
    public String name;
    public boolean busy;

    // operation and operand fields (strings so GUI shows text)
    public String op;         // e.g. "ADD.D"
    public String Vj, Vk;     // operand values as strings
    public String Qj, Qk;     // tags like "Add0", "Load1"
    public int latencyRemaining;

    // bookkeeping
    public String dest;        // destination register
    public Integer effectiveAddress = null;
    public double result = 0.0;  // computed result
    public boolean ready = false;  // ready to commit

    // flag to ensure we only write-back after execution started
    public boolean startedExecution = false;

    public ReservationStation(String name) {
        this.name = name;
        clear();
    }

    public void clear() {
        busy = false;
        op = null;
        Vj = null;
        Vk = null;
        Qj = null;
        Qk = null;
        latencyRemaining = 0;
        dest = null;
        effectiveAddress = null;
        startedExecution = false;
        result = 0.0;
        ready = false;
    }
}
