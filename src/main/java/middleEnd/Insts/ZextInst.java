package middleEnd.Insts;

import backEnd.Insts.ADDIUInst;
import backEnd.Insts.LIInst;
import backEnd.MIPSComment;
import backEnd.MIPSInst;
import backEnd.MIPSManager;
import backEnd.Register;
import middleEnd.LLVMInstruction;
import middleEnd.LLVMType;
import middleEnd.UsableValue;

import java.util.HashMap;
import java.util.LinkedList;

public class ZextInst extends LLVMInstruction implements UsableValue {
    private int regNo;
    private final LLVMType.TypeID baseType;
    private final UsableValue from;

    public ZextInst(UsableValue from, LLVMType.TypeID baseType) {
        super(LLVMType.InstType.ZEXT);
        this.baseType = baseType;
        this.from = from;
    }

    @Override
    public String toValueIR() {
        return String.format("%%%d", regNo);
    }

    @Override
    public String toLLVMType() {
        return baseType.toString();
    }

    @Override
    public int toAlign() {
        return baseType.toAlign();
    }

    @Override
    public void setVirtualRegNo(int regNo) {
        this.regNo = regNo;
    }

    @Override
    public int getMemorySize() {
        return 4;
    }

    @Override
    public String toString() {
        return String.format("%s = zext %s %s to %s", toValueIR(), from.toLLVMType(), from.toValueIR(), toLLVMType());
    }

    @Override
    public HashMap<UsableValue, Integer> getReferencedValues() {
        HashMap<UsableValue, Integer> map = new HashMap<>();
        map.put(from, 1);
        return map;
    }

    @Override
    public LinkedList<MIPSInst> toMIPS() {
        LinkedList<MIPSInst> mipsInsts = new LinkedList<>();
        mipsInsts.add(new MIPSComment(this.toString()));

        Register fromReg;
        if (from.toValueIR().startsWith("%")) {
//            if (!MIPSManager.getInstance().hasReg(from)) {
//                mipsInsts.addAll(MIPSManager.getInstance().deallocateReg());
//            }
            fromReg = MIPSManager.getInstance().getReg(from);
            mipsInsts.add(MIPSManager.getInstance().loadValueToReg(from, fromReg));
//            MIPSManager.getInstance().reserveUsedReg(fromReg);
        } else {
            fromReg = Register.K0;
            mipsInsts.add(new LIInst(fromReg, from.toValueIR()));
        }

        Register reg;
//        mipsInsts.addAll(MIPSManager.getInstance().deallocateReg());
        reg = MIPSManager.getInstance().getReg(this);
        mipsInsts.add(new ADDIUInst(fromReg, reg, 0));
        mipsInsts.add(MIPSManager.getInstance().saveRegToMemory(this, reg));
        MIPSManager.getInstance().releaseRegs();

        return mipsInsts;
    }
}
