public class Memory {
    public byte[] mem = new byte[4096];

    public int loadWord(int address) {
        return ((mem[address] & 0xFF) << 24) |
               ((mem[address+1] & 0xFF) << 16) |
               ((mem[address+2] & 0xFF) <<   8) |
               ((mem[address+3] & 0xFF));
    }

    public void storeWord(int address, int value) {
        mem[address]   = (byte) ((value >> 24) & 0xFF);
        mem[address+1] = (byte) ((value >> 16) & 0xFF);
        mem[address+2] = (byte) ((value >> 8) & 0xFF);
        mem[address+3] = (byte) (value & 0xFF);
    }
}
