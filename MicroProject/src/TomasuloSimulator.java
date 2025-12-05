import java.util.*;

public class TomasuloSimulator {

    public RegisterFile registers;
  
    public Cache cache;
    public Memory memory;
    public SimulatorConfig config;

    public List<ReservationStation> fpAddStations;
    public List<ReservationStation> fpMulStations;
    public List<ReservationStation> loadBuffers;
    public List<ReservationStation> storeBuffers;  // Separated from loads

    public List<Instruction> instructionQueue = new ArrayList<>();
    private List<Instruction> originalProgram = new ArrayList<>();

    
    public int clockCycle = 0;
    public int pc = 0;  // Program counter
    
    // Track pending cache operations
    private Map<ReservationStation, Integer> cachePendingCycles = new HashMap<>();
    
    // Cache miss notification callback
    public interface CacheMissListener {
        void onCacheMiss(int address);
    }
    private CacheMissListener cacheMissListener;
    
    // Address clash notification callback
    public interface AddressClashListener {
        void onAddressClash(String stationName, int address, String reason);
    }
    private AddressClashListener addressClashListener;

    public TomasuloSimulator(SimulatorConfig config) {
        this.config = config;
        registers = new RegisterFile();
      
        memory = new Memory();
        cache = new Cache(config.cacheSize, config.blockSize, memory);

        fpAddStations = new ArrayList<>();
        fpMulStations = new ArrayList<>();
        loadBuffers = new ArrayList<>();
        storeBuffers = new ArrayList<>();

        // Create stations based on config
        for (int i = 0; i < config.fpAddStations; i++) 
            fpAddStations.add(new ReservationStation("Add" + i));
        for (int i = 0; i < config.fpMulStations; i++) 
            fpMulStations.add(new ReservationStation("Mul" + i));
        for (int i = 0; i < config.loadBuffers; i++) 
            loadBuffers.add(new ReservationStation("Load" + i));
        for (int i = 0; i < config.intStations; i++) 
            storeBuffers.add(new ReservationStation("Store" + i));
    }

    public void loadProgram(List<Instruction> instructions) {
        originalProgram = new ArrayList<>(instructions);
        instructionQueue.clear();
        instructionQueue.addAll(instructions);
        clockCycle = 0;
        pc = 0;
    }
    
    public void setCacheMissListener(CacheMissListener listener) {
        this.cacheMissListener = listener;
    }
    
    public void setAddressClashListener(AddressClashListener listener) {
        this.addressClashListener = listener;
    }

    public void step() {
        clockCycle++;
        commit();
        writeBack();
        execute();
        issue();
    }

    // -------------------------
    // ISSUE
    // -------------------------
    private void issue() {
        if (instructionQueue.isEmpty()) return;

        Instruction inst = instructionQueue.get(0);
        ReservationStation rs = findFreeStationFor(inst.op);
        if (rs == null) {
            return;
        }

        // No ROB - use RS name as tag
        rs.busy = true;
        rs.op = inst.op.toString();
        rs.dest = inst.dest;  // Store destination register

        switch (inst.op) {
            case ADD_D: case ADD_S:
            case SUB_D: case SUB_S:
            case MUL_D: case MUL_S:
            case DIV_D: case DIV_S:
                bindSourceToRS(rs, inst.src1, true);
                bindSourceToRS(rs, inst.src2, false);
                break;

            case DADDI: case DSUBI:
                bindSourceToRS(rs, inst.src1, true);
                rs.Vk = Integer.toString(inst.immediate);
                rs.Qk = null;
                break;

            case LW: case LD: case L_S: case L_D:
                bindSourceToRS(rs, inst.src1, true);
                rs.Vk = Integer.toString(inst.immediate);
                rs.Qk = null;
                break;

            case SW: case SD: case S_S: case S_D:
                bindSourceToRS(rs, inst.dest, true);
                bindSourceToRS(rs, inst.src1, false);
                rs.effectiveAddress = inst.immediate;
                break;

            case BEQ: case BNE:
                bindSourceToRS(rs, inst.src1, true);
                bindSourceToRS(rs, inst.src2, false);
                rs.branchTarget = inst.immediate;  // store jump target here
                break;

            default:
                break;
        }

        // Mark destination register as waiting on this RS (not ROB)
        if (!isStore(inst.op) && inst.dest != null) {
            RegisterFile.Register reg = registers.get(inst.dest);
            if (reg != null) {
                reg.tag = rs.name;  // Use RS name instead of ROB tag
            }
        }
        //for branching
        rs.pcAtIssue = pc; 

        System.out.println("Issued to " + rs.name + " op=" + rs.op);
        instructionQueue.remove(0);
        pc += 4;  // Increment PC
    }

