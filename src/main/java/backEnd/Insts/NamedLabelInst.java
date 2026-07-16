package backEnd.Insts;

import backEnd.MIPSInst;

public class NamedLabelInst extends MIPSInst {
    private final String label;

    public NamedLabelInst(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label + ":";
    }
}
