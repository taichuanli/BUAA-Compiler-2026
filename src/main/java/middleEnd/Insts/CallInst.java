package middleEnd.Insts;

import backEnd.Insts.*;
import backEnd.MIPSComment;
import backEnd.MIPSInst;
import backEnd.MIPSManager;
import backEnd.Register;
import middleEnd.LLVMInstruction;
import middleEnd.LLVMType;
import middleEnd.UsableValue;

import java.util.HashMap;
import java.util.LinkedList;

public class CallInst extends LLVMInstruction implements UsableValue {
    private final LLVMType.TypeID retType;
    private final String funcName;
    private final LinkedList<UsableValue> params;
    private int regNo;

    public CallInst(LLVMType.TypeID retType, String func, LinkedList<UsableValue> params) {
        super(LLVMType.InstType.CALL);
        this.retType = retType;
        this.funcName = func;
        this.params = params;
    }

    public CallInst(LLVMType.TypeID retType, String func) {
        super(LLVMType.InstType.CALL);
        this.retType = retType;
        this.funcName = func;
        this.params = new LinkedList<>();
    }

    public CallInst(String funcName, LinkedList<UsableValue> params) {
        super(LLVMType.InstType.CALL);
        this.retType = LLVMType.TypeID.VoidTyID;
        this.funcName = funcName;
        this.params = params;
    }

    @Override
    public String toValueIR() {
        return String.format("%%%d", regNo);
    }

    @Override
    public String toLLVMType() {
        return retType.toString();
    }

    @Override
    public int toAlign() {
        return retType.toAlign();
    }

    @Override
    public void setVirtualRegNo(int regNo) {
        this.regNo = regNo;
    }

