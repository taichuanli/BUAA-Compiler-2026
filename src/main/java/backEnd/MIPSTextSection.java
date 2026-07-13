package backEnd;

import java.util.LinkedList;

public class MIPSTextSection {
    private final LinkedList<MIPSFunction> functions = new LinkedList<>();

    public void addFunction(MIPSFunction mipsFunction) {
        functions.add(mipsFunction);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(".text\n");
        sb.append("jal func_main\n");
        sb.append("j end\n");
        for (MIPSFunction function : functions) {
            sb.append(function.toString());
        }
        sb.append("end:\n");
        return sb.toString();
    }
}
