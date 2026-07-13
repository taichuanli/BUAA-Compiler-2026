package backEnd.Insts;

import backEnd.Register;

public class ANDIInst extends backEnd.MIPSInst {
    private final Register rs;
    private final Register rt;
    private final int imm;
    public ANDIInst(Register rs, Register rt, int imm) {
        super();
        this.rs = rs;
        this.rt = rt;
        this.imm = imm;
    }

    @Override
    public String toString() {
        return "andi " + rt + ", " + rs + ", " + imm;
    }
}
