package backEnd.Insts;

import backEnd.MIPSInst;

public class JALInst extends MIPSInst {
    private final String label;
    public JALInst(String label) {
        super();
        this.label = label;
    }

    @Override
    public String toString() {
        return "jal " + label;
    }
}
