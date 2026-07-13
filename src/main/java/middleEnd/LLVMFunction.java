package middleEnd;

import backEnd.MIPSManager;
import backEnd.Register;
import frontEnd.Symbol;
import middleEnd.Insts.BranchInst;
import middleEnd.Insts.RetInst;

import java.util.HashMap;
import java.util.LinkedList;

public class LLVMFunction extends Value {
    public LLVMType.TypeID retType;
    public final String name;
    public final LinkedList<FuncFParam> params = new LinkedList<>();
    private Block block;
    private int offset = 0;
    public LinkedList<LLVMBasicBlock> basicBlocks = new LinkedList<>();
    private final LinkedList<Register> freeArgRegs = new LinkedList<>();
    public HashMap<LLVMLabel, LLVMBasicBlock> labelToBlock = new HashMap<>();
    // private final LinkedList<Register> usedArgRegs = new LinkedList<>();

    public LLVMFunction(Symbol symbol) {
        super();
        initFreeArgRegs();
        switch (symbol.symbolType) {
            case IntFunc -> this.retType = LLVMType.TypeID.IntegerTyID;
            case CharFunc -> this.retType = LLVMType.TypeID.CharTyID;
            case VoidFunc -> this.retType = LLVMType.TypeID.VoidTyID;
        }
        this.name = symbol.token.token;
    }

    public LLVMFunction(String name, LLVMType.TypeID type) {
        super();
        initFreeArgRegs();
        this.retType = type;
        this.name = name;
    }

    private void initFreeArgRegs() {
        freeArgRegs.add(Register.A0);
        freeArgRegs.add(Register.A1);
        freeArgRegs.add(Register.A2);
        freeArgRegs.add(Register.A3);
    }

    public void setBlock(Block block) {
        this.block = block;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("define dso_local ");
        sb.append(retType.toString());
        sb.append(" @");
        sb.append(name);
        sb.append("(");
        if (!params.isEmpty()) {
            for (FuncFParam param : params) {
                sb.append(param.toString());
                sb.append(", ");
            }
            // remove the last comma and space
            sb.delete(sb.length() - 2, sb.length());
        }
        sb.append(")");
        sb.append(block.toString());
        sb.append("\n");
        return sb.toString();
    }

    public void setFParams(LinkedList<FuncFParam> params) {
        this.params.addAll(params);
    }

    public LLVMType.TypeID getReturnType() {
        return retType;
    }

    public void generateBasicBlocks() {
        // 从 Block 中生成 BasicBlock
        int blockNo = 0;
        LLVMBasicBlock bb = new LLVMBasicBlock(String.format("%s_%d", name, blockNo++));
        LLVMLabel label = new LLVMLabel();
        label.setVirtualRegNo(0);
        for (LLVMInstruction inst : block.getInstructions()) {
            if (inst instanceof LLVMLabel) {
                label = (LLVMLabel) inst;
                continue;
            }
            bb.addInst(inst);
            if (inst instanceof BranchInst || inst instanceof RetInst) {
                bb.setLLVMLabel(label);
                labelToBlock.put(label, bb);
                basicBlocks.add(bb);
                bb = new LLVMBasicBlock(String.format("%s_%d", name, blockNo++));
            }
        }
    }

    public void translateEntryBlock() {
        for (FuncFParam param : params) {
//            if (!freeArgRegs.isEmpty()) {
//                Register reg = freeArgRegs.removeFirst();
//                MIPSManager.getInstance().setRegMap(reg, param);
//            }
            MIPSManager.getInstance().allocateMemForArg();
            MIPSManager.getInstance().setParamOffset(param, offset);
            offset -= 4;
        }
    }
}
