package backEnd.Insts;

import backEnd.Register;

public class SGTInst extends MIPScmpInst {
    public SGTInst(Register reg, Register reg1, Register reg2) {
        super("sgt", reg, reg1, reg2);
    }
}
