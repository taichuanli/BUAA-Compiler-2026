package middleEnd.Insts;

import backEnd.Insts.*;
import backEnd.MIPSComment;
import backEnd.MIPSInst;
import backEnd.MIPSManager;
import backEnd.Register;
import middleEnd.LLVMInstruction;
import middleEnd.LLVMType;
import middleEnd.UsableValue;

import java.util.HashMap;
import java.util.LinkedList;

public class GetelementptrInst extends LLVMInstruction implements UsableValue {
    private int regNo;
    private final LLVMType.TypeID baseType;
    private final UsableValue from;
    private final UsableValue offset;
    public GetelementptrInst(LLVMType.TypeID baseType, UsableValue from, UsableValue offset) {
        super(LLVMType.InstType.GETELEMENTPTR);
        this.baseType = baseType.toPointerType();
        this.from = from;
        this.offset = offset;
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
        int tmpLen = from.toLLVMType().length() - 1;
        if (from.toLLVMType().charAt(0) == '[') {
            return String.format("%s = getelementptr %s, %s %s, i32 0, i32 %s", toValueIR(), from.toLLVMType().substring(0, tmpLen), from.toLLVMType(), from.toValueIR(), offset.toValueIR());
        } else {
            return String.format("%s = getelementptr %s, %s %s, i32 %s", toValueIR(), from.toLLVMType().substring(0, tmpLen), from.toLLVMType(), from.toValueIR(), offset.toValueIR());
        }
    }

    @Override
    public HashMap<UsableValue, Integer> getReferencedValues() {
        HashMap<UsableValue, Integer> map = new HashMap<>();
        map.put(from, 1);
        map.put(offset, 1);
        return map;
    }

    @Override
    public LinkedList<MIPSInst> toMIPS() {
        LinkedList<MIPSInst> mipsInsts = new LinkedList<>();
        mipsInsts.add(new MIPSComment(this.toString()));

        Register fromReg;
        if (from.toValueIR().startsWith("@")) {
            // 全局数组
            fromReg = Register.K0;
            mipsInsts.add(new LAInst(fromReg, "global_" + from.toValueIR().substring(1)));
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

        Register offsetReg;
        if (offset.toValueIR().startsWith("%")) {
//            if (MIPSManager.getInstance().hasReg(offset)) {
//                offsetReg = MIPSManager.getInstance().getReg(offset);
//            } else {
//                mipsInsts.addAll(MIPSManager.getInstance().deallocateReg());
                offsetReg = MIPSManager.getInstance().getReg(offset);
                mipsInsts.add(MIPSManager.getInstance().loadValueToReg(offset, offsetReg));
//                mipsInsts.add(new LWInst(Register.SP, offsetReg, MIPSManager.getInstance().getValueOffset(offset)));
//            }
//            MIPSManager.getInstance().reserveUsedReg(offsetReg);
        } else {
            offsetReg = Register.K1;
            mipsInsts.add(new LIInst(offsetReg, offset.toValueIR()));
        }

        mipsInsts.add(new SLLInst(offsetReg, offsetReg, 2));

        Register toReg;
//        if (!MIPSManager.getInstance().hasReg(this)) {
//            mipsInsts.addAll(MIPSManager.getInstance().deallocateReg());
//        }
        toReg = MIPSManager.getInstance().getReg(this);
        mipsInsts.add(new ADDUInst(fromReg, offsetReg, toReg));
        mipsInsts.add(MIPSManager.getInstance().saveRegToMemory(this, toReg));
        MIPSManager.getInstance().releaseRegs();

//        MIPSManager.getInstance().resetReservedRegs();

        return mipsInsts;
    }
}
