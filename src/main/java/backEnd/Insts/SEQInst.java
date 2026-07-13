package backEnd.Insts;

import backEnd.Register;

public class SEQInst extends MIPScmpInst {
    public SEQInst(Register reg, Register reg1, Register reg2) {
        super("seq", reg, reg1, reg2);
    }
}