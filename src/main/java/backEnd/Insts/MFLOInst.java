package backEnd.Insts;

import backEnd.MIPSInst;
import backEnd.Register;

public class MFLOInst extends MIPSInst {
    private final Register rd;
    public MFLOInst(Register rd) {
        super();
        this.rd = rd;
    }

    @Override
    public String toString() {
        return "mflo " + rd.toString();
    }
}
