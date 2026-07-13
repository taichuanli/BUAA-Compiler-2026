package middleEnd;

import frontEnd.Symbol;
import middleEnd.Insts.AllocaInst;
import middleEnd.Insts.GetelementptrInst;
import middleEnd.Insts.StoreInst;
import middleEnd.Insts.TruncInst;

import java.util.LinkedList;

public class LLVMVariable extends Value {
    public boolean isConst;
    public String name;
    public int arrayLength; // 为 0 是表示不是数组
    public LLVMType.TypeID baseType;
    public InitVal initVal;
    public UsableValue usableValue;

    public LLVMVariable(Symbol symbol, int arrayLength) {
        super();
        setFromSymbol(symbol);
        this.arrayLength = arrayLength;
    }

    public void setInitVal(InitVal initVal) {
        this.initVal = initVal;
    }

    protected void setFromSymbol(Symbol symbol) {
        this.isConst = symbol.symbolType.toString().startsWith("Const");
        this.name = symbol.token.token;
        switch (symbol.symbolType) {
            case ConstInt:
            case Int:
            case IntArray:
            case ConstIntArray:
                this.baseType = LLVMType.TypeID.IntegerTyID;
                break;
            case ConstChar:
            case Char:
            case CharArray:
            case ConstCharArray:
                this.baseType = LLVMType.TypeID.CharTyID;
                break;
            default:
                throw new RuntimeException("wrong symbol type for GlobalVariable: " + symbol.symbolType);
        }
    }

    public LinkedList<LLVMInstruction> getInstructions() {
        LinkedList<LLVMInstruction> instructions = new LinkedList<>();
        if (arrayLength == 0) {
            // 单个变量/常量
            if (isConst) {
                // 单个常量
                int initValAsInt = ((ConstInitVal) initVal).getConstValue(0);
                AllocaInst allocaInst = new AllocaInst(baseType, arrayLength);
                usableValue = allocaInst;
                instructions.add(allocaInst);
                LLVMConst llvmConst = new LLVMConst(baseType, initValAsInt);
                instructions.add(new StoreInst(llvmConst, allocaInst));
            } else {
                // 单个变量
                if (initVal != null) {
                    LLVMExp llvmExp = initVal.get(0);
                    if (llvmExp instanceof LLVMConst llvmConst) {
                        llvmExp = new LLVMExp(llvmConst);
                    }
                    if (baseType == LLVMType.TypeID.CharTyID) {
                        llvmExp.addUsableInstruction(new TruncInst(llvmExp.value, baseType));
                    }
                    LinkedList<LLVMInstruction> initValInsts = llvmExp.getInstructions();
                    instructions.addAll(initValInsts);
                    AllocaInst allocaInst = new AllocaInst(baseType, arrayLength);
                    usableValue = allocaInst;
                    instructions.add(allocaInst);
                    instructions.add(new StoreInst(llvmExp.value, allocaInst));
                } else {
                    AllocaInst allocaInst = new AllocaInst(baseType, arrayLength);
                    usableValue = allocaInst;
                    instructions.add(allocaInst);
                }
            }
        } else {
            // 数组
            if (isConst || initVal instanceof ConstInitVal) {
                // 常量数组
                ConstInitVal constInitVal = (ConstInitVal) initVal;
                AllocaInst allocaInst = new AllocaInst(baseType, arrayLength);
                usableValue = allocaInst;
                instructions.add(allocaInst);
                for (int i = 0; i < arrayLength; i++) {
                    GetelementptrInst getelementptrInst = new GetelementptrInst(baseType, allocaInst, new LLVMConst(LLVMType.TypeID.IntegerTyID, i));
                    instructions.add(getelementptrInst);
                    LLVMConst llvmConst = new LLVMConst(baseType, constInitVal.getConstValue(i));
                    instructions.add(new StoreInst(llvmConst, getelementptrInst));
                }
            } else {
                // 变量数组
                if (initVal != null) {
                    AllocaInst allocaInst = new AllocaInst(baseType, arrayLength);
                    usableValue = allocaInst;
                    instructions.add(allocaInst);
                    for (int i = 0; i < initVal.size(); i++) {
                        LLVMExp llvmExp = initVal.get(i);
                        if (llvmExp instanceof LLVMConst constExp) {
                            if (baseType == LLVMType.TypeID.CharTyID) {
                                constExp.constValue = constExp.constValue & 0xFF;
                                constExp.baseType = LLVMType.TypeID.CharTyID;
                            }
                            GetelementptrInst getelementptrInst = new GetelementptrInst(baseType, allocaInst, new LLVMConst(LLVMType.TypeID.IntegerTyID, i));
                            instructions.add(getelementptrInst);
                            instructions.add(new StoreInst(constExp, getelementptrInst));
                            continue;
                        }
                        if (baseType == LLVMType.TypeID.CharTyID) {
                            llvmExp.addUsableInstruction(new TruncInst(llvmExp.value, baseType));
                        }
                        LinkedList<LLVMInstruction> initValInsts = llvmExp.getInstructions();
                        instructions.addAll(initValInsts);
                        GetelementptrInst getelementptrInst = new GetelementptrInst(baseType, allocaInst, new LLVMConst(LLVMType.TypeID.IntegerTyID, i));
                        instructions.add(getelementptrInst);
                        instructions.add(new StoreInst(llvmExp.value, getelementptrInst));
                    }
                } else {
                    AllocaInst allocaInst = new AllocaInst(baseType, arrayLength);
                    usableValue = allocaInst;
                    instructions.add(allocaInst);
                }
            }
        }
        return instructions;
    }
}
