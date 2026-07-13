package backEnd.Insts;

import backEnd.Register;

public class SLEInst extends MIPScmpInst {
    public SLEInst(Register reg, Register reg1, Register reg2) {
        super("sle", reg, reg1, reg2);
    }
}