    @Override
    public int getMemorySize() {
        return 4;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.retType != LLVMType.TypeID.VoidTyID) {
            sb.append(String.format("%%%d = ", regNo));
        }
        sb.append(String.format("call %s @%s", toLLVMType(), funcName));
        sb.append("(");
        for (int i = 0; i < params.size(); i++) {
            UsableValue param = params.get(i);
            sb.append(param.toLLVMType());
            sb.append(" ");
            sb.append(param.toValueIR());
            if (i != params.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public HashMap<UsableValue, Integer> getReferencedValues() {
        HashMap<UsableValue, Integer> map = new HashMap<>();
        for (UsableValue param : params) {
            if (map.containsKey(param)) {
                map.put(param, map.get(param) + 1);
            } else {
                map.put(param, 1);
            }
        }
        return map;
    }

    public boolean isDifferentType(UsableValue val) {
        if (retType != LLVMType.TypeID.CharTyID && retType != LLVMType.TypeID.IntegerTyID) {
            return false;
        }

        return !val.toLLVMType().equals(retType.toPointerType().toString());
    }

    public LLVMInstruction fix() {
        if (retType == LLVMType.TypeID.CharTyID) {
            return new ZextInst(this, LLVMType.TypeID.IntegerTyID);
        } else {
            return new TruncInst(this, LLVMType.TypeID.CharTyID);
        }
    }

    public boolean isVoid() {
        return this.retType == LLVMType.TypeID.VoidTyID;
    }

    private static Register getReg(int i) {
        return switch (i) {
            case 0 -> Register.A0;
            case 1 -> Register.A1;
            case 2 -> Register.A2;
            case 3 -> Register.A3;
            default -> null;
        };
    }

    private boolean isOutput() {
        return this.funcName.equals("putstr") || this.funcName.equals("putch") || this.funcName.equals("putint");
    }

    private boolean isInput() {
        return this.funcName.equals("getchar") || this.funcName.equals("getint");
    }

    @Override
    public LinkedList<MIPSInst> toMIPS() {
        LinkedList<MIPSInst> mipsInsts = new LinkedList<>();
        mipsInsts.add(new MIPSComment(this.toString()));

        if (isOutput()) {
            UsableValue param = params.get(0);
            Register fromReg;
            if (param.toValueIR().startsWith("%")) {
//                if (MIPSManager.getInstance().hasReg(param)) {
//                    fromReg = MIPSManager.getInstance().getReg(param);
//                } else {
//                    mipsInsts.addAll(MIPSManager.getInstance().deallocateReg());
                fromReg = MIPSManager.getInstance().getReg(param);
//                    mipsInsts.add(new LWInst(Register.SP, fromReg, MIPSManager.getInstance().getValueOffset(param)));
                mipsInsts.add(MIPSManager.getInstance().loadValueToReg(param, fromReg));
//                }
            } else if (param.toValueIR().startsWith("@")) {
                fromReg = Register.K0;
                mipsInsts.add(new LAInst(fromReg, "global_" + param.toValueIR().substring(1)));
            } else {
                fromReg = Register.K0;
                mipsInsts.add(new LIInst(fromReg, param.toValueIR()));
            }

            mipsInsts.add(new ADDIUInst(fromReg, Register.A0, 0));
            if (funcName.equals("putint")) {
                mipsInsts.add(new LIInst(Register.V0, String.valueOf(1)));
            } else if (funcName.equals("putstr")) {
                mipsInsts.add(new LIInst(Register.V0, String.valueOf(4)));
            } else {
                mipsInsts.add(new LIInst(Register.V0, String.valueOf(11)));
            }

            mipsInsts.add(new SYSCALLInst());
        } else if (isInput()) {
//            mipsInsts.addAll(MIPSManager.getInstance().deallocateReg());
            Register reg = MIPSManager.getInstance().getReg(this);
            if (funcName.equals("getint")) {
                mipsInsts.add(new LIInst(Register.V0, String.valueOf(5)));
            } else {
                mipsInsts.add(new LIInst(Register.V0, String.valueOf(12)));
            }
            mipsInsts.add(new SYSCALLInst());
            mipsInsts.add(new ADDIUInst(Register.V0, reg, 0));
            mipsInsts.add(MIPSManager.getInstance().saveRegToMemory(this, reg));
        } else {
            mipsInsts.addAll(MIPSManager.getInstance().storeAllReg());
            int offset = MIPSManager.getInstance().getOffset();
            mipsInsts.add(new MIPSComment("handling params"));
            for (int i = 0; i < params.size(); i++) {
                UsableValue param = params.get(i);
                Register fromReg;
                if (param.toValueIR().startsWith("%")) {
//                    if (MIPSManager.getInstance().hasReg(param)) {
//                        fromReg = MIPSManager.getInstance().getReg(param);
//                    } else {
//                        mipsInsts.addAll(MIPSManager.getInstance().deallocateReg());
                    fromReg = MIPSManager.getInstance().getReg(param);
                    mipsInsts.add(MIPSManager.getInstance().loadValueToReg(param, fromReg));
//                        mipsInsts.add(new LWInst(Register.SP, fromReg, MIPSManager.getInstance().getValueOffset(param)));
//                    }
//                    MIPSManager.getInstance().reserveUsedReg(fromReg);
                } else {
                    mipsInsts.add(new LIInst(Register.K0, param.toValueIR()));
                    fromReg = Register.K0;
                }

                Register reg = getReg(i);
                if (reg != null) {
                    mipsInsts.add(new ADDIUInst(fromReg, reg, 0));
                    mipsInsts.add(new SWInst(Register.SP, reg, offset - 4 * i));
                } else {
                    mipsInsts.add(new SWInst(Register.SP, fromReg, offset - 4 * i));
                }
            }

            MIPSManager.getInstance().releaseRegs();
            mipsInsts.add(new ADDIUInst(Register.SP, Register.SP, offset));
            // 准备工作完成，开始调用
            mipsInsts.add(new JALInst("func_" + funcName));
            // 调用完成，恢复现场
            mipsInsts.add(new ADDIUInst(Register.SP, Register.SP, -offset));
            mipsInsts.addAll(MIPSManager.getInstance().restoreAllReg());

            if (retType != LLVMType.TypeID.VoidTyID) {
//                mipsInsts.addAll(MIPSManager.getInstance().deallocateReg());
                Register reg = MIPSManager.getInstance().getReg(this);
                mipsInsts.add(new ADDIUInst(Register.V0, reg, 0));
                mipsInsts.add(MIPSManager.getInstance().saveRegToMemory(this, reg));
            }
        }
        MIPSManager.getInstance().releaseRegs();

        return mipsInsts;
    }
}
