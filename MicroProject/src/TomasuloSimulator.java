import java.util.*;

public class TomasuloSimulator {

    public List<Instruction> instructionQueue = new ArrayList<>();
    public RegisterFile registers;
    public ReorderBuffer rob;
    public List<ReservationStation> fpAddStations;
    public List<ReservationStation> fpMulStations;
    public List<ReservationStation> loadBuffers;
    public Memory memory = new Memory();
    
    // For tracking instruction state
    private int pc = 0;
    private boolean branchTaken = false;
    private int branchTarget = 0;
    
    // Common Data Bus - stores results ready to be broadcast
    private Map<String, Integer> cdb = new HashMap<>();

    public TomasuloSimulator() {
        registers = new RegisterFile();
        rob = new ReorderBuffer();

        fpAddStations = new ArrayList<>();
        fpMulStations = new ArrayList<>();
        loadBuffers = new ArrayList<>();
        
        for (int i = 0; i < 3; i++) fpAddStations.add(new ReservationStation("Add" + i));
        for (int i = 0; i < 2; i++) fpMulStations.add(new ReservationStation("Mul" + i));
        for (int i = 0; i < 3; i++) loadBuffers.add(new ReservationStation("Load" + i));
    }

    public void loadInstructions(List<Instruction> instr) {
        instructionQueue.addAll(instr);
    }

    public void step() {
        // Clear CDB from previous cycle
        cdb.clear();
        
        writeBack();  // Write back first to free up stations
        execute();
        issue();
        
        // Handle branch misprediction
        if (branchTaken) {
            // Flush instructions after branch
            instructionQueue.clear();
            pc = branchTarget;
            branchTaken = false;
        }
    }

    private void issue() {
        if (instructionQueue.isEmpty()) return;

        Instruction inst = instructionQueue.get(0);
        ReservationStation station = findAvailableStation(inst);
        
        if (station == null) return; // No station available
        
        // Allocate ROB entry
        int robIndex = rob.add(inst.dest);
        String robTag = "ROB" + robIndex;
        
        // Configure reservation station
        station.busy = true;
        station.op = inst.op.toString();
        station.immediate = inst.immediate;
        station.resultTag = robTag;
        
        // Set Vj, Vk, Qj, Qk based on register status
        if (inst.src1 != null) {
            RegisterFile.Register src1Reg = registers.get(inst.src1);
            if (src1Reg.tag == null) {
                station.Vj = String.valueOf(src1Reg.value);
                station.Qj = null;
            } else {
                station.Vj = null;
                station.Qj = src1Reg.tag;
            }
        }
        
        if (inst.src2 != null) {
            RegisterFile.Register src2Reg = registers.get(inst.src2);
            if (src2Reg.tag == null) {
                station.Vk = String.valueOf(src2Reg.value);
                station.Qk = null;
            } else {
                station.Vk = null;
                station.Qk = src2Reg.tag;
            }
        }
        
        // Set latency based on operation type
        station.latencyRemaining = getLatency(inst.op);
        
        // Update register tag if this instruction writes to a register
        if (inst.dest != null) {
            registers.get(inst.dest).tag = robTag;
        }
        
        instructionQueue.remove(0);
    }

    private void execute() {
        // Process all station types
        executeStations(fpAddStations);
        executeStations(fpMulStations);
        executeStations(loadBuffers);
    }
    
    private void executeStations(List<ReservationStation> stations) {
        for (ReservationStation station : stations) {
            if (!station.busy || station.readyToBroadcast) continue;
            
            // Check if operands are ready
            if ((station.Qj != null && !station.Qj.equals("0")) || 
                (station.Qk != null && !station.Qk.equals("0"))) {
                continue; // Operands not ready
            }
            
            if (station.latencyRemaining > 0) {
                station.latencyRemaining--;
                
                if (station.latencyRemaining == 0) {
                    // Execution complete, calculate result
                    calculateResult(station);
                    station.readyToBroadcast = true;
                }
            }
        }
    }
    
    private void calculateResult(ReservationStation station) {
        double vj = station.Vj != null ? Double.parseDouble(station.Vj) : 0;
        double vk = station.Vk != null ? Double.parseDouble(station.Vk) : 0;
        double result = 0;
        
        switch (station.op) {
            case "ADD.D":
            case "ADD.S":
                result = vj + vk;
                break;
            case "SUB.D":
            case "SUB.S":
                result = vj - vk;
                break;
            case "MUL.D":
            case "MUL.S":
                result = vj * vk;
                break;
            case "DIV.D":
            case "DIV.S":
                result = vj / vk;
                break;
            case "DADDI":
                result = vj + station.immediate;
                break;
            case "DSUBI":
                result = vj - station.immediate;
                break;
            case "LW":
            case "LD":
            case "L.S":
            case "L.D":
                // Calculate effective address and load from memory
                int address = (int)vj + station.immediate;
                result = memory.loadWord(address);
                break;
            case "BEQ":
                branchTaken = (vj == vk);
                branchTarget = pc + station.immediate;
                break;
            case "BNE":
                branchTaken = (vj != vk);
                branchTarget = pc + station.immediate;
                break;
        }
        
        station.resultValue = result;
        
        // For stores, don't calculate result but mark as ready
        if (station.op.startsWith("S.")) {
            station.readyToBroadcast = true;
        }
    }

