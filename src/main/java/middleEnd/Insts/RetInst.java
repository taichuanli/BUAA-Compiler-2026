package middleEnd.Insts;

import backEnd.Insts.ADDIUInst;
import backEnd.Insts.JRRAInst;
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

public class RetInst extends LLVMInstruction {
    UsableValue ret = null;
    public RetInst() {
        super(LLVMType.InstType.RETURN);
    }

    public RetInst(UsableValue value) {
        super(LLVMType.InstType.RETURN);
        ret = value;
    }

    @Override
    public String toString() {
        if (ret == null) {
            return "ret void";
        } else {
            return String.format("ret %s %s", ret.toLLVMType(), ret.toValueIR());
        }
    }

    @Override
    public HashMap<UsableValue, Integer> getReferencedValues() {
        HashMap<UsableValue, Integer> map = new HashMap<>();
        if (ret != null) {
            map.put(ret, 1);
        }
        return map;
    }

    @Override
    public LinkedList<MIPSInst> toMIPS() {
        LinkedList<MIPSInst> mipsInsts = new LinkedList<>();
        mipsInsts.add(new MIPSComment(this.toString()));

        if (ret != null) {
            if (ret.toValueIR().startsWith("%")) {
//                if (!MIPSManager.getInstance().hasReg(ret)) {
//                    mipsInsts.addAll(MIPSManager.getInstance().deallocateReg());
//                }
                Register retReg = MIPSManager.getInstance().getReg(ret);
                mipsInsts.add(MIPSManager.getInstance().loadValueToReg(ret, retReg));
                mipsInsts.add(new ADDIUInst(retReg, Register.V0, 0));
            } else {
                mipsInsts.add(new LIInst(Register.V0, ret.toValueIR()));
            }
        }
        mipsInsts.add(new JRRAInst());
        MIPSManager.getInstance().releaseRegs();

        return mipsInsts;
    }
}
