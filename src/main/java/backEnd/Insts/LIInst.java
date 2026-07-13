package backEnd.Insts;

import backEnd.MIPSInst;
import backEnd.Register;

public class LIInst extends MIPSInst {
    private final Register target;
    private final String val;
    public LIInst(Register target, String val) {
        super();
        this.target = target;
        this.val = val;
    }

    @Override
    public String toString() {
        return "li " + target + ", " + val;
    }
}
