public class SimulatorConfig {
    // Station sizes
    public int fpAddStations = 3;
    public int fpMulStations = 2;
    public int loadBuffers = 3;
    public int intStations = 2;
    
    // Instruction latencies
    public int addSubLatency = 2;
    public int mulLatency = 10;
    public int divLatency = 40;
    public int loadLatency = 3;
    public int storeLatency = 2;
    public int intAluLatency = 1;
    public int branchLatency = 1;
    
    // Cache configuration
    public int cacheSize = 256;        // bytes
    public int blockSize = 16;         // bytes
    public int cacheHitLatency = 1;    // cycles
    public int cacheMissPenalty = 10;  // cycles
    
    // ROB size
    public int robSize = 16;
    
    public SimulatorConfig() {
        // Default values already set
    }
}