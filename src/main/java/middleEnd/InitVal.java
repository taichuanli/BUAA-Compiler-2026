package middleEnd;

import java.util.ArrayList;

public class InitVal extends Value {
    private final ArrayList<LLVMExp> exps = new ArrayList<>();
    // 供 ConstInitVal 使用
    public InitVal() {
        super();
    }

    public void addExp(LLVMExp llvmExp) {
        exps.add(llvmExp);
    }

    public LLVMExp get(int i) {
        return exps.get(i);
    }

    public int size() {
        return exps.size();
    }

    public void padToLength(int arrayLength, LLVMType.TypeID type) {
        while (exps.size() < arrayLength) {
            exps.add(new LLVMConst(type, 0));
        }
    }
}
