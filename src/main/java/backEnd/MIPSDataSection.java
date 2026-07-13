package backEnd;

import middleEnd.GlobalString;
import middleEnd.GlobalVariable;

import java.util.LinkedList;

public class MIPSDataSection {
    private final LinkedList<MIPSDecl> decls = new LinkedList<>();

    public void addGlobalVariable(GlobalVariable value) {
        decls.add(new MIPSVarDecl(value));
    }

    public void addString(GlobalString value) {
        decls.add(new MIPSStringDecl(value));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(".data\n");
        for (MIPSDecl decl : decls) {
            sb.append('\t').append(decl).append('\n');
        }
        return sb.toString();
    }
}
