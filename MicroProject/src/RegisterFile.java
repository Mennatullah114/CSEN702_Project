import java.util.HashMap;

public class RegisterFile {
    public static class Register {
        public double value;  // Changed to double to support floating point
        public String tag;

        public Register(double v) {
            value = v;
            tag = null;
        }
    }

    private HashMap<String, Register> registers = new HashMap<>();

    public RegisterFile() {
        // Populate integer registers (R0-R31) - stored as doubles but used as integers
        for (int i = 0; i < 32; i++) {
            registers.put("R" + i, new Register(0.0));
        }
        // Populate floating point registers (F0-F31) - can hold actual floating point values
        for (int i = 0; i < 32; i++) {
            registers.put("F" + i, new Register(0.0));
        }
    }

    public Register get(String name) {
        return registers.get(name);
    }
}