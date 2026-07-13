package backEnd;

import backEnd.Insts.LWInst;
import backEnd.Insts.SWInst;
import middleEnd.*;

import java.util.*;

public class MIPSManager {
    private static final MIPSManager MIPS_MANAGER = new MIPSManager();
    private int offset = 0;
    private LLVMFunction currentFunction;
    private final HashMap<LLVMFunction, HashMap<UsableValue, Integer>> offsetMap = new HashMap<>();
    private final LinkedList<Register> tempRegs = new LinkedList<>();
    private final LinkedList<Register> globalRegs = new LinkedList<>();
    private LinkedList<Register> used = new LinkedList<>();
    private LinkedList<Register> free = new LinkedList<>();
//    private LinkedList<Register> usedArgs = new LinkedList<>();
//    private LinkedList<Register> freeArgs = new LinkedList<>();
//    private HashMap<Register, UsableValue> regMap = new HashMap<>();
//    private HashSet<Register> reserved = new HashSet<>();
//    private UsableValue forSP = new LLVMLabel();
    private int regIndex = 0;
    private final UsableValue forRA = new LLVMLabel();
    private final HashMap<UsableValue, Register> globalRegMap = new HashMap<>();
    private MIPSManager() {
        tempRegs.add(Register.T0);
        tempRegs.add(Register.T1);
        tempRegs.add(Register.T2);
        
        globalRegs.add(Register.T3);
        globalRegs.add(Register.T4);
        globalRegs.add(Register.T5);
        globalRegs.add(Register.T6);
        globalRegs.add(Register.T7);
        globalRegs.add(Register.T8);
        globalRegs.add(Register.T9);
        globalRegs.add(Register.S0);
        globalRegs.add(Register.S1);
        globalRegs.add(Register.S2);
        globalRegs.add(Register.S3);
        globalRegs.add(Register.S4);
        globalRegs.add(Register.S5);
        globalRegs.add(Register.S6);
    }

    public static MIPSManager getInstance() {
        return MIPS_MANAGER;
    }

    public int getOffset() {
        return offset;
    }

//    public int getValueOffset(UsableValue value) {
//        return offsetMap.get(currentFunction).get(value);
//    }

    public void subOffset(int size) {
        offset -= size;
    }

    public void setCurrentFunction(LLVMFunction function) {
        currentFunction = function;
        offsetMap.putIfAbsent(function, new HashMap<>());
        free = new LinkedList<>(tempRegs);
        used = new LinkedList<>();
//        regMap = new HashMap<>();
        offset = 0;
    }

    public int allocateMemForAlloca(int size) {
        // int offset = this.offset;
        subOffset(size);
        return offset;
//        HashMap<UsableValue, Integer> map = offsetMap.get(currentFunction);
//        int size = value.getMemorySize();
//        // 申请内存
//        if (!map.containsKey(value) && size != 0) {
//            map.put(value, offset);
//            subOffset(size);
//        }
    }

//    public LinkedList<MIPSInst> deallocateReg() {
//        // 如果没有空闲寄存器，就踢出一个占用寄存器，并将其值保存到占用该寄存器对应临时寄存器的内存对应位置
//        LinkedList<MIPSInst> insts = new LinkedList<>();
//        if (free.isEmpty()) {
//            Register reg = nextUsedReg();
//            used.remove(reg);
//            free.add(reg);
//            UsableValue value = regMap.get(reg);
//            insts.add(new MIPSComment("saving " + value.toValueIR() + " to memory, reg: " + reg));
//            if (offsetMap.get(currentFunction).containsKey(value)) {
//                int offset = offsetMap.get(currentFunction).get(value);
//                insts.add(new SWInst(Register.SP, reg, offset));
//            } else {
//                HashMap<UsableValue, Integer> map = offsetMap.get(currentFunction);
//                int size = value.getMemorySize();
//                // 申请内存
//                map.put(value, offset);
//                insts.add(new SWInst(Register.SP, reg, offset));
//                subOffset(size);
//            }
//        }
//        return insts;
//    }

//    public boolean hasReg(UsableValue value) {
//        return regMap.containsValue(value);
//    }

    public Register getReg(UsableValue value) {
//        for (HashMap.Entry<Register, UsableValue> entry : regMap.entrySet()) {
//            if (entry.getValue().equals(value)) {
//                return entry.getKey();
//            }
//        }
        //        regMap.put(reg, value);
//        free.remove(reg);
//        used.add(reg);
        if (globalRegMap.containsKey(value)) {
            return globalRegMap.get(value);
        }
        return nextFreeReg();
    }

