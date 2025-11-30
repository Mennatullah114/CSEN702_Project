import java.util.*;

public class Cache {
    public static class CacheBlock {
        public boolean valid;
        public int tag;
        public byte[] data;
        
        public CacheBlock(int blockSize) {
            this.valid = false;
            this.tag = -1;
            this.data = new byte[blockSize];
        }
    }
    
    private int cacheSize;      // Total cache size in bytes
    private int blockSize;      // Block size in bytes
    private int numBlocks;      // Number of blocks in cache
    private CacheBlock[] blocks;
    
    private Memory memory;
    
    public Cache(int cacheSize, int blockSize, Memory memory) {
        this.cacheSize = cacheSize;
        this.blockSize = blockSize;
        this.numBlocks = cacheSize / blockSize;
        this.blocks = new CacheBlock[numBlocks];
        this.memory = memory;
        
        for (int i = 0; i < numBlocks; i++) {
            blocks[i] = new CacheBlock(blockSize);
        }
    }
    
    // Direct-mapped cache: address maps to index = (address / blockSize) % numBlocks
    // tag = address / blockSize
    public boolean isHit(int address) {
        int blockAddress = address / blockSize;
        int index = blockAddress % numBlocks;
        int tag = blockAddress;
        
        return blocks[index].valid && blocks[index].tag == tag;
    }
    
    public void access(int address) {
        if (!isHit(address)) {
            // Cache miss - fetch from memory
            fetchBlock(address);
        }
    }
    
    private void fetchBlock(int address) {
        int blockAddress = address / blockSize;
        int blockStartAddr = blockAddress * blockSize;
        int index = blockAddress % numBlocks;
        int tag = blockAddress;
        
        // Fetch entire block from memory
        CacheBlock block = blocks[index];
        block.valid = true;
        block.tag = tag;
        
        for (int i = 0; i < blockSize; i++) {
            block.data[i] = memory.mem[blockStartAddr + i];
        }
    }
    
    public int loadWord(int address) {
        access(address);
        
        // After access, data is in cache
        int blockAddress = address / blockSize;
        int index = blockAddress % numBlocks;
        int offsetInBlock = address % blockSize;
        
        CacheBlock block = blocks[index];
        return ((block.data[offsetInBlock] & 0xFF) << 24) |
               ((block.data[offsetInBlock + 1] & 0xFF) << 16) |
               ((block.data[offsetInBlock + 2] & 0xFF) << 8) |
               ((block.data[offsetInBlock + 3] & 0xFF));
    }
    
    public void storeWord(int address, int value) {
        access(address);
        
        // Write-through: update both cache and memory
        int blockAddress = address / blockSize;
        int index = blockAddress % numBlocks;
        int offsetInBlock = address % blockSize;
        
        CacheBlock block = blocks[index];
        block.data[offsetInBlock] = (byte) ((value >> 24) & 0xFF);
        block.data[offsetInBlock + 1] = (byte) ((value >> 16) & 0xFF);
        block.data[offsetInBlock + 2] = (byte) ((value >> 8) & 0xFF);
        block.data[offsetInBlock + 3] = (byte) (value & 0xFF);
        
        // Write to memory as well
        memory.storeWord(address, value);
    }
    
    public List<String> getCacheStatus() {
        List<String> status = new ArrayList<>();
        for (int i = 0; i < numBlocks; i++) {
            CacheBlock block = blocks[i];
            if (block.valid) {
                status.add("Block " + i + ": Valid, Tag=" + block.tag);
            } else {
                status.add("Block " + i + ": Invalid");
            }
        }
        return status;
    }
    
    public void invalidate() {
        for (int i = 0; i < numBlocks; i++) {
            blocks[i].valid = false;
        }
    }
}