public class Instruction {

	public enum OpCode {

	    // Integer ops
	    DADDI("DADDI"),
	    DSUBI("DSUBI"),

	    // Floating-point add/sub
	    ADD_D("ADD.D"),
	    ADD_S("ADD.S"),
	    SUB_D("SUB.D"),
	    SUB_S("SUB.S"),

	    // Floating-point multiply/divide
	    MUL_D("MUL.D"),
	    MUL_S("MUL.S"),
	    DIV_D("DIV.D"),
	    DIV_S("DIV.S"),

	    // Loads
	    LW("LW"),
	    LD("LD"),
	    L_S("L.S"),
	    L_D("L.D"),

	    // Stores
	    SW("SW"),
	    SD("SD"),
	    S_S("S.S"),
	    S_D("S.D"),

	    // Branches
	    BEQ("BEQ"),
	    BNE("BNE");

	    public final String text;

	    OpCode(String t) {
	        text = t;
	    }

	    @Override
	    public String toString() {
	        return text;
	    }
	}


    public OpCode op;
    public String dest;
    public String src1;
    public String src2;
    public int immediate;

    public Instruction(OpCode op, String dest, String src1, String src2, int imm) {
        this.op = op;
        this.dest = dest;
        this.src1 = src1;
        this.src2 = src2;
        this.immediate = imm;
    }

    @Override
    public String toString() {
        return op + " " + dest + ", " + src1 + ", " + src2;
    }
}
