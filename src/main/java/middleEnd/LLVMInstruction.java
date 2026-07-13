package middleEnd;

import backEnd.MIPSInst;

import java.util.HashMap;
import java.util.LinkedList;

public class LLVMInstruction extends User {
    public LLVMType.InstType type;
    public LLVMInstruction(LLVMType.InstType type) {
        this.type = type;
    }

    public LinkedList<MIPSInst> toMIPS() {
        throw new RuntimeException("LLVMInstruction.toMIPS() not implemented");
    }

    public HashMap<UsableValue, Integer> getReferencedValues() {
        return new HashMap<>();
    }
}
