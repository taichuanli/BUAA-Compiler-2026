package backEnd;

import frontEnd.Trimmer;
import middleEnd.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

public class MIPSGenerator {
    private final LLVMModule llvmModule;
    public MIPSGenerator(LLVMModule llvmModule) {
        this.llvmModule = llvmModule;
    }

    public void translate(String forOutput) {
        if (Trimmer.optimize) {
            LLVMFunction mainFunc = null;
            for (LLVMFunction llvmFunction : llvmModule.LLVMFunctions) {
                if (llvmFunction.name.equals("main")) {
                    mainFunc = llvmFunction;
                }
            }
            MIPSManager.getInstance().setReference(mainFunc);
        }

        MIPSDataSection mipsDataSection = translateMIPSDataSection();
        MIPSTextSection mipsTextSection = translateMIPSTextSection();
        try (FileWriter writer = new FileWriter(forOutput)) {
            writer.write(mipsDataSection.toString());
            writer.write(mipsTextSection.toString());
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }

    private MIPSTextSection translateMIPSTextSection() {
        MIPSTextSection newSection = new MIPSTextSection();
        for (LLVMFunction LLVMFunction : llvmModule.LLVMFunctions) {
            newSection.addFunction(translateMIPSFunction(LLVMFunction));
        }
        return newSection;
    }

    private MIPSFunction translateMIPSFunction(LLVMFunction llvmFunction) {
        MIPSFunction newFunction = new MIPSFunction(llvmFunction.name);
        MIPSManager.getInstance().setCurrentFunction(llvmFunction);
//        int paramsSize = llvmFunction.getParamsSize();
//        MIPSManager.getInstance().subOffset(paramsSize);
//        for (FuncFParam param : llvmFunction.params) {
//            if (MIPSManager.getInstance().hasFreeArgReg()) {
//
//            }
//        }

        llvmFunction.translateEntryBlock();

        for (LLVMBasicBlock basicBlock : llvmFunction.basicBlocks) {
            newFunction.addBasicBlock(translateMIPSBasicBlock(basicBlock));
        }

        //MIPSBasicBlock entryBlock = new MIPSBasicBlock(llvmFunction.name + "_entry");
        //LinkedList<MIPSInst> insts = new LinkedList<>();
        //insts.add(new ADDIUInst(Register.SP, Register.SP, -MIPSManager.getInstance().getOffset()));
        //entryBlock.addInstructions(insts);
        //newFunction.addBasicBlock(0, entryBlock);
        return newFunction;
    }

    private MIPSBasicBlock translateMIPSBasicBlock(LLVMBasicBlock basicBlock) {
        MIPSBasicBlock newBlock = new MIPSBasicBlock(basicBlock.name);
        for (LLVMInstruction instruction : basicBlock.instructions) {
            newBlock.addInstructions(translateMIPSInstruction(instruction));
        }
        return newBlock;
    }

    private LinkedList<MIPSInst> translateMIPSInstruction(LLVMInstruction instruction) {
        return instruction.toMIPS();
    }

    private MIPSDataSection translateMIPSDataSection() {
        MIPSDataSection newSection = new MIPSDataSection();
        for (Value value : llvmModule.globalValues) {
            if (value instanceof GlobalVariable) {
                newSection.addGlobalVariable((GlobalVariable) value);
            } else {
                newSection.addString((GlobalString) value);
            }
        }
        return newSection;
    }
}
