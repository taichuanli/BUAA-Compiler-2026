package middleEnd.utils;

import middleEnd.FuncFParam;
import middleEnd.Insts.CallInst;
import middleEnd.LLVMInstruction;
import middleEnd.UsableValue;

import java.util.LinkedList;

public class RegTracker {
    private final int scopeId;
    private final LinkedList<UsableValue> usableValues = new LinkedList<>();
    private int regNo = 0; // 已经用过的编号

    public RegTracker(int scopeId) {
        this.scopeId = scopeId;
    }

    public void addValue(UsableValue inst) {
        usableValues.add(inst);
    }

    public void addInstructions(LinkedList<LLVMInstruction> instructions) {
        for (LLVMInstruction inst : instructions) {
            if (inst instanceof UsableValue) {
                usableValues.add((UsableValue) inst);
            }
        }
    }

    public void setRegNo() {
        boolean FParamsIsEnd = false;
        for (UsableValue value : usableValues) {
            if (!(value instanceof FuncFParam) && !FParamsIsEnd) {
                FParamsIsEnd = true;
                regNo++;
            }
            if (value instanceof CallInst callInst) {
                if (callInst.isVoid()) {
                    continue;
                }
            }
            value.setVirtualRegNo(regNo++);
        }
    }

    @Override
    public String toString() {
        return String.format("RegTracker<%d>:%d", scopeId, regNo);
    }

    public int getRegNo() {
        return regNo;
    }
}
