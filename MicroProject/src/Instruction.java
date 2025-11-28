public class Instruction {

    public enum OpCode {
    	   DADDI, DSUBI,

    	    // Floating and double ops
    	    ADD_D, ADD_S,
    	    SUB_D, SUB_S,
    	    MUL_D, MUL_S,
    	    DIV_D, DIV_S,

    	    // Loads (integer + FP)
    	    LW, LD, L_S, L_D,

    	    // Stores (integer + FP)
    	    SW, SD, S_S, S_D,

    	    // Branches
    	    BNE, BEQ
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
