import java.util.*;

public class TomasuloSimulator {

    public RegisterFile registers;
    public ReorderBuffer rob;
    public Cache cache;
    public Memory memory;
    public SimulatorConfig config;

    public List<ReservationStation> fpAddStations;
    public List<ReservationStation> fpMulStations;
    public List<ReservationStation> loadBuffers;
    public List<ReservationStation> intStations;

    public List<Instruction> instructionQueue = new ArrayList<>();
    
    // Track pending cache operations
    private Map<ReservationStation, Integer> cachePendingCycles = new HashMap<>();

    public TomasuloSimulator(SimulatorConfig config) {
        this.config = config;
        registers = new RegisterFile();
        rob = new ReorderBuffer();
        memory = new Memory();
        cache = new Cache(config.cacheSize, config.blockSize, memory);

        fpAddStations = new ArrayList<>();
        fpMulStations = new ArrayList<>();
        loadBuffers = new ArrayList<>();
        intStations = new ArrayList<>();

        // Create stations based on config
        for (int i = 0; i < config.fpAddStations; i++) 
            fpAddStations.add(new ReservationStation("Add" + i));
        for (int i = 0; i < config.fpMulStations; i++) 
            fpMulStations.add(new ReservationStation("Mul" + i));
        for (int i = 0; i < config.loadBuffers; i++) 
            loadBuffers.add(new ReservationStation("Load" + i));
        for (int i = 0; i < config.intStations; i++) 
            intStations.add(new ReservationStation("Int" + i));
    }

    public void loadProgram(List<Instruction> instructions) {
        instructionQueue.clear();
        instructionQueue.addAll(instructions);
    }

    public void step() {
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

        boolean isStore = isStore(inst.op);
        String dest = inst.dest;
        int robIndex = rob.add(dest == null ? "MEM" : dest, isStore);

        rs.busy = true;
        rs.op = inst.op.toString();
        rs.robIndex = robIndex;

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
                break;

            default:
                break;
        }

        if (!isStore && inst.dest != null) {
            RegisterFile.Register reg = registers.get(inst.dest);
            if (reg != null) {
                reg.tag = "ROB" + robIndex;
            }
        }

        System.out.println("Issued to " + rs.name + " op=" + rs.op + " rob=ROB" + robIndex);
        instructionQueue.remove(0);
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
            } else if (rs.op != null && isStoreOpString(rs.op)) {
                if (!(readyJ && readyK)) continue;
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

        // Handle multiple writebacks: prioritize older instructions (lower ROB index)
        finished.sort(Comparator.comparingInt(rs -> rs.robIndex));

        for (ReservationStation rs : finished) {
            int robId = rs.robIndex;
            if (robId < 0 || robId >= rob.queue.size()) continue;
            
            ReorderBuffer.ROBEntry entry = rob.queue.get(robId);
            System.out.println("WriteBack from " + rs.name + " op=" + rs.op + " rob=ROB" + robId);

            int result = 0;
            double resultDouble = 0.0;
            boolean useDouble = false;
            
            try {
                switch (rs.op) {
                    case "ADD.D": case "ADD.S":
                        resultDouble = Double.parseDouble(rs.Vj == null ? "0" : rs.Vj) + 
                                      Double.parseDouble(rs.Vk == null ? "0" : rs.Vk);
                        useDouble = true;
                        break;
                    case "SUB.D": case "SUB.S":
                        resultDouble = Double.parseDouble(rs.Vj == null ? "0" : rs.Vj) - 
                                      Double.parseDouble(rs.Vk == null ? "0" : rs.Vk);
                        useDouble = true;
                        break;
                    case "MUL.D": case "MUL.S":
                        resultDouble = Double.parseDouble(rs.Vj == null ? "0" : rs.Vj) * 
                                      Double.parseDouble(rs.Vk == null ? "0" : rs.Vk);
                        useDouble = true;
                        break;
                    case "DIV.D": case "DIV.S":
                        double denom = Double.parseDouble(rs.Vk == null ? "1" : rs.Vk);
                        resultDouble = denom == 0 ? 0 : 
                                      Double.parseDouble(rs.Vj == null ? "0" : rs.Vj) / denom;
                        useDouble = true;
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
                        entry.isStore = true;
                        entry.storeAddress = (rs.effectiveAddress != null) ? rs.effectiveAddress : 0;
                        entry.storeValue = (int)Double.parseDouble(rs.Vj == null ? "0" : rs.Vj);
                        entry.ready = true;
                        result = entry.storeValue;
                        break;
                    case "BEQ":
                        result = (Double.parseDouble(rs.Vj == null ? "0" : rs.Vj) == 
                                 Double.parseDouble(rs.Vk == null ? "0" : rs.Vk)) ? 1 : 0;
                        break;
                    case "BNE":
                        result = (Double.parseDouble(rs.Vj == null ? "0" : rs.Vj) != 
                                 Double.parseDouble(rs.Vk == null ? "0" : rs.Vk)) ? 1 : 0;
                        break;
                    default:
                        result = 0;
                }
            } catch (Exception ex) {
                result = 0;
                resultDouble = 0.0;
            }

            if (!entry.isStore) {
                if (useDouble) {
                    entry.valueDouble = resultDouble;
                    entry.value = (int)resultDouble;  // For display compatibility
                } else {
                    entry.value = result;
                    entry.valueDouble = result;
                }
                entry.ready = true;
            }

            String tag = "ROB" + robId;
            for (ReservationStation other : all) {
                if (!other.busy) continue;
                if (tag.equals(other.Qj)) {
                    other.Vj = useDouble ? Double.toString(resultDouble) : Integer.toString(result);
                    other.Qj = null;
                }
                if (tag.equals(other.Qk)) {
                    other.Vk = useDouble ? Double.toString(resultDouble) : Integer.toString(result);
                    other.Qk = null;
                }
            }

            rs.clear();
        }
    }

    // -------------------------
    // COMMIT
    // -------------------------
    private void commit() {
        ReorderBuffer.ROBEntry head = rob.peek();
        if (head == null) return;
        if (!head.ready) return;

        System.out.println("Commit ROB" + head.id + " dest=" + head.dest + " ready=" + head.ready);

        if (head.isStore) {
            cache.storeWord(head.storeAddress, head.storeValue);
            rob.popIfReady();
            return;
        }

        if (head.dest != null && !head.dest.equals("MEM")) {
            RegisterFile.Register reg = registers.get(head.dest);
            if (reg != null && ("ROB" + head.id).equals(reg.tag)) {
                // Enforce type: F registers get double, R registers get int
                if (head.dest.startsWith("F")) {
                    reg.value = head.valueDouble;
                } else if (head.dest.startsWith("R")) {
                    reg.value = (int)head.valueDouble;  // Truncate to integer for R registers
                }
                reg.tag = null;
            }
        }

        rob.popIfReady();
    }

    // -------------------------
    // Helpers
    // -------------------------
    public List<ReservationStation> getAllStations() {
        List<ReservationStation> all = new ArrayList<>();
        all.addAll(fpAddStations);
        all.addAll(fpMulStations);
        all.addAll(loadBuffers);
        all.addAll(intStations);
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
            case SW: case SD: case S_S: case S_D:
            case BEQ: case BNE:
                for (ReservationStation s : intStations) if (!s.busy) return s;
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
}