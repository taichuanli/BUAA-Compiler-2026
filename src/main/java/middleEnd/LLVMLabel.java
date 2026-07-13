package middleEnd;

import static middleEnd.LLVMType.InstType.LABEL;

public class LLVMLabel extends LLVMInstruction implements UsableValue {
    private int regNo;
    public LLVMLabel() {
        super(LABEL);
    }

    @Override
    public String toValueIR() {
        return String.format("%%%d", regNo);
    }

    @Override
    public String toLLVMType() {
        return "";
    }

    @Override
    public int toAlign() {
        return 0;
    }

    @Override
    public void setVirtualRegNo(int regNo) {
        this.regNo = regNo;
    }

    @Override
    public int getMemorySize() {
        return 0;
    }

    @Override
    public String toString() {
        return String.format("\n%d: ", regNo);
    }
}
