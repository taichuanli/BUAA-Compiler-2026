package backEnd.Insts;

import backEnd.MIPSInst;
import backEnd.Register;

public class BEQZInst extends MIPSInst {
    private final Register reg;
    private final String s;
    public BEQZInst(Register reg, String s) {
        super();
        this.reg = reg;
        this.s = s;
    }

    @Override
    public String toString() {
        return "beqz " + reg + ", " + s;
    }
}
