package backEnd.Insts;

import backEnd.MIPSInst;
import backEnd.Register;

public class MULTInst extends MIPSInst {
    private final Register rs;
    private final Register rt;
    public MULTInst(Register rs, Register rt) {
        super();
        this.rs = rs;
        this.rt = rt;
    }

    @Override
    public String toString() {
        return "mult " + rs + ", " + rt;
    }
}