    private void writeBack() {
        // Broadcast results from ready stations
        broadcastResults(fpAddStations);
        broadcastResults(fpMulStations);
        broadcastResults(loadBuffers);
        
        // Commit from ROB
        commit();
    }
    
    private void broadcastResults(List<ReservationStation> stations) {
        for (ReservationStation station : stations) {
            if (station.busy && station.readyToBroadcast) {
                // Broadcast on CDB
                cdb.put(station.resultTag, (int)station.resultValue);
                
                // Update waiting reservation stations
                updateWaitingStations(station.resultTag, (int)station.resultValue);
                
                // Clear the station
                station.clear();
            }
        }
    }
    
    private void updateWaitingStations(String tag, int value) {
        // Update all stations waiting for this tag
        List<ReservationStation> allStations = getAllStations();
        for (ReservationStation station : allStations) {
            if (station.busy) {
                if (tag.equals(station.Qj)) {
                    station.Vj = String.valueOf(value);
                    station.Qj = null;
                }
                if (tag.equals(station.Qk)) {
                    station.Vk = String.valueOf(value);
                    station.Qk = null;
                }
            }
        }
        
        // Update registers
        for (int i = 0; i < 32; i++) {
            RegisterFile.Register reg = registers.get("R" + i);
            if (tag.equals(reg.tag)) {
                reg.value = value;
                reg.tag = null;
            }
            
            RegisterFile.Register freg = registers.get("F" + i);
            if (tag.equals(freg.tag)) {
                freg.value = value;
                freg.tag = null;
            }
        }
    }
    
    private void commit() {
        ReorderBuffer.ROBEntry entry = rob.popIfReady();
        while (entry != null) {
            // For store instructions, perform the actual memory store
            if (isStoreInstruction(entry)) {
                // Store implementation would go here
                // memory.storeWord(address, entry.value);
            }
            
            // Mark entry as committed and remove from ROB
            entry.busy = false;
            
            // Get next entry
            entry = rob.popIfReady();
        }
    }
    
    private ReservationStation findAvailableStation(Instruction inst) {
        List<ReservationStation> targetStations = getStationsForInstruction(inst);
        
        for (ReservationStation station : targetStations) {
            if (!station.busy) {
                return station;
            }
        }
        return null; // No available station
    }
    
    private List<ReservationStation> getStationsForInstruction(Instruction inst) {
        switch (inst.op) {
            case ADD_D:
            case ADD_S:
            case SUB_D:
            case SUB_S:
            case DADDI:
            case DSUBI:
                return fpAddStations;
                
            case MUL_D:
            case MUL_S:
            case DIV_D:
            case DIV_S:
                return fpMulStations;
                
            case LW:
            case LD:
            case L_S:
            case L_D:
            case SW:
            case SD:
            case S_S:
            case S_D:
                return loadBuffers;
                
            case BEQ:
            case BNE:
                return fpAddStations; // Branches use integer units
                
            default:
                return new ArrayList<>();
        }
    }
    
    private int getLatency(Instruction.OpCode op) {
        switch (op) {
            case ADD_D:
            case ADD_S:
            case SUB_D:
            case SUB_S:
            case DADDI:
            case DSUBI:
                return 2;
                
            case MUL_D:
            case MUL_S:
                return 10;
                
            case DIV_D:
            case DIV_S:
                return 40;
                
            case LW:
            case LD:
            case L_S:
            case L_D:
            case SW:
            case SD:
            case S_S:
            case S_D:
                return 3;
                
            case BEQ:
            case BNE:
                return 1;
                
            default:
                return 1;
        }
    }
    
    private boolean isStoreInstruction(ReorderBuffer.ROBEntry entry) {
        // Check if this ROB entry corresponds to a store instruction
        // This would need to track the original instruction type
        return entry.dest != null && entry.dest.startsWith("MEM");
    }
    
    public List<ReservationStation> getAllStations() {
        List<ReservationStation> all = new ArrayList<>();
        all.addAll(fpAddStations);
        all.addAll(fpMulStations);
        all.addAll(loadBuffers);
        return all;
    }
    
    public void loadProgram(List<Instruction> instructions) {
        instructionQueue.clear();
        instructionQueue.addAll(instructions);
        pc = 0;
        branchTaken = false;
    }
}