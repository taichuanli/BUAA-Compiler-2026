package backEnd.Insts;

import backEnd.MIPSInst;
import backEnd.Register;

public class SBInst extends MIPSInst {
    private final Register base;
    private final Register rt;
    private final int offset;

    public SBInst(Register base, Register rt, int offset) {
        this.base = base;
        this.rt = rt;
        this.offset = offset;
    }

    @Override
    public String toString() {
        return memoryAccess("sb", base, rt, offset);
    }
}
