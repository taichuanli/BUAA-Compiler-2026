package backEnd;

public class MIPSInst {
    protected static boolean fitsSigned16(int value) {
        return value >= Short.MIN_VALUE && value <= Short.MAX_VALUE;
    }

    protected static String addImmediate(Register source, Register target, int immediate) {
        if (fitsSigned16(immediate)) {
            return "addiu " + target + ", " + source + ", " + immediate;
        }
        return "li " + Register.AT + ", " + immediate + "\n\taddu "
                + target + ", " + source + ", " + Register.AT;
    }

    protected static String memoryAccess(String operation, Register base, Register target, int offset) {
        if (fitsSigned16(offset)) {
            return operation + " " + target + ", " + offset + "(" + base + ")";
        }
        return "li " + Register.AT + ", " + offset + "\n\taddu " + Register.AT + ", "
                + base + ", " + Register.AT + "\n\t" + operation + " " + target + ", 0(" + Register.AT + ")";
    }
}
