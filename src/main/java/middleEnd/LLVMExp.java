package middleEnd;

import middleEnd.Insts.BinaryInst;

import java.util.LinkedList;

public class LLVMExp extends Value {
    LinkedList<LLVMInstruction> instructions = new LinkedList<>();
    UsableValue value;

    public LLVMExp(UsableValue value) {
        this.value = value;
    }

    public LLVMExp() {
        this.value = null;
    }

    public LinkedList<LLVMInstruction> getInstructions() {
        return instructions;
    }

    public LLVMExp binaryOperate(LLVMType.InstType instType, LLVMExp llvmExp) {
        instructions.addAll(llvmExp.instructions);
        UsableValue left = this.value;
        UsableValue right = llvmExp.value;
        BinaryInst newInst = new BinaryInst(instType, left, right);
        instructions.add(newInst);
        this.value = newInst;
        return this;
    }

    public LLVMExp negate() {
        BinaryInst newInst = new BinaryInst(LLVMType.InstType.SUB, new LLVMConst(LLVMType.TypeID.IntegerTyID, 0), this.value);
        instructions.add(newInst);
        this.value = newInst;
        return this;
    }

    public LLVMExp logicalNot() {
        BinaryInst newInst = new BinaryInst(LLVMType.InstType.ICMP_EQ, this.value, new LLVMConst(LLVMType.TypeID.IntegerTyID, 0));
        instructions.add(newInst);
        this.value = newInst;
        return this;
    }

    public void addUsableInstruction(LLVMInstruction inst) {
        assert inst instanceof UsableValue;
        instructions.add(inst);
        this.value = (UsableValue) inst;
    }

    public void addFromExp(LLVMExp exp1) {
        instructions.addAll(exp1.instructions);
    }

    public void logical() {
        BinaryInst newInst = new BinaryInst(LLVMType.InstType.ICMP_NE, this.value, new LLVMConst(LLVMType.TypeID.IntegerTyID, 0));
        instructions.add(newInst);
        this.value = newInst;
    }
}