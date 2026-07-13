package backEnd.Insts;

import backEnd.Register;

public class SNEInst extends MIPScmpInst {
    public SNEInst(Register reg, Register reg1, Register reg2) {
        super("sne", reg, reg1, reg2);
    }
}
