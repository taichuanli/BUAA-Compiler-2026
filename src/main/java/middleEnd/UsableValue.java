package middleEnd;

public interface UsableValue {
    String toValueIR();
    String toLLVMType();
    int toAlign();
    void setVirtualRegNo(int regNo);
    int getMemorySize();
}
