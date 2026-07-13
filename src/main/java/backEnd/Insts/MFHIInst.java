package backEnd.Insts;

import backEnd.MIPSInst;
import backEnd.Register;

public class MFHIInst extends MIPSInst {
    private final Register rd;
    public MFHIInst(Register rd) {
        super();
        this.rd = rd;
    }

    @Override
    public String toString() {
        return "mfhi " + rd.toString();
    }
}
