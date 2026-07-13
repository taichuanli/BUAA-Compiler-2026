package middleEnd.Insts;
import backEnd.Insts.LAInst;
import backEnd.Insts.LWInst;
import backEnd.MIPSComment;
import backEnd.MIPSInst;
import backEnd.MIPSManager;
import backEnd.Register;
import middleEnd.LLVMInstruction;
import middleEnd.LLVMType;
import middleEnd.UsableValue;

import java.util.HashMap;
import java.util.LinkedList;

public class LoadInst extends LLVMInstruction implements UsableValue {
    int regNo;
    LLVMType.TypeID baseType;
    UsableValue from;
    public LoadInst(LLVMType.TypeID baseType, UsableValue from) {
        super(LLVMType.InstType.LOAD);
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
        return String.format("%s = load %s, %s* %s", toValueIR(), toLLVMType(), toLLVMType(), from.toValueIR());
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
        if (from.toValueIR().startsWith("@")) {
            // 全局单个变量，加载地址
            mipsInsts.add(new LAInst(Register.K0, "global_" + from.toValueIR().substring(1)));
            fromReg = Register.K0;
        } else {
            // 为虚拟寄存器
//            if (MIPSManager.getInstance().hasReg(from)) {
//                // 使用已有的物理寄存器
//                fromReg = MIPSManager.getInstance().getReg(from);
//            } else {
                // 分配一个物理寄存器，从内存中加载值
//                mipsInsts.addAll(MIPSManager.getInstance().deallocateReg());
                fromReg = MIPSManager.getInstance().getReg(from);
                mipsInsts.add(MIPSManager.getInstance().loadValueToReg(from, fromReg));
//                mipsInsts.add(new LWInst(Register.SP, fromReg, MIPSManager.getInstance().getValueOffset(from)));
//            }
//            MIPSManager.getInstance().reserveUsedReg(fromReg);
        }

//        mipsInsts.addAll(MIPSManager.getInstance().deallocateReg());
        Register toReg = MIPSManager.getInstance().getReg(this);
        mipsInsts.add(new LWInst(fromReg, toReg, 0));
        mipsInsts.add(MIPSManager.getInstance().saveRegToMemory(this, toReg));

        MIPSManager.getInstance().releaseRegs();

//        MIPSManager.getInstance().resetReservedRegs();

        return mipsInsts;
    }
}