    public MIPSInst saveRegToMemory(UsableValue value, Register reg) {
        MIPSInst inst;
        if (offsetMap.get(currentFunction).containsKey(value)) {
            int offset = offsetMap.get(currentFunction).get(value);
            inst = new SWInst(Register.SP, reg, offset);
        } else {
            HashMap<UsableValue, Integer> map = offsetMap.get(currentFunction);
            int size = value.getMemorySize();
            // 申请内存
            map.put(value, offset);
            inst = new SWInst(Register.SP, reg, offset);
            subOffset(size);
        }
        if (globalRegMap.containsKey(value)) {
            return new MIPSComment(String.format("value %s is in globalRegMap", value.toValueIR()));
        }
        return inst;
    }

    public MIPSInst loadValueToReg(UsableValue value, Register reg) {
//        LinkedList<MIPSInst> insts = new LinkedList<>();
        if (globalRegMap.containsKey(value)) {
            return new MIPSComment(String.format("value %s is in globalRegMap", value.toValueIR()));
        }
        int offset = offsetMap.get(currentFunction).get(value);
        return new LWInst(Register.SP, reg, offset);
    }

    private Register nextFreeReg() {
        Register ret = free.get(regIndex);
        regIndex = (regIndex + 1) % free.size();
        return ret;
    }

//    private Register nextUsedReg() {
//        for (Register reg : used) {
//            if (!reserved.contains(reg)) {
//                return reg;
//            }
//        }
//        throw new RuntimeException("No free register");
//    }

//    public void reserveUsedReg(Register fromReg) {
//        reserved.add(fromReg);
//    }

//    public void resetReservedRegs() {
//        reserved.clear();
//    }

//    public boolean hasFreeArgReg() {
//        return !this.freeArgs.isEmpty();
//    }

//    public void setRegMap(Register reg, FuncFParam param) {
//        regMap.put(reg, param);
//    }

    public void setParamOffset(FuncFParam param, int offset) {
        offsetMap.get(currentFunction).put(param, offset);
    }

    public void allocateMemForArg() {
        offset -= 4;
    }

    public LinkedList<MIPSInst> storeAllReg() {
        LinkedList<MIPSInst> insts = new LinkedList<>();
//        for (Register reg : used) {
//            UsableValue value = regMap.get(reg);
//            if (offsetMap.get(currentFunction).containsKey(value)) {
//                int offset = offsetMap.get(currentFunction).get(value);
//                insts.add(new SWInst(Register.SP, reg, offset));
//            } else {
//                HashMap<UsableValue, Integer> map = offsetMap.get(currentFunction);
//                int size = value.getMemorySize();
//                // 申请内存
//                map.put(value, offset);
//                insts.add(new SWInst(Register.SP, reg, offset));
//                subOffset(size);
//            }
//        }
        HashMap<UsableValue, Integer> map = offsetMap.get(currentFunction);
//        map.put(MIPS_MANAGER.forSP, offset);
//        insts.add(new SWInst(Register.SP, Register.SP, offset));
//        subOffset(4);
        map.put(MIPS_MANAGER.forRA, offset);
        insts.add(new SWInst(Register.SP, Register.RA, offset));
        subOffset(4);
        return insts;
    }

    public LinkedList<MIPSInst> restoreAllReg() {
        LinkedList<MIPSInst> insts = new LinkedList<>();
//        for (Register reg : used) {
//            UsableValue value = regMap.get(reg);
//            int offset = offsetMap.get(currentFunction).get(value);
//            insts.add(new LWInst(Register.SP, reg, offset));
//        }
        HashMap<UsableValue, Integer> map = offsetMap.get(currentFunction);
//        insts.add(new LWInst(Register.SP, Register.SP, map.get(MIPS_MANAGER.forSP)));
        insts.add(new LWInst(Register.SP, Register.RA, map.get(MIPS_MANAGER.forRA)));
        return insts;
    }

    public String getMIPSLabel(LLVMLabel dest) {
        return currentFunction.labelToBlock.get(dest).name;
    }

    public void releaseRegs() {
//        free.addAll(used);
//        used.clear();
    }

    public void setReference(LLVMFunction mainFunc) {
        if (mainFunc == null) {
            return; // 怎么可能
        }

        HashMap<UsableValue, Integer> map = new HashMap<>(); // 记录引用次数

        for (LLVMBasicBlock block : mainFunc.basicBlocks) {
            for (LLVMInstruction inst : block.instructions) {
                if (inst instanceof LLVMLabel) {
                    continue;
                }
                if (inst instanceof UsableValue value) {
                    map.put(value, 0);
                }
                HashMap<UsableValue, Integer> temp = inst.getReferencedValues();
                for (UsableValue value : temp.keySet()) {
                    if (value instanceof LLVMInstruction) {
                        map.put(value, map.getOrDefault(value, 0) + temp.get(value));
                    }
                }
            }
        }

        int k = 14;
        List<Map.Entry<UsableValue, Integer>> topK = new ArrayList<>(map.entrySet());
        topK.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        topK = topK.subList(0, Math.min(k, topK.size()));
        for (Map.Entry<UsableValue, Integer> entry : topK) {
            UsableValue value = entry.getKey();
            globalRegMap.put(value, globalRegs.remove());
        }
    }
}