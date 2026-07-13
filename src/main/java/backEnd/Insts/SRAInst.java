package backEnd.Insts;

import backEnd.Register;

public class SRAInst extends backEnd.MIPSInst {
    private final Register rt;
    private final Register rd;
    private final int shamt;
    public SRAInst(Register rt, Register rd, int shamt) {
        super();
        this.rt = rt;
        this.rd = rd;
        this.shamt = shamt;
    }

    @Override
    public String toString() {
        return "sra " + rd + ", " + rt + ", " + shamt;
    }
}
