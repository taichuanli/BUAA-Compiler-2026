package middleEnd;

import middleEnd.Insts.AllocaInst;

import java.util.LinkedList;

public class Block extends Value {
    private final LinkedList<LLVMInstruction> insts = new LinkedList<>();

    public void addInsts(LinkedList<LLVMInstruction> instructions) {
        insts.addAll(instructions);
    }

    public void addInst(LLVMInstruction instruction) {
        insts.add(instruction);
    }

    @Override
    public String toString() {
        StringBuilder block = new StringBuilder();
        block.append("{\n");
        for (LLVMInstruction inst : insts) {
            block.append("\t").append(inst.toString()).append("\n");
        }
        block.append("}");
        return block.toString();
    }

    public LinkedList<LLVMInstruction> getInstructions() {
        return insts;
    }

    public void sortAllocaInstsToFront() {
        LinkedList<LLVMInstruction> allocaInsts = new LinkedList<>();
        LinkedList<LLVMInstruction> otherInsts = new LinkedList<>();
        for (LLVMInstruction inst : insts) {
            if (inst instanceof AllocaInst) {
                allocaInsts.add(inst);
            } else {
                otherInsts.add(inst);
            }
        }
        insts.clear();
        insts.addAll(allocaInsts);
        insts.addAll(otherInsts);
    }
}
