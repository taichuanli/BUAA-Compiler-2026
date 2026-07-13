package backEnd.Insts;

import backEnd.Register;

public class SLLInst extends backEnd.MIPSInst {
    private final Register rt;
    private final Register rd;
    private final int shamt;
    public SLLInst(Register rt, Register rd, int shamt) {
        super();
        this.rt = rt;
        this.rd = rd;
        this.shamt = shamt;
    }

    @Override
    public String toString() {
        return "sll " + rd + ", " + rt + ", " + shamt;
    }
}
