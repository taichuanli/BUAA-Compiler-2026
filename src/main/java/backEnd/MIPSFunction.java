package backEnd;

import java.util.LinkedList;

public class MIPSFunction {
    private final String name;
    private final LinkedList<MIPSBasicBlock> basicBlocks = new LinkedList<>();
    public MIPSFunction(String name) {
        this.name = "func_" + name;
    }

    public void addBasicBlock(int index, MIPSBasicBlock mipsBasicBlock) {
        basicBlocks.add(index, mipsBasicBlock);
    }

    public void addBasicBlock(MIPSBasicBlock mipsBasicBlock) {
        basicBlocks.add(mipsBasicBlock);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(":\n");
        for (MIPSBasicBlock mipsBasicBlock : basicBlocks) {
            sb.append(mipsBasicBlock.toString());
        }
        return sb.toString();
    }
}
