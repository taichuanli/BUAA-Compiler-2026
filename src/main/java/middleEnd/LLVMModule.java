package middleEnd;

import java.util.LinkedList;

public class LLVMModule {
    // <CompUnit> ::= {<Decl>} {<FuncDef>} <MainFuncDef>
    public final LinkedList<Value> globalValues = new LinkedList<>();
    public final LinkedList<LLVMFunction> LLVMFunctions = new LinkedList<>();

    public void addGlobalDecls(LinkedList<Value> values) {
        globalValues.addAll(values);
    }

    public void addStrDecl(Value value) {
        globalValues.add(value);
    }

    public void addFunction(LLVMFunction globalValue) {
        LLVMFunctions.add(globalValue);
    }

    @Override
    public String toString() {
        StringBuilder module = new StringBuilder();
        for (Value value : globalValues) {
            module.append(value.toString()).append("\n");
        }
        for (LLVMFunction LLVMFunction : LLVMFunctions) {
            module.append(LLVMFunction.toString()).append("\n");
        }
        return module.toString();
    }
}
