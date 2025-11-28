import java.util.HashMap;

public class RegisterFile {
    public static class Register {
        public int value;
        public String tag;

        public Register(int v) {
            value = v;
            tag = null;
        }
    }

    private HashMap<String, Register> registers = new HashMap<>();

    public RegisterFile() {
        // Populate integer and FP registers
        for (int i = 0; i < 32; i++) {
            registers.put("R" + i, new Register(0));
            registers.put("F" + i, new Register(0));
        }
    }

    public Register get(String name) {
        return registers.get(name);
    }
}