    // -------------------------
    // EXECUTE
    // -------------------------
    private void execute() {
        List<ReservationStation> all = getAllStations();
        
        for (ReservationStation rs : all) {
            if (!rs.busy) continue;

            boolean readyJ = (rs.Qj == null);
            boolean readyK = (rs.Qk == null);

            if (rs.op != null && isLoadOpString(rs.op)) {
                if (!readyJ) continue;
                
                // Address clash check for loads: ensure no earlier stores to same address
                if (!rs.startedExecution && rs.Qj == null) {
                    int loadAddr = Integer.parseInt(rs.Vj == null ? "0" : rs.Vj) + 
                                  Integer.parseInt(rs.Vk == null ? "0" : rs.Vk);
                    if (hasAddressClash(rs, loadAddr)) {
                        String reason = "Earlier store to same address not yet completed";
                        System.out.println(rs.name + " stalled due to address clash at " + loadAddr);
                        if (addressClashListener != null) {
                            addressClashListener.onAddressClash(rs.name, loadAddr, reason);
                        }
                        continue;  // Stall this load
                    }
                }
            } else if (rs.op != null && isStoreOpString(rs.op)) {
                if (!(readyJ && readyK)) continue;
                
                // Address clash check for stores: ensure no earlier loads/stores to same address
                if (!rs.startedExecution) {
                    int storeAddr = Integer.parseInt(rs.Vk == null ? "0" : rs.Vk) + 
                                   (rs.effectiveAddress != null ? rs.effectiveAddress : 0);
                    if (hasAddressClash(rs, storeAddr)) {
                        String reason = "Earlier memory operation to same address not yet completed";
                        System.out.println(rs.name + " stalled due to address clash at " + storeAddr);
                        if (addressClashListener != null) {
                            addressClashListener.onAddressClash(rs.name, storeAddr, reason);
                        }
                        continue;  // Stall this store
                    }
                }
            } else {
                if (!readyJ || !readyK) continue;
            }

            // Handle cache access for loads/stores
            if (isLoadOpString(rs.op) || isStoreOpString(rs.op)) {
                if (cachePendingCycles.containsKey(rs)) {
                    // Cache operation in progress
                    int remaining = cachePendingCycles.get(rs);
                    if (remaining > 1) {
                        cachePendingCycles.put(rs, remaining - 1);
                        continue;
                    } else {
                        // Cache operation complete
                        cachePendingCycles.remove(rs);
                        rs.latencyRemaining = 0; // Ready for writeback
                    }
                } else if (rs.latencyRemaining == 0 && !rs.startedExecution) {
                    // Start cache access
                    rs.startedExecution = true;
                    
                    // Compute address
                    int address = 0;
                    if (isLoadOpString(rs.op)) {
                        int base = Integer.parseInt(rs.Vj == null ? "0" : rs.Vj);
                        int offset = Integer.parseInt(rs.Vk == null ? "0" : rs.Vk);
                        address = base + offset;
                        rs.effectiveAddress = address;
                    } else {
                        int base = Integer.parseInt(rs.Vk == null ? "0" : rs.Vk);
                        int offset = (rs.effectiveAddress != null) ? rs.effectiveAddress : 0;
                        address = base + offset;
                        rs.effectiveAddress = address;
                    }
                    
                    // Check cache and set latency
                    boolean hit = cache.isHit(address);
                    int latency = hit ? config.cacheHitLatency : 
                                       (config.cacheHitLatency + config.cacheMissPenalty);
                    
                    System.out.println(rs.name + " accessing address " + address + 
                                     " - " + (hit ? "HIT" : "MISS") + 
                                     " (latency=" + latency + ")");
                    
                    // Notify on cache miss
                    if (!hit && cacheMissListener != null) {
                        cacheMissListener.onCacheMiss(address);
                    }
                    
                    if (latency > 0) {
                        cachePendingCycles.put(rs, latency);
                    } else {
                        rs.latencyRemaining = 0;
                    }
                }
            } else {
                // Non-memory operations
                if (rs.latencyRemaining == 0) {
                    rs.startedExecution = true;
                    rs.latencyRemaining = latencyForOp(rs.op);
                } else {
                    rs.latencyRemaining = Math.max(0, rs.latencyRemaining - 1);
                }
            }
        }
    }

