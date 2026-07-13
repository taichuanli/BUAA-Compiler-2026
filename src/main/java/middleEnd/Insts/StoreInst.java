package middleEnd.Insts;

import backEnd.Insts.LAInst;
import backEnd.Insts.LIInst;
import backEnd.Insts.SWInst;
import backEnd.MIPSComment;
import backEnd.MIPSInst;
import backEnd.MIPSManager;
import backEnd.Register;
import middleEnd.LLVMInstruction;
import middleEnd.LLVMType;
import middleEnd.UsableValue;

import java.util.HashMap;
import java.util.LinkedList;

public class StoreInst extends LLVMInstruction {

    private final UsableValue from;
    private final UsableValue to;

    public StoreInst(UsableValue from, UsableValue to) {
        super(LLVMType.InstType.STORE);
        this.from = from;
        this.to = to;
    }

    @Override
    public String toString() {
        return String.format("store %s %s, %s %s", from.toLLVMType(), from.toValueIR(), to.toLLVMType(), to.toValueIR());
    }

    @Override
    public HashMap<UsableValue, Integer> getReferencedValues() {
        HashMap<UsableValue, Integer> map = new HashMap<>();
        map.put(from, 1);
        map.put(to, 1);
        return map;
    }

    @Override
    public LinkedList<MIPSInst> toMIPS() {
        LinkedList<MIPSInst> mipsInsts = new LinkedList<>();
        mipsInsts.add(new MIPSComment(this.toString()));

        Register fromReg;
        if (from.toValueIR().startsWith("%")) {
//            if (MIPSManager.getInstance().hasReg(from)) {
//                // 使用已有的物理寄存器
//                fromReg = MIPSManager.getInstance().getReg(from);
//            } else {
                // 为虚拟寄存器，分配一个物理寄存器
//                mipsInsts.addAll(MIPSManager.getInstance().deallocateReg());
                fromReg = MIPSManager.getInstance().getReg(from);
                mipsInsts.add(MIPSManager.getInstance().loadValueToReg(from, fromReg));
//                mipsInsts.add(new LWInst(Register.SP, fromReg, MIPSManager.getInstance().getValueOffset(from)));
//            }
//            MIPSManager.getInstance().reserveUsedReg(fromReg);
        } else {
            // 为常量使用寄存器 K0 存储
            mipsInsts.add(new LIInst(Register.K0, from.toValueIR()));
            fromReg = Register.K0;
        }

        Register toReg;
        if (to.toValueIR().startsWith("@")) {
            // 全局单个变量，加载地址
            mipsInsts.add(new LAInst(Register.K1, "global_" + to.toValueIR().substring(1)));
            toReg = Register.K1;
        } else {
            // 为虚拟寄存器
//            if (MIPSManager.getInstance().hasReg(to)) {
//                // 使用已有的物理寄存器
//                toReg = MIPSManager.getInstance().getReg(to);
//            } else {
                // 分配一个物理寄存器，从内存中加载值
//                mipsInsts.addAll(MIPSManager.getInstance().deallocateReg());
                toReg = MIPSManager.getInstance().getReg(to);
                mipsInsts.add(MIPSManager.getInstance().loadValueToReg(to, toReg));
//                mipsInsts.add(new LWInst(Register.SP, toReg, MIPSManager.getInstance().getValueOffset(to)));
//            }
        }


        mipsInsts.add(new SWInst(toReg, fromReg, 0));
        MIPSManager.getInstance().releaseRegs();

//        MIPSManager.getInstance().resetReservedRegs();

        return mipsInsts;
    }
}
