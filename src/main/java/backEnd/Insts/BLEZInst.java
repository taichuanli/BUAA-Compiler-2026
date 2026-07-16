package backEnd.Insts;

import backEnd.MIPSInst;
import backEnd.Register;

public class BLEZInst extends MIPSInst {
    private final Register reg;
    private final String label;

    public BLEZInst(Register reg, String label) {
        this.reg = reg;
        this.label = label;
    }

    @Override
    public String toString() {
        return "blez " + reg + ", " + label;
    }
}
