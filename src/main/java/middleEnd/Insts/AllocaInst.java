package middleEnd.Insts;

import backEnd.Insts.ADDIUInst;
import backEnd.MIPSComment;
import backEnd.MIPSInst;
import backEnd.MIPSManager;
import backEnd.Register;
import middleEnd.LLVMInstruction;
import middleEnd.LLVMType;
import middleEnd.UsableValue;

import java.util.LinkedList;

public class AllocaInst extends LLVMInstruction implements UsableValue {
    private int regNo;
    private final LLVMType.TypeID baseType;
    private final int arrayLength;
    public AllocaInst(LLVMType.TypeID baseType, int arrayLength) {
        super(LLVMType.InstType.ALLOCA);
        this.baseType = baseType;
        this.arrayLength = arrayLength;
    }

    public void setVirtualRegNo(int regNo) {
        this.regNo = regNo;
    }

    @Override
    public String toValueIR() {
        return String.format("%%%d", regNo);
    }

    @Override
    public String toLLVMType() {
        if (arrayLength == 0) {
            return baseType.toPointerType().toString();
        } else {
            return String.format("[%d x %s]*", arrayLength, baseType.toString());
        }
    }

    @Override
    public int toAlign() {
        return baseType.toAlign();
    }

    @Override
    public int getMemorySize() {
        return 4;
//        if (arrayLength == 0) {
//            return baseType.toAlign();
//        }
//        return baseType.toAlign() * arrayLength;
    }

    private String toNoPointerType() {
        if (arrayLength == 0) {
            return baseType.toString();
        } else {
            return String.format("[%d x %s]", arrayLength, baseType.toString());
        }
    }

    @Override
    public String toString() {
        return String.format("%s = alloca %s, align %s", toValueIR(), toNoPointerType(), baseType.toAlign());
    }

    @Override
    public LinkedList<MIPSInst> toMIPS() {
        LinkedList<MIPSInst> mipsInsts = new LinkedList<>();
        mipsInsts.add(new MIPSComment(this.toString()));
        // 给值申请内存
        int size;
        if (arrayLength == 0) {
            size = 4;
        } else {
            size = 4 * arrayLength;
        }
        int offset = MIPSManager.getInstance().allocateMemForAlloca(size);
        // 给指针申请寄存器
        // mipsInsts.addAll(MIPSManager.getInstance().deallocateReg());
        Register reg = MIPSManager.getInstance().getReg(this);
        mipsInsts.add(new ADDIUInst(Register.SP, reg, offset + 4));
        mipsInsts.add(MIPSManager.getInstance().saveRegToMemory(this, reg));
        MIPSManager.getInstance().releaseRegs();
        return mipsInsts;
    }
}
