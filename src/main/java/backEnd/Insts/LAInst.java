package backEnd.Insts;

import backEnd.MIPSInst;
import backEnd.Register;

public class LAInst extends MIPSInst {
    private final Register target;
    private final String val;
    public LAInst(Register target, String val) {
        super();
        this.target = target;
        this.val = val;
    }

    @Override
    public String toString() {
        return "la " + target + ", " + val;
    }
}
