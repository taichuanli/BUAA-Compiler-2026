package backEnd.Insts;

import backEnd.Register;

public class SGEInst extends MIPScmpInst {
    public SGEInst(Register reg, Register reg1, Register reg2) {
        super("sge", reg, reg1, reg2);
    }
}
