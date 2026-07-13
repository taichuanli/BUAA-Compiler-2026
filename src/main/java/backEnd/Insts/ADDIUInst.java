package backEnd.Insts;

import backEnd.Register;

public class ADDIUInst extends backEnd.MIPSInst {
    private final Register rs;
    private final Register rt;
    private final int imm;
    public ADDIUInst(Register rs, Register rt, int imm) {
        super();
        this.rs = rs;
        this.rt = rt;
        this.imm = imm;
    }

    @Override
    public String toString() {
        return "addiu " + rt + ", " + rs + ", " + imm;
    }
}
