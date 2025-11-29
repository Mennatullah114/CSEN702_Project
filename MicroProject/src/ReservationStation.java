public class ReservationStation {
    public String name;
    public boolean busy;

    // operation and operand fields (strings so GUI shows text)
    public String op;         // e.g. "ADD.D"
    public String Vj, Vk;     // operand values as strings
    public String Qj, Qk;     // tags like "ROB0"
    public int latencyRemaining;

    // bookkeeping
    public int robIndex = -1;
    public Integer effectiveAddress = null;

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
        robIndex = -1;
        effectiveAddress = null;
        startedExecution = false;
    }
}
