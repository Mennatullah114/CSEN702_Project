import java.util.*;

public class InstructionParser {

    public static List<Instruction> parse(String text) {
        List<Instruction> list = new ArrayList<>();
        String[] lines = text.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            list.add(parseLine(line));
        }
        return list;
    }

    private static Instruction parseLine(String line) {
        // Normalize spacing and remove commas
        line = line.replace(",", "");
        String[] parts = line.split("\\s+");

        String op = parts[0].toUpperCase();

        // ===========================
        //  Floating arithmetic: ADD.D / ADD.S etc.
        // ===========================
        if (op.matches("ADD\\.D|ADD\\.S|SUB\\.D|SUB\\.S|MUL\\.D|MUL\\.S|DIV\\.D|DIV\\.S")) {
            return new Instruction(
                    opcodeFromString(op),
                    parts[1], parts[2], parts[3],
                    0
            );
        }

        // ===========================
        //  Integer immediate instructions
        // ===========================
        if (op.equals("DADDI") || op.equals("DSUBI")) {
            return new Instruction(
                    opcodeFromString(op),
                    parts[1], parts[2], null,
                    Integer.parseInt(parts[3])
            );
        }

        // ===========================
        // Load: LW / LD / L.S / L.D
        // Format: LW R1, 8(R2)
        // ===========================
        if (op.equals("LW") || op.equals("LD") || op.equals("L.S") || op.equals("L.D")) {

            String rt = parts[1];
            String offset = parts[2];

            int imm = Integer.parseInt(offset.substring(0, offset.indexOf("(")));
            String base = offset.substring(offset.indexOf("(") + 1, offset.indexOf(")"));

            return new Instruction(
                    opcodeFromString(op),
                    rt, base, null,
                    imm
            );
        }

        // ===========================
        // Store: SW / SD / S.S / S.D
        // Format: S.D F4, 8(R1)
        // ===========================
        if (op.equals("SW") || op.equals("SD") || op.equals("S.S") || op.equals("S.D")) {

            String src = parts[1];   // the register whose value we store
            String offset = parts[2];

            int imm = Integer.parseInt(offset.substring(0, offset.indexOf("(")));
            String base = offset.substring(offset.indexOf("(") + 1, offset.indexOf(")"));

            return new Instruction(
                    opcodeFromString(op),
                    src, base, null,
                    imm
            );
        }

        // ===========================
        // Branches: BEQ R1 R2 offset
        // ===========================
        if (op.equals("BEQ") || op.equals("BNE")) {
            return new Instruction(
                    opcodeFromString(op),
                    null,              // no dest
                    parts[1], parts[2],
                    Integer.parseInt(parts[3])
            );
        }

        // ===========================
        // If we reach here -> invalid instruction
        // ===========================
        throw new RuntimeException("Unsupported or invalid instruction: " + line);
    }


    // Helper: map string op to enum opcode
    private static Instruction.OpCode opcodeFromString(String op) {
        switch (op) {
            case "DADDI": return Instruction.OpCode.DADDI;
            case "DSUBI": return Instruction.OpCode.DSUBI;

            case "ADD.D": return Instruction.OpCode.ADD_D;
            case "ADD.S": return Instruction.OpCode.ADD_S;
            case "SUB.D": return Instruction.OpCode.SUB_D;
            case "SUB.S": return Instruction.OpCode.SUB_S;
            case "MUL.D": return Instruction.OpCode.MUL_D;
            case "MUL.S": return Instruction.OpCode.MUL_S;
            case "DIV.D": return Instruction.OpCode.DIV_D;
            case "DIV.S": return Instruction.OpCode.DIV_S;

            case "LW":   return Instruction.OpCode.LW;
            case "LD":   return Instruction.OpCode.LD;
            case "L.S":  return Instruction.OpCode.L_S;
            case "L.D":  return Instruction.OpCode.L_D;

            case "SW":   return Instruction.OpCode.SW;
            case "SD":   return Instruction.OpCode.SD;
            case "S.S":  return Instruction.OpCode.S_S;
            case "S.D":  return Instruction.OpCode.S_D;

            case "BNE":  return Instruction.OpCode.BNE;
            case "BEQ":  return Instruction.OpCode.BEQ;
        }
        throw new RuntimeException("Unknown opcode: " + op);
    }
}
