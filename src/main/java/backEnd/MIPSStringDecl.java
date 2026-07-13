package backEnd;

import middleEnd.GlobalString;

public class MIPSStringDecl extends MIPSDecl {
    String name;
    String string;
    public MIPSStringDecl(GlobalString value) {
        super();
        name = "global_" + value.toValueIR().substring(1);
        string = value.string;
        modifyString();
    }

    private void modifyString() {
        string = string.replace("\\0A", "\\n");
        string = string.replace("\\00", "");
    }

    @Override
    public String toString() {
        return name + ": .asciiz \"" + string + "\"";
    }
}
