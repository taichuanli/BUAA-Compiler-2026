package backEnd.Insts;

import backEnd.MIPSInst;
import backEnd.Register;

public class MIPScmpInst extends MIPSInst {
    private final String type;
    private final Register reg;
    private final Register reg1;
    private final Register reg2;
    protected MIPScmpInst(String type, Register reg, Register reg1, Register reg2) {
        super();
        this.type = type;
        this.reg = reg;
        this.reg1 = reg1;
        this.reg2 = reg2;
    }

    public String toString() {
        return type + " " + reg + ", " + reg1 + ", " + reg2;
    }
}
