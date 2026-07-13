package middleEnd.Insts;

import backEnd.Insts.BEQZInst;
import backEnd.Insts.BNEZInst;
import backEnd.Insts.JInst;
import backEnd.MIPSComment;
import backEnd.MIPSInst;
import backEnd.MIPSManager;
import backEnd.Register;
import middleEnd.LLVMInstruction;
import middleEnd.LLVMLabel;
import middleEnd.UsableValue;

import java.util.LinkedList;

import static middleEnd.LLVMType.InstType.BRANCH;

public class BranchInst extends LLVMInstruction {
    private UsableValue val = null;
    private LLVMLabel isTrue = null;
    private LLVMLabel isFalse = null;
    public LLVMLabel dest = null;

    public BranchInst(LLVMLabel dest) {
        super(BRANCH);
        this.dest = dest;
    }

    public BranchInst(UsableValue val, LLVMLabel isTrue, LLVMLabel isFalse) {
        super(BRANCH);
        this.isTrue = isTrue;
        this.isFalse = isFalse;
        this.val = val;
    }

    @Override
    public String toString() {
        if (val == null) {
            return "br label " + dest.toValueIR();
        } else {
            return "br i1 " + val.toValueIR() + ", label " + isTrue.toValueIR() + ", label " + isFalse.toValueIR();
        }
    }

    @Override
    public LinkedList<MIPSInst> toMIPS() {
        LinkedList<MIPSInst> mipsInsts = new LinkedList<>();
        mipsInsts.add(new MIPSComment(this.toString()));

        if (val == null) {
            mipsInsts.add(new JInst(MIPSManager.getInstance().getMIPSLabel(dest)));
        } else {
            Register reg;
            if (val.toValueIR().startsWith("%")) {
//                if (MIPSManager.getInstance().hasReg(val)) {
//                    reg = MIPSManager.getInstance().getReg(val);
//                } else {
//                    mipsInsts.addAll(MIPSManager.getInstance().deallocateReg());
                reg = MIPSManager.getInstance().getReg(val);
//                    mipsInsts.add(new LWInst(Register.SP, reg, MIPSManager.getInstance().getValueOffset(val)));
                mipsInsts.add(MIPSManager.getInstance().loadValueToReg(val, reg));
//                }
                mipsInsts.add(new BEQZInst(reg, MIPSManager.getInstance().getMIPSLabel(isFalse)));
                mipsInsts.add(new BNEZInst(reg, MIPSManager.getInstance().getMIPSLabel(isTrue)));
            } else {
                if (Integer.parseInt(val.toValueIR()) == 0) {
                    mipsInsts.add(new JInst(MIPSManager.getInstance().getMIPSLabel(isFalse)));
                } else {
                    mipsInsts.add(new JInst(MIPSManager.getInstance().getMIPSLabel(isTrue)));
                }
            }
        }
        MIPSManager.getInstance().releaseRegs();

        return mipsInsts;
    }
}
