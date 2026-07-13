package backEnd.Insts;

import backEnd.Register;

public class SLTInst extends MIPScmpInst {
    public SLTInst(Register reg, Register reg1, Register reg2) {
        super("slt", reg, reg1, reg2);
    }
}
