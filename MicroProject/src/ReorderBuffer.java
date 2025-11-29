import java.util.LinkedList;

public class ReorderBuffer {
    public static class ROBEntry {
        public int id;
        public boolean busy;
        public String dest;   // destination register name (or "MEM" for stores)
        public int value;
        public boolean ready;
        public boolean isStore;
        public int storeAddress;
        public int storeValue;

        public ROBEntry(int id, String dest, boolean isStore) {
            this.id = id;
            this.dest = dest;
            this.isStore = isStore;
            this.busy = true;
            this.ready = false;
        }
    }

    public LinkedList<ROBEntry> queue = new LinkedList<>();

    public int add(String dest, boolean isStore) {
        int id = queue.size(); // simple id: current size (will shift on pop but tags are strings "ROB"+id used only locally)
        ROBEntry e = new ROBEntry(id, dest, isStore);
        queue.add(e);
        return id;
    }

    public ROBEntry peek() {
        return queue.peekFirst();
    }

    public ROBEntry popIfReady() {
        ROBEntry e = queue.peekFirst();
        if (e != null && e.ready) {
            return queue.removeFirst();
        }
        return null;
    }
}
