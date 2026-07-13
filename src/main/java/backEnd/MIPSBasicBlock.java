package backEnd;

import java.util.LinkedList;

public class MIPSBasicBlock {
    public String name;
    public LinkedList<MIPSInst> instructions = new LinkedList<>();

    public MIPSBasicBlock(String name) {
        this.name = name;
    }

    public void addInstructions(LinkedList<MIPSInst> instruction) {
        instructions.addAll(instruction);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(":\n");
        for (MIPSInst inst : instructions) {
            sb.append("\t").append(inst.toString()).append("\n");
        }
        return sb.toString();
    }
}