    // -------------------------
    // WRITE BACK
    // -------------------------
    private void writeBack() {
        List<ReservationStation> all = getAllStations();
        List<ReservationStation> finished = new ArrayList<>();
        
        for (ReservationStation rs : all) {
            if (!rs.busy) continue;
            if (rs.latencyRemaining > 0) continue;
            if (!rs.startedExecution) continue;
            if (cachePendingCycles.containsKey(rs)) continue;
            finished.add(rs);
        }

        // Handle multiple writebacks: prioritize first in list
        for (ReservationStation rs : finished) {
            System.out.println("WriteBack from " + rs.name + " op=" + rs.op);

            double result = 0.0;
            boolean isStoreOp = isStoreOpString(rs.op);
            
            try {
                switch (rs.op) {
                    case "ADD.D": case "ADD.S":
                        result = Double.parseDouble(rs.Vj == null ? "0" : rs.Vj) + 
                                Double.parseDouble(rs.Vk == null ? "0" : rs.Vk);
                        break;
                    case "SUB.D": case "SUB.S":
                        result = Double.parseDouble(rs.Vj == null ? "0" : rs.Vj) - 
                                Double.parseDouble(rs.Vk == null ? "0" : rs.Vk);
                        break;
                    case "MUL.D": case "MUL.S":
                        result = Double.parseDouble(rs.Vj == null ? "0" : rs.Vj) * 
                                Double.parseDouble(rs.Vk == null ? "0" : rs.Vk);
                        break;
                    case "DIV.D": case "DIV.S":
                        double denom = Double.parseDouble(rs.Vk == null ? "1" : rs.Vk);
                        result = denom == 0 ? 0 : 
                                Double.parseDouble(rs.Vj == null ? "0" : rs.Vj) / denom;
                        break;
                    case "DADDI":
                        result = (int)Double.parseDouble(rs.Vj == null ? "0" : rs.Vj) + 
                                (int)Double.parseDouble(rs.Vk == null ? "0" : rs.Vk);
                        break;
                    case "DSUBI":
                        result = (int)Double.parseDouble(rs.Vj == null ? "0" : rs.Vj) - 
                                (int)Double.parseDouble(rs.Vk == null ? "0" : rs.Vk);
                        break;
                    case "LW": case "LD": case "L.S": case "L.D":
                        result = cache.loadWord(rs.effectiveAddress);
                        break;
                    case "SW": case "SD": case "S.S": case "S.D":
                        // Store: write directly to cache/memory
                        int storeValue = (int)Double.parseDouble(rs.Vj == null ? "0" : rs.Vj);
                        cache.storeWord(rs.effectiveAddress, storeValue);
                        break;
                    case "BNE":
                        if (Double.parseDouble(rs.Vj) != Double.parseDouble(rs.Vk)) {
                            // Calculate target: PC of branch + 4 + (offset * 4)
                            int targetPC = rs.pcAtIssue + 4 + (rs.branchTarget * 4);
                            pc = targetPC;
                            // Clear instruction queue as we need to fetch from new location
                            instructionQueue.clear();
                            System.out.println("Branch taken to PC=" + targetPC);
                        }
                        break;

                    case "BEQ":
                        if (Double.parseDouble(rs.Vj) == Double.parseDouble(rs.Vk)) {
                            int targetPC = rs.pcAtIssue + 4 + (rs.branchTarget * 4);
                            pc = targetPC;
                            instructionQueue.clear();
                            System.out.println("Branch taken to PC=" + targetPC);
                        }
                        break;

                    default:
                        result = 0;
                }
            } catch (Exception ex) {
                result = 0;
            }

            // Broadcast result using RS name as tag
            String tag = rs.name;
            for (ReservationStation other : all) {
                if (!other.busy) continue;
                if (tag.equals(other.Qj)) {
                    other.Vj = Double.toString(result);
                    other.Qj = null;
                }
                if (tag.equals(other.Qk)) {
                    other.Vk = Double.toString(result);
                    other.Qk = null;
                }
            }
            
            // Store result for commit
            rs.result = result;
            rs.ready = true;
        

        }
    }

    // -------------------------
    // COMMIT: Write results from ready RS to register file
    // -------------------------
    private void commit() {
        List<ReservationStation> all = getAllStations();
        
        for (ReservationStation rs : all) {
            if (!rs.busy) continue;
            if (!rs.ready) continue;
            
            System.out.println("Commit from " + rs.name + " dest=" + rs.dest);
            
            // Write to register file if register is still tagged with this RS
            if (rs.dest != null && !rs.dest.isEmpty()) {
                RegisterFile.Register reg = registers.get(rs.dest);
                if (reg != null && rs.name.equals(reg.tag)) {
                    // Enforce type: F registers get double, R registers get int
                    if (rs.dest.startsWith("F")) {
                        reg.value = rs.result;
                    } else if (rs.dest.startsWith("R")) {
                        reg.value = (int)rs.result;
                    }
                    reg.tag = null;
                }
            }
            
            // Clear the RS
            rs.clear();
        }
    }

