package backEnd.Insts;

import backEnd.MIPSInst;
import backEnd.Register;

public class LBUInst extends MIPSInst {
    private final Register base;
    private final Register rt;
    private final int offset;

    public LBUInst(Register base, Register rt, int offset) {
        this.base = base;
        this.rt = rt;
        this.offset = offset;
    }

    @Override
    public String toString() {
        return "lbu " + rt + ", " + offset + "(" + base + ")";
    }
}
