package backEnd.Insts;

import backEnd.MIPSInst;
import backEnd.Register;

public class DIVInst extends MIPSInst {
    private final Register rs;
    private final Register rt;
    public DIVInst(Register rs, Register rt) {
        super();
        this.rs = rs;
        this.rt = rt;
    }

    @Override
    public String toString() {
        return "div " + rs + " " + rt;
    }
}
