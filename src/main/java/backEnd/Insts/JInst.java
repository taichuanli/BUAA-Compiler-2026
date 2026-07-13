package backEnd.Insts;

import backEnd.MIPSInst;

public class JInst extends MIPSInst {
    private final String label;
    public JInst(String label) {
        super();
        this.label = label;
    }

    @Override
    public String toString() {
        return "j " + label;
    }
}
