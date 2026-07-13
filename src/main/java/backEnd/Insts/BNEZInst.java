package backEnd.Insts;

import backEnd.MIPSInst;
import backEnd.Register;

public class BNEZInst extends MIPSInst {
    private Register reg;
    private String s;
    public BNEZInst(Register reg, String s) {
        super();
        this.reg = reg;
        this.s = s;
    }

    @Override
    public String toString() {
        return "bnez " + reg + ", " + s;
    }
}
