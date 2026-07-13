package backEnd.Insts;

import backEnd.MIPSInst;
import backEnd.Register;

public class ADDUInst extends MIPSInst {
    private final Register rs;
    private final Register rt;
    private final Register rd;
    public ADDUInst(Register rs, Register rt, Register rd) {
        super();
        this.rs = rs;
        this.rt = rt;
        this.rd = rd;
    }

    @Override
    public String toString() {
        return "addu " + rd + ", " + rs + ", " + rt;
    }
}
