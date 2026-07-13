package backEnd.Insts;

import backEnd.MIPSInst;
import backEnd.Register;

public class SWInst extends MIPSInst {
    private final Register base;
    private final Register rt;
    private final int offset;
    public SWInst(Register base, Register rt, int offset) {
        super();
        this.base = base;
        this.rt = rt;
        this.offset = offset;
    }

    @Override
    public String toString() {
        return "sw " + rt + ", " + offset + "(" + base + ")";
    }
}
