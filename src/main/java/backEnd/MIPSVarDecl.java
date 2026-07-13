package backEnd;

import middleEnd.ConstInitVal;
import middleEnd.GlobalVariable;

public class MIPSVarDecl extends MIPSDecl {
    String name;
    String type;
    int length;
    ConstInitVal initVal;
    public MIPSVarDecl(GlobalVariable variable) {
        super();
        this.name = "global_" + variable.name;
//        if (variable.baseType == LLVMType.TypeID.IntegerTyID) {
        this.type = ".word";
//        } else {
//            this.type = ".byte";
//        }
        length = variable.arrayLength == 0 ? 1 : variable.arrayLength;
        initVal = (ConstInitVal) variable.initVal;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(": ");
        sb.append(type).append(" ");
        if (initVal.isAllZero && length > 1) {
            sb.append("0:");
            sb.append(length);
        } else {
            for (int i = 0; i < length; i++) {
                sb.append(initVal.getConstValue(i));
                if (i != length - 1) {
                    sb.append(", ");
                }
            }
        }
        return sb.toString();
    }
}
