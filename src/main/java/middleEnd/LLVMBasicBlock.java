package middleEnd;

import java.util.LinkedList;

public class LLVMBasicBlock {
    public final String name;
    private LLVMLabel label;
    public final LinkedList<LLVMInstruction> instructions = new LinkedList<>();

    public LLVMBasicBlock(String name) {
        this.name = name;
    }

    public void addInst(LLVMInstruction inst) {
        instructions.add(inst);
    }

    public void setLLVMLabel(LLVMLabel label) {
        this.label = label;
    }
}