    // -------------------------
    // Helpers
    // -------------------------
    public List<ReservationStation> getAllStations() {
        List<ReservationStation> all = new ArrayList<>();
        all.addAll(fpAddStations);
        all.addAll(fpMulStations);
        all.addAll(loadBuffers);
        all.addAll(storeBuffers);
        return all;
    }

    private ReservationStation findFreeStationFor(Instruction.OpCode op) {
        switch (op) {
            case ADD_D: case ADD_S: case SUB_D: case SUB_S:
                for (ReservationStation s : fpAddStations) if (!s.busy) return s;
                break;
            case MUL_D: case MUL_S: case DIV_D: case DIV_S:
                for (ReservationStation s : fpMulStations) if (!s.busy) return s;
                break;
            case LW: case LD: case L_S: case L_D:
                for (ReservationStation s : loadBuffers) if (!s.busy) return s;
                break;
            case DADDI: case DSUBI:
            case BEQ: case BNE:
                // Integer operations can use any FP add station as ALU
                for (ReservationStation s : fpAddStations) if (!s.busy) return s;
                break;
            case SW: case SD: case S_S: case S_D:
                for (ReservationStation s : storeBuffers) if (!s.busy) return s;
                break;
            default:
                return null;
        }
        return null;
    }

    private void bindSourceToRS(ReservationStation rs, String regName, boolean toVj) {
        if (regName == null) {
            if (toVj) { rs.Vj = "0"; rs.Qj = null; }
            else { rs.Vk = "0"; rs.Qk = null; }
            return;
        }

        RegisterFile.Register r = registers.get(regName);
        if (r == null) {
            if (toVj) { rs.Vj = "0"; rs.Qj = null; }
            else { rs.Vk = "0"; rs.Qk = null; }
            return;
        }

        if (r.tag != null) {
            if (toVj) { rs.Qj = r.tag; rs.Vj = null; }
            else { rs.Qk = r.tag; rs.Vk = null; }
        } else {
            // Format based on register type
            String valueStr;
            if (regName.startsWith("R")) {
                // Integer register - convert to int
                valueStr = Integer.toString((int)r.value);
            } else {
                // Floating point register - keep as double
                valueStr = Double.toString(r.value);
            }
            
            if (toVj) { rs.Vj = valueStr; rs.Qj = null; }
            else { rs.Vk = valueStr; rs.Qk = null; }
        }
    }

    private boolean isStore(Instruction.OpCode op) {
        return op == Instruction.OpCode.SW || op == Instruction.OpCode.SD || 
               op == Instruction.OpCode.S_S || op == Instruction.OpCode.S_D;
    }

    private boolean isLoadOpString(String op) {
        return op != null && (op.equals("LW") || op.equals("LD") || 
                             op.equals("L.S") || op.equals("L.D"));
    }

    private boolean isStoreOpString(String op) {
        return op != null && (op.equals("SW") || op.equals("SD") || 
                             op.equals("S.S") || op.equals("S.D"));
    }

    private int latencyForOp(String op) {
        if (op == null) return 1;
        if (op.startsWith("MUL")) return config.mulLatency;
        if (op.startsWith("DIV")) return config.divLatency;
        if (op.startsWith("ADD") || op.startsWith("SUB")) return config.addSubLatency;
        if (op.equals("DADDI") || op.equals("DSUBI")) return config.intAluLatency;
        if (op.startsWith("B")) return config.branchLatency;
        return 1;
    }
    
    // Check for address clashes between memory operations
    // Without ROB, we check based on issue order (earlier issued = earlier in getAllStations list)
    private boolean hasAddressClash(ReservationStation current, int currentAddr) {
        List<ReservationStation> all = getAllStations();
        int currentIndex = all.indexOf(current);
        
        // Check all earlier issued instructions (those before current in the list that are still busy)
        for (int i = 0; i < currentIndex; i++) {
            ReservationStation earlier = all.get(i);
            if (earlier.busy && !earlier.ready) {
                // Check if it's a memory operation with computed address
                if (isLoadOpString(earlier.op) || isStoreOpString(earlier.op)) {
                    if (earlier.effectiveAddress != null && earlier.effectiveAddress == currentAddr) {
                        // Address clash detected
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
}