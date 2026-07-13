package middleEnd;

import frontEnd.*;
import frontEnd.lexer.LexType;
import middleEnd.Insts.*;
import middleEnd.utils.ConstCalculator;
import middleEnd.utils.RegTracker;

import java.io.FileWriter;
import java.io.IOException;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Stack;

public class IRGenerator {
    private final ASTNode root;
    private final HashMap<Integer, SymbolTable> oldSymbolTables; // 仅用于快捷查询符号类型，LLVM IR 生成过程中需要新符号表
    private LLVMSymbolTable currTable = null;
    private final Stack<LLVMSymbolTable> symbolTableStack = new Stack<>();
    private final HashMap<Integer, LLVMSymbolTable> newSymbolTables = new HashMap<>();
    private final HashMap<Integer, RegTracker> regTrackers = new HashMap<>();
    private final HashMap<String, LLVMFunction> functions = new HashMap<>();
    private int scopeId = 1;
    private int scopeCnt = 1;
    private final ConstCalculator constCalculator;
    private LLVMType.TypeID funcRetType;
    private final LLVMModule module = new LLVMModule();
    private int strNum = 1;
    private final Stack<LLVMLabel> forBreakLabels = new Stack<>();
    private final Stack<LLVMLabel> forContinueLabels = new Stack<>();
    public IRGenerator(ASTNode root, HashMap<Integer, SymbolTable> oldSymbolTables) {
        this.root = root;
        this.oldSymbolTables = oldSymbolTables;
        this.constCalculator = new ConstCalculator(newSymbolTables);
    }

    public LLVMModule translate(String forOutput) {
        translateModule(root);
        for (RegTracker tracker : regTrackers.values()) {
            if (tracker.getRegNo() > 0) {
                continue;
            }
            tracker.setRegNo();
        }

        for (LLVMFunction function : functions.values()) {
            function.generateBasicBlocks();
        }

        try (FileWriter writer = new FileWriter(forOutput)) {

            /*
            declare i32 @getint()          ; 读取一个整数
            declare i32 @getchar()         ; 读取一个字符
            declare void @putint(i32)      ; 输出一个整数
            declare void @putch(i32)       ; 输出一个字符
            declare void @putstr(i8*)      ; 输出字符串
             */
             writer.write("""
                     declare i32 @getint()          ; 读取一个整数
                     declare i32 @getchar()         ; 读取一个字符
                     declare void @putint(i32)      ; 输出一个整数
                     declare void @putch(i32)       ; 输出一个字符
                     declare void @putstr(i8*)      ; 输出字符串
                     
                     """);
            writer.write(module.toString());
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
        return module;
    }

    private void enterScope() {
        LLVMSymbolTable newTable = new LLVMSymbolTable(scopeCnt++, currTable);
        scopeId = scopeCnt - 1;
        symbolTableStack.push(newTable);
        newSymbolTables.put(newTable.id, newTable);
        currTable = newTable;

        RegTracker tracker = new RegTracker(scopeId);
        regTrackers.put(scopeId, tracker);
    }

    private void exitScope() {
        symbolTableStack.pop();
        try {
            currTable = symbolTableStack.peek();
            scopeId = currTable.id;
        } catch (EmptyStackException e) {
            currTable = null;
        }
    }

    private Symbol getOldSymbol(LeafASTNode node) {
        String identName = node.token.token;
        return oldSymbolTables.get(scopeId).getSymbol(identName);
    }

    private UsableValue getLLVMVariable(String token) {
        LLVMSymbolTable table = currTable;
        while (table != null) {
            if (table.hasVariable(token)) {
                return table.get(token);
            }
            table = table.parentTable;
        }

        throw new RuntimeException("Variable " + token + " not found");
    }

    private void addLLVMVariable(LLVMVariable symbol) {
        newSymbolTables.get(scopeId).addVariable(symbol);
    }

    private void addLLVMFParam(String name, UsableValue value) {
        newSymbolTables.get(scopeId).symbols.put(name, value);
    }

    private void translateModule(ASTNode root) {
        enterScope();
        for (ASTNode child : root.children) {
            if (child.isNode("Decl")) {
                module.addGlobalDecls(translateGlobalDecl(child));
            } else if (child.isNode("FuncDef")) {
                module.addFunction(translateFuncDef(child));
            } else if (child.isNode("MainFuncDef")) {
                module.addFunction(translateMainFuncDef(child));
            }
        }
        exitScope();
    }

    private LinkedList<Value> translateGlobalDecl(ASTNode node) {
        node = node.children.get(0);
        LinkedList<Value> values = new LinkedList<>();
        for (ASTNode child : node.children) {
            if (child.isNode("ConstDef") || child.isNode("VarDef")) {
                LeafASTNode ident = (LeafASTNode) child.children.get(0);
                Symbol symbol = getOldSymbol(ident);
                GlobalVariable var;
                int arrayLength;
                if (symbol.symbolType.toString().endsWith("Array")) {
                    arrayLength = calculateConstExp(child.children.get(2));
                } else {
                    arrayLength = 0;
                }
                var = new GlobalVariable(symbol, arrayLength);
                ASTNode lastChild = child.children.get(child.children.size() - 1);
                ConstInitVal constInitVal;
                if (lastChild.isNode("InitVal") || lastChild.isNode("ConstInitVal")) {
                    constInitVal = translateConstInitVal(lastChild, arrayLength);
                } else {
                    constInitVal = new ConstInitVal();
                }
                constInitVal.padToLength(arrayLength);
                var.setInitVal(constInitVal);
                values.add(var);
                addLLVMVariable(var);
            }
        }
        return values;
    }

    private ConstInitVal translateConstInitVal(ASTNode node, int arrayLength) {
        // 形式上包括常量初值 ConstInitVal 和变量初值 InitVal，但全局变量的初值表达式必须是常量表达式 ConstExp，故按照 ConstInitVal 翻译
        // ConstInitVal  ::= ConstExp | '{' ConstExp { ',' ConstExp } '}' | StringConst
        // InitVal       ::= Exp | '{' Exp { ',' Exp } '}' | StringConst
        // ConstExp      ::= AddExp
        // Exp           ::= AddExp
        if (node.children.get(0).isNode("StringConst")) {
            LeafASTNode leaf = (LeafASTNode) node.children.get(0).children.get(0);
            return new ConstInitVal(leaf.token.token, arrayLength);
        } else {
            ConstInitVal constInitVal = new ConstInitVal();
            for (ASTNode child : node.children) {
                if (child.isNode("ConstExp") || child.isNode("Exp")) {
                    constInitVal.addConstExp(calculateConstExp(child));
                }
            }
            return constInitVal;
        }
    }

    private int calculateConstExp(ASTNode node) {
        // ConstExp      ::= AddExp，但文法规定使用的 Ident 必须是常量，故可以计算得出
        return constCalculator.calculateConstExp(node, scopeId);
    }

    private LLVMFunction translateMainFuncDef(ASTNode node) {
        LLVMFunction main = new LLVMFunction("main", LLVMType.TypeID.IntegerTyID);
        functions.put("main", main);
        funcRetType = main.getReturnType();
        enterScope();
        Block block = new Block();
        LinkedList<LLVMInstruction> instructions = translateBlock(node.children.get(node.children.size() - 1));
        for (int i = 0; i < instructions.size() - 1; i++) {
            if (instructions.get(i) instanceof RetInst && !(instructions.get(i + 1) instanceof LLVMLabel)) {
                instructions.add(i + 1, new LLVMLabel());
            }
        }
        block.addInsts(instructions);
        block.sortAllocaInstsToFront();
        main.setBlock(block);
        regTrackers.get(scopeId).addInstructions(block.getInstructions());
        exitScope();
        return main;
    }

    private LLVMFunction translateFuncDef(ASTNode node) {
        LeafASTNode ident = (LeafASTNode) node.children.get(1);
        Symbol symbol = getOldSymbol(ident);
        LLVMFunction LLVMFunction = new LLVMFunction(symbol);
        functions.put(symbol.token.token, LLVMFunction);
        enterScope();
        funcRetType = LLVMFunction.getReturnType();
        for (ASTNode child : node.children) {
            if (child.isNode("FuncFParams")) {
                LinkedList<FuncFParam> params = translateFuncFParams(child);
                LLVMFunction.setFParams(params);
            }
        }
        Block block = new Block();
        for (FuncFParam param : LLVMFunction.params) {
            AllocaInst allocaInst = new AllocaInst(param.baseType, 0);
            block.addInst(allocaInst);
            block.addInst(new StoreInst(param, allocaInst));
            addLLVMFParam(param.name, allocaInst);
        }
        LinkedList<LLVMInstruction> instructions = translateBlock(node.children.get(node.children.size() - 1));
        for (int i = 0; i < instructions.size() - 1; i++) {
            if (instructions.get(i) instanceof RetInst && !(instructions.get(i + 1) instanceof LLVMLabel)) {
                instructions.add(i + 1, new LLVMLabel());
            }
        }
        block.addInsts(instructions);
        // 检查 instructions 最后一句是不是 ret，不是的话加指令
        if (instructions.isEmpty() || !(instructions.getLast() instanceof RetInst)) {
            block.addInst(new RetInst());
        }
        LLVMFunction.setBlock(block);
        block.sortAllocaInstsToFront();
        regTrackers.get(scopeId).addInstructions(block.getInstructions());
        exitScope();
        return LLVMFunction;
    }

    private LinkedList<FuncFParam> translateFuncFParams(ASTNode node) {
        LinkedList<FuncFParam> params = new LinkedList<>();
        for (ASTNode child : node.children) {
            if (child.isNode("FuncFParam")) {
                FuncFParam funcFParam = translateFuncFParam(child);
                params.add(funcFParam);
                regTrackers.get(scopeId).addValue(funcFParam);
            }
        }
        return params;
    }

    private FuncFParam translateFuncFParam(ASTNode node) {
        LeafASTNode ident = (LeafASTNode) node.children.get(1);
        Symbol symbol = getOldSymbol(ident);
        return new FuncFParam(symbol);
    }

    private LinkedList<LLVMInstruction> translateBlock(ASTNode node) {
        // System.out.println("In translateBlock: " + node.print());
        LinkedList<LLVMInstruction> instructions = new LinkedList<>();
        for (ASTNode child : node.children) {
            if (child.isNode("BlockItem")) {
                instructions.addAll(translateBlockItem(child));
            }
        }
        return instructions;
    }

    private LinkedList<LLVMInstruction> translateBlockItem(ASTNode node) {
        if (node.children.get(0).isNode("Decl")) {
            return translateDecl(node.children.get(0));
        } else {
            return translateStmt(node.children.get(0));
        }
    }

    private LinkedList<LLVMInstruction> translateDecl(ASTNode node) {
        // <Decl> ::= <ConstDecl> | <VarDecl>
        LinkedList<LLVMInstruction> instructions = new LinkedList<>();
        if (node.children.get(0).isNode("ConstDecl")) {
            instructions.addAll(translateConstDecl(node.children.get(0)));
        } else {
            instructions.addAll(translateVarDecl(node.children.get(0)));
        }
        return instructions;
    }

    private LinkedList<LLVMInstruction> translateConstDecl(ASTNode node) {
        LinkedList<LLVMInstruction> list = new LinkedList<>();
        for (ASTNode child : node.children) {
            if (child.isNode("ConstDef") ) {
                LeafASTNode ident = (LeafASTNode) child.children.get(0);
                Symbol symbol = getOldSymbol(ident);
                LLVMVariable var;
                int arrayLength;
                if (symbol.symbolType.toString().endsWith("Array")) {
                    arrayLength = calculateConstExp(child.children.get(2));
                } else {
                    arrayLength = 0;
                }
                var = new LLVMVariable(symbol, arrayLength);
                ASTNode lastChild = child.children.get(child.children.size() - 1);
                ConstInitVal constInitVal = translateConstInitVal(lastChild, arrayLength);
                constInitVal.padToLength(arrayLength);
                var.setInitVal(constInitVal);
                LinkedList<LLVMInstruction> instructions = var.getInstructions();
                addLLVMVariable(var);
                list.addAll(instructions);
            }
        }
        return list;
    }

    private LinkedList<LLVMInstruction> translateVarDecl(ASTNode node) {
        LinkedList<LLVMInstruction> list = new LinkedList<>();
        for (ASTNode child : node.children) {
            if (child.isNode("VarDef")) {
                LeafASTNode ident = (LeafASTNode) child.children.get(0);
                Symbol symbol = getOldSymbol(ident);
                LLVMVariable var;
                int arrayLength;
                if (symbol.symbolType.toString().endsWith("Array")) {
                    arrayLength = calculateConstExp(child.children.get(2));
                } else {
                    arrayLength = 0;
                }
                var = new LLVMVariable(symbol, arrayLength);
                ASTNode lastChild = child.children.get(child.children.size() - 1);
                InitVal initVal = null;
                if (lastChild.isNode("InitVal")) {
                    initVal = translateInitVal(lastChild, arrayLength);
                }
                if (initVal != null && symbol.symbolType.toString().contains("Char")) {
                    initVal.padToLength(arrayLength, LLVMType.TypeID.CharTyID);
                }
                // 不必填充
                var.setInitVal(initVal);
                LinkedList<LLVMInstruction> instructions = var.getInstructions();
                addLLVMVariable(var);
                list.addAll(instructions);
            }
        }
        return list;
    }

    private InitVal translateInitVal(ASTNode node, int arrayLength) {
        // InitVal       ::= Exp | '{' Exp { ',' Exp } '}' | StringConst
        if (node.children.get(0).isNode("StringConst")) {
            LeafASTNode leaf = (LeafASTNode) node.children.get(0).children.get(0);
            return new ConstInitVal(leaf.token.token, arrayLength);
        } else {
            InitVal initVal = new InitVal();
            for (ASTNode child : node.children) {
                if (child.isNode("Exp")) {
                    initVal.addExp(translateExp(child));
                }
            }
            return initVal;
        }
    }

    private LinkedList<LLVMInstruction> translateStmt(ASTNode node) {
        LinkedList<LLVMInstruction> instructions = new LinkedList<>();
        if (node.children.get(0) instanceof LeafASTNode child) {
            switch (child.token.type) {
                case IFTK       -> instructions.addAll(translateIfStmt(node));
                case FORTK      -> instructions.addAll(translateForStmt(node));
                case BREAKTK    -> instructions.addAll(translateBreakStmt());
                case PRINTFTK   -> instructions.addAll(translatePrintfStmt(node));
                case RETURNTK   -> instructions.addAll(translateReturnStmt(node));
                case CONTINUETK -> instructions.addAll(translateContinueStmt());
            }
        } else if (node.children.get(0).isNode("LVal")) {
            if (node.children.size() > 2 && node.children.get(2).isNode("LEAF")) {
                LexType type = ((LeafASTNode) node.children.get(2)).token.type;
                if (type == LexType.GETCHARTK) {
                    instructions.addAll(translateGetCharStmt(node));
                } else {
                    instructions.addAll(translateGetIntStmt(node));
                }
            } else {
                instructions.addAll(translateAssignStmt(node));
            }
        } else if (node.children.get(0).isNode("Exp")) {
            instructions.addAll(translateExpStmt(node));
        } else {
            RegTracker tracker = regTrackers.get(scopeId);
            enterScope();
            regTrackers.put(scopeId, tracker);
            instructions.addAll(translateBlock(node.children.get(0)));
            exitScope();
        }

        return instructions;
    }

    private LinkedList<LLVMInstruction> translateReturnStmt(ASTNode node) {
        LinkedList<LLVMInstruction> instructions = new LinkedList<>();
        if (node.children.size() > 1 && node.children.get(1).isNode("Exp")) {
            LLVMExp exp = translateExp(node.children.get(1));
            if (exp instanceof LLVMConst llvmConst) {
                llvmConst.changeType(funcRetType);
                instructions.add(new RetInst(llvmConst));
                return instructions;
            }
            instructions.addAll(exp.instructions);
            if (funcRetType == LLVMType.TypeID.CharTyID && !exp.value.toLLVMType().contains("i8")){
                TruncInst truncInst = new TruncInst(exp.value, LLVMType.TypeID.CharTyID);
                instructions.add(truncInst);
                exp.addUsableInstruction(truncInst);
            } else if (funcRetType == LLVMType.TypeID.IntegerTyID && !exp.value.toLLVMType().contains("i32")) {
                ZextInst zextInst = new ZextInst(exp.value, LLVMType.TypeID.IntegerTyID);
                instructions.add(zextInst);
                exp.addUsableInstruction(zextInst);
            }
            instructions.add(new RetInst(exp.value));
        } else {
            instructions.add(new RetInst());
        }
        return instructions;
    }

    private LinkedList<LLVMInstruction> translateBreakStmt() {
        LinkedList<LLVMInstruction> instructions = new LinkedList<>();
        instructions.add(new BranchInst(forBreakLabels.peek()));
        instructions.add(new LLVMLabel());
        return instructions;
    }

    private LinkedList<LLVMInstruction> translateContinueStmt() {
        LinkedList<LLVMInstruction> instructions = new LinkedList<>();
        instructions.add(new BranchInst(forContinueLabels.peek()));
        instructions.add(new LLVMLabel());
        return instructions;
    }

//    private LinkedList<Instruction> translateFromCond(ASTNode condNode, LLVMLabel trueLabel, LLVMLabel falseLabel) {
//        assert condNode.isNode("Cond");
//
//        LinkedList<Instruction> instructions = new LinkedList<>();
//        ASTNode LOrExpNode = condNode.children.get(0);
//
//
//        boolean condAlwaysFalse = true;
//        for (int i = 0; i < LOrExpNode.children.size(); i += 2) {
//            ASTNode LAndExpNode = LOrExpNode.children.get(i);
//            if (LAndExpNode.isNode("LAndExp")) {
//                LLVMLabel LAndExpIsFalse;
//                if (i == LOrExpNode.children.size() - 1) {
//                    LAndExpIsFalse = falseLabel;
//                } else {
//                    LAndExpIsFalse = new LLVMLabel();
//                }
//                LinkedList<Instruction> LAndExpInstructions = new LinkedList<>();
//                LLVMExp eqExp = null;
//                boolean hasLLVMExp = false;
//                for (int j = 0; j < LAndExpNode.children.size(); j += 2) {
//                    ASTNode EqExpNode = LAndExpNode.children.get(j);
//                    if (EqExpNode.isNode("EqExp")) {
//                        LLVMLabel EqExpIsTrue = new LLVMLabel();
//                        eqExp = translateEqExp(EqExpNode);
//                        if (eqExp instanceof LLVMConst constEqExp) {
//                            if (constEqExp.constValue == 0) {
//                                eqExp = null;
//                                break;
//                            } else {
//                                eqExp = new LLVMExp(eqExp);
//                            }
//                        } else {
//                            hasLLVMExp = true;
//                        }
//                        if (!eqExp.toLLVMType().contains("i1")) {
//                            eqExp.logical();
//                        }
//                        LAndExpInstructions.addAll(eqExp.instructions);
//                        if (j != LAndExpNode.children.size() - 1) {
//                            BranchInst branchInst = new BranchInst(eqExp, EqExpIsTrue, LAndExpIsFalse);
//                            LAndExpInstructions.add(branchInst);
//                            LAndExpInstructions.add(EqExpIsTrue);
//                        }
//                    }
//                }
//                if (!hasLLVMExp && eqExp != null) {
//                    // 整个条件可短路为 1
//                    instructions = new LinkedList<>();
//                    instructions.add(new BranchInst(trueLabel));
//                    return instructions;
//                }
//
//                if (eqExp == null) {
//                    continue;
//                } else {
//                    condAlwaysFalse = false;
//                }
//                instructions.addAll(LAndExpInstructions);
//                BranchInst branchInst = new BranchInst(eqExp, trueLabel, LAndExpIsFalse);
//                instructions.add(branchInst);
//                if (i != LOrExpNode.children.size() - 1) {
//                    instructions.add(LAndExpIsFalse);
//                }
//            }
//        }
//
//        if (condAlwaysFalse) {
//            // 整个条件可短路为 0
//            instructions = new LinkedList<>();
//            instructions.add(new BranchInst(falseLabel));
//            return instructions;
//        }
//        return instructions;
//    }

    private LinkedList<LLVMInstruction> translateFromCond(ASTNode condNode, LLVMLabel trueLabel, LLVMLabel falseLabel) {
        assert condNode.isNode("Cond");

        LinkedList<LLVMInstruction> instructions = new LinkedList<>();
        ASTNode LOrExpNode = condNode.children.get(0);

//        boolean condAlwaysFalse = true;
        for (int i = 0; i < LOrExpNode.children.size(); i += 2) {
            ASTNode LAndExpNode = LOrExpNode.children.get(i);
            if (LAndExpNode.isNode("LAndExp")) {
                LLVMLabel nextLAndExp;
                if (i != LOrExpNode.children.size() - 1) {
                    nextLAndExp = new LLVMLabel();
                } else {
                    nextLAndExp = falseLabel;
                }
//                boolean hasLLVMExp = false;
                LLVMExp eqExp;
                for (int j = 0; j < LAndExpNode.children.size(); j += 2) {
                    ASTNode EqExpNode = LAndExpNode.children.get(j);
                    if (EqExpNode.isNode("EqExp")) {
                        LLVMLabel nextEqExp;
                        if (j != LAndExpNode.children.size() - 1) {
                            nextEqExp = new LLVMLabel();
                        } else {
                            nextEqExp = trueLabel;
                        }
                        eqExp = translateEqExp(EqExpNode);
                        if (eqExp instanceof LLVMConst constEqExp) {
//                            if (constEqExp.constValue != 0) {
//                                continue;
//                            }
                            eqExp = new LLVMExp(constEqExp);
                        }
                        if (!eqExp.value.toLLVMType().contains("i1")) {
                            eqExp.logical();
                        }
                        instructions.addAll(eqExp.instructions);
                        instructions.add(new BranchInst(eqExp.value, nextEqExp, nextLAndExp));
                        if (j != LAndExpNode.children.size() - 1) {
                            instructions.add(nextEqExp);
                        }
                    }
                }

//                if (!hasLLVMExp && eqExp != null) {
//                    // 整个条件可短路为 1
//                    instructions.add(new BranchInst(trueLabel));
//                    return instructions;
//                }

//                if (eqExp == null) {
//                    continue;
//                } else {
//                    condAlwaysFalse = false;
//                }

//                instructions.add(new BranchInst(eqExp, trueLabel, nextLAndExp));

                if (i != LOrExpNode.children.size() - 1) {
                    instructions.add(nextLAndExp);
                }
            }
        }

//        if (condAlwaysFalse) {
//            instructions.add(new BranchInst(falseLabel));
//            return instructions;
//        }

        return instructions;
    }

    private LinkedList<LLVMInstruction> translateIfStmt(ASTNode node) {
        // 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
        // Cond == 1 : to stmt1
        // Cond == 0 : to stmt2
        // stmt1 to nextBlock
        // stmt2 to nextBlock
        // 对于 LOrExp: 如果 LAndExp 有一个为常值 1，则优化整个 LOrExp 为 1； 如果 LAndExp 结果为 1，则跳转到 stmt1，否则跳转到下一个 LAndExp / stmt2
        // 对于 LAndExp: 如果 EqExp 有一个为常值 0，则优化整个 LAndExp 为 0； 如果 EqExp 结果为 0，则跳转到下一个 LAndExp，否则跳转到下一个 EqExp

        LLVMLabel condIsTrue = new LLVMLabel();
        LLVMLabel condIsFalse = new LLVMLabel();
        LLVMLabel afterIfStmt = new LLVMLabel();
        // LLVMLabel stmt2Label = new LLVMLabel();
        // if (node.children.size() > 5) {
            // 有 else 语句
            // stmt2Label = new LLVMLabel();
            // condIsFalse = stmt2Label;
        // } else {
            // 没有 else 语句
            // condIsFalse = afterIfStmt;
        // }

        LinkedList<LLVMInstruction> condInsts = translateFromCond(node.children.get(2), condIsTrue, condIsFalse);
        // if (condInsts.size() == 1 && condInsts.get(0) instanceof BranchInst branchInst) {
        //     if (condIsTrue.equals(branchInst.dest)) {
        //         instructions.addAll(stmt1Instructions);
        //     } else if (stmt2Label != null && stmt2Label.equals(branchInst.dest)) {
        //         instructions.addAll(translateStmt(node.children.get(6)));
        //     }
        //     return instructions;
        // }

        LinkedList<LLVMInstruction> instructions = new LinkedList<>(condInsts);
        instructions.add(condIsTrue);
        instructions.addAll(translateStmt(node.children.get(4)));
        instructions.add(new BranchInst(afterIfStmt));
        // if (stmt2Label != null) {
        instructions.add(condIsFalse);
        if (node.children.size() > 5) {
            instructions.addAll(translateStmt(node.children.get(6)));
        }
        instructions.add(new BranchInst(afterIfStmt));
        // }
        instructions.add(afterIfStmt);
        return instructions;
    }

    private LLVMExp translateEqExp(ASTNode node) {
        if (node.children.size() == 1) {
            return translateRelExp(node.children.get(0));
        } else {
            LLVMExp left = translateEqExp(node.children.get(0));
            LLVMExp right = translateRelExp(node.children.get(2));
            LexType lexType = ((LeafASTNode) node.children.get(1)).token.type;
            if (left instanceof LLVMConst constLeft && right instanceof LLVMConst constRight) {
                switch (lexType) {
                    case EQL -> {
                        return constLeft.binaryOperate(LLVMType.InstType.ICMP_EQ, constRight);
                    }

                    case NEQ -> {
                        return constLeft.binaryOperate(LLVMType.InstType.ICMP_NE, constRight);
                    }
                }
            } else if (left instanceof LLVMConst constLeft) {
                left = new LLVMExp(constLeft);
            }
            if (!left.value.toLLVMType().equals("i32")) {
                left.addUsableInstruction(new ZextInst(left.value, LLVMType.TypeID.IntegerTyID));
            }
            if (!right.value.toLLVMType().equals("i32")) {
                if (right instanceof LLVMConst constRight) {
                    right = new LLVMExp(constRight);
                }
                right.addUsableInstruction(new ZextInst(right.value, LLVMType.TypeID.IntegerTyID));
            }
            switch (lexType) {
                case EQL -> left.binaryOperate(LLVMType.InstType.ICMP_EQ, right);
                case NEQ -> left.binaryOperate(LLVMType.InstType.ICMP_NE, right);
            }
            return left;
        }
    }

    private LLVMExp translateRelExp(ASTNode node) {
        if (node.children.size() == 1) {
            return translateAddExp(node.children.get(0));
        } else {
            LLVMExp left = translateRelExp(node.children.get(0));
            LLVMExp right = translateAddExp(node.children.get(2));
            LexType lexType = ((LeafASTNode) node.children.get(1)).token.type;
            if (left instanceof LLVMConst constLeft && right instanceof LLVMConst constRight) {
                switch (lexType) {
                    case LSS -> {
                        return constLeft.binaryOperate(LLVMType.InstType.ICMP_SLT, constRight);
                    }
                    case LEQ -> {
                        return constLeft.binaryOperate(LLVMType.InstType.ICMP_SLE, constRight);
                    }
                    case GRE -> {
                        return constLeft.binaryOperate(LLVMType.InstType.ICMP_SGT, constRight);
                    }
                    case GEQ -> {
                        return constLeft.binaryOperate(LLVMType.InstType.ICMP_SGE, constRight);
                    }
                }
            } else if (left instanceof LLVMConst constLeft) {
                left = new LLVMExp(constLeft);
            }
            if (!left.value.toLLVMType().equals("i32")) {
                left.addUsableInstruction(new ZextInst(left.value, LLVMType.TypeID.IntegerTyID));
            }
            if (!right.value.toLLVMType().equals("i32")) {
                if (right instanceof LLVMConst constRight) {
                    right = new LLVMExp(constRight);
                }
                right.addUsableInstruction(new ZextInst(right.value, LLVMType.TypeID.IntegerTyID));
            }
            switch (lexType) {
                case LSS -> left.binaryOperate(LLVMType.InstType.ICMP_SLT, right);
                case LEQ -> left.binaryOperate(LLVMType.InstType.ICMP_SLE, right);
                case GRE -> left.binaryOperate(LLVMType.InstType.ICMP_SGT, right);
                case GEQ -> left.binaryOperate(LLVMType.InstType.ICMP_SGE, right);
            }
            return left;
        }
    }

    private LinkedList<LLVMInstruction> translateForStmt(ASTNode node) {
        // 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
        LinkedList<LLVMInstruction> initInstructions = new LinkedList<>();
        int index;
        if (node.children.get(2).isNode("ForStmt")) {
            initInstructions = translateAssignStmt(node.children.get(2));
            index = 4;
        } else {
            index = 3;
        }
        LLVMLabel condIsTrue = new LLVMLabel();
        LLVMLabel condIsFalse = new LLVMLabel();
        LLVMLabel toCond = new LLVMLabel();
        LLVMLabel toUpdate = new LLVMLabel();
        LinkedList<LLVMInstruction> condInstructions = new LinkedList<>();
        if (node.children.get(index).isNode("Cond")) {
            ASTNode condNode = node.children.get(index);
            condInstructions = translateFromCond(condNode, condIsTrue, condIsFalse);
            index += 2;
        } else {
            // 无条件循环
            // toCond = condIsTrue;
            index += 1;
        }
        LinkedList<LLVMInstruction> updateInstructions = new LinkedList<>();
        if (node.children.get(index).isNode("ForStmt")) {
            updateInstructions = translateAssignStmt(node.children.get(index));
        }   //  else {
            // toUpdate = toCond;
        // }

        forBreakLabels.push(condIsFalse);
        forContinueLabels.push(toUpdate);

        LinkedList<LLVMInstruction> bodyInstructions = translateStmt(node.children.get(node.children.size() - 1));
        LinkedList<LLVMInstruction> instructions = new LinkedList<>(initInstructions);
        instructions.add(new BranchInst(toCond));
        instructions.add(toCond);
        // if (!condInstructions.isEmpty()) {
        if (!condInstructions.isEmpty()) {
            instructions.addAll(condInstructions);
        } else {
            instructions.add(new BranchInst(condIsTrue));
        }
        instructions.add(condIsTrue);
        // }
        // if (!bodyInstructions.isEmpty()) {
        instructions.addAll(bodyInstructions);
        instructions.add(new BranchInst(toUpdate));
        // }
        // if (!updateInstructions.isEmpty()) {
        instructions.add(toUpdate);
        instructions.addAll(updateInstructions);
        instructions.add(new BranchInst(toCond));
        // }
        instructions.add(condIsFalse);

        forBreakLabels.pop();
        forContinueLabels.pop();
        return instructions;
    }

    private LinkedList<LLVMInstruction> translatePrintfStmt(ASTNode node) {
        LinkedList<LLVMInstruction> instructions = new LinkedList<>();
        LinkedList<LLVMExp> exps = new LinkedList<>();

        for (ASTNode child : node.children) {
            if (child.isNode("Exp")) {
                LLVMExp exp = translateExp(child);
                instructions.addAll(exp.instructions);
                exps.add(exp);
            }
        }

        LeafASTNode str = (LeafASTNode) node.children.get(2).children.get(0);
        String strVal = str.token.token;
        // 根据 strVal 拆分出常量字符串与函数调用
        StringBuilder tmp = new StringBuilder();
        int tmpLen = 0;
        boolean hasPercent = false;
        boolean hasSlash = false;
        int currExpIndex = 0;
        for (int i = 1; i < strVal.length() - 1; i++) {

            if (hasSlash) {
                if (strVal.charAt(i) == 'n') {
                    tmp.append("\\0A");
                    tmpLen += 1;
                } else {
                    tmp.append('\\');
                    tmp.append(strVal.charAt(i));
                    tmpLen += 2;
                }
                hasSlash = false;
                continue;
            }

            if (hasPercent) {
                hasPercent = false;
                if (strVal.charAt(i) == 'd') {
                    if (!tmp.isEmpty()) {
                        tmp.append("\\00");
                        tmpLen += 1;
                        instructions.addAll(PrintConstStr(tmp.toString(), tmpLen));
                        tmp = new StringBuilder();
                        tmpLen = 0;
                    }

                    LLVMExp exp = exps.get(currExpIndex++);
                    LinkedList<UsableValue> params = new LinkedList<>();
                    if (!exp.value.toLLVMType().contains("i32")) {
                        ZextInst zextInst = new ZextInst(exp.value, LLVMType.TypeID.IntegerTyID);
                        instructions.add(zextInst);
                        params.add(zextInst);
                    } else {
                        params.add(exp.value);
                    }
                    CallInst callInst = new CallInst("putint", params);
                    instructions.add(callInst);
                } else if (strVal.charAt(i) == 'c') {
                    if (!tmp.isEmpty()) {
                        tmp.append("\\00");
                        tmpLen += 1;
                        instructions.addAll(PrintConstStr(tmp.toString(), tmpLen));
                        tmp = new StringBuilder();
                        tmpLen = 0;
                    }

                    LLVMExp exp = exps.get(currExpIndex++);
                    LinkedList<UsableValue> params = new LinkedList<>();
                    if (!exp.value.toLLVMType().contains("i32")) {
                        ZextInst zextInst = new ZextInst(exp.value, LLVMType.TypeID.IntegerTyID);
                        instructions.add(zextInst);
                        params.add(zextInst);
                    } else {
                        params.add(exp.value);
                    }
                    CallInst callInst = new CallInst("putch", params);
                    instructions.add(callInst);
                } else {
                    tmp.append('%');
                    tmp.append(strVal.charAt(i));
                    tmpLen += 2;
                }
                continue;
            }

            if (strVal.charAt(i) == '%') {
                hasPercent = true;
                continue;
            }

            if (strVal.charAt(i) == '\\') {
                hasSlash = true;
                continue;
            }

            tmp.append(strVal.charAt(i));
            tmpLen += 1;

        }

        if (!tmp.isEmpty()) {
            tmp.append("\\00");
            tmpLen += 1;
            instructions.addAll(PrintConstStr(tmp.toString(), tmpLen));
        }

        return instructions;
    }

    private LinkedList<LLVMInstruction> PrintConstStr(String string, int tmpLen) {
        LinkedList<LLVMInstruction> instructions = new LinkedList<>();
        GlobalString newStr = new GlobalString(strNum++, string, tmpLen);
        module.addStrDecl(newStr);
        GetelementptrInst getInst = new GetelementptrInst(LLVMType.TypeID.CharTyID, newStr, new LLVMConst(LLVMType.TypeID.IntegerTyID, 0));
        instructions.add(getInst);
        LinkedList<UsableValue> param = new LinkedList<>();
        param.add(getInst);
        CallInst callInst = new CallInst("putstr", param);
        instructions.add(callInst);
        return instructions;
    }

    private LinkedList<LLVMInstruction> translateGetCharStmt(ASTNode node) {
        LLVMExp lval = translateLVal(node.children.get(0));
        LinkedList<LLVMInstruction> instructions = new LinkedList<>(lval.instructions);

        CallInst callInst = new CallInst(LLVMType.TypeID.IntegerTyID, "getchar");
        instructions.add(callInst);
        if (callInst.isDifferentType(lval.value)) {
            LLVMInstruction fix = callInst.fix();
            instructions.add(fix);
            instructions.add(new StoreInst((UsableValue) fix, lval.value));
        } else {
            instructions.add(new StoreInst(callInst, lval.value));
        }
        return instructions;
    }

    private LinkedList<LLVMInstruction> translateGetIntStmt(ASTNode node) {
        LLVMExp lval = translateLVal(node.children.get(0));
        LinkedList<LLVMInstruction> instructions = new LinkedList<>(lval.instructions);

        CallInst callInst = new CallInst(LLVMType.TypeID.IntegerTyID, "getint");
        instructions.add(callInst);
        if (callInst.isDifferentType(lval.value)) {
            LLVMInstruction fix = callInst.fix();
            instructions.add(fix);
            instructions.add(new StoreInst((UsableValue) fix, lval.value));
        } else {
            instructions.add(new StoreInst(callInst, lval.value));
        }
        return instructions;
    }

    private LinkedList<LLVMInstruction> translateAssignStmt(ASTNode node) {
        LLVMExp exp = translateExp(node.children.get(2));
        if (exp instanceof LLVMConst llvmConst) {
            exp = new LLVMExp(llvmConst);
        }
        LinkedList<LLVMInstruction> instructions = new LinkedList<>(exp.instructions);
        LLVMExp lval = translateLVal(node.children.get(0));
        instructions.addAll(lval.instructions);
        if (lval.value.toLLVMType().contains("i32") && !exp.value.toLLVMType().contains("i32")) {
            ZextInst zextInst = new ZextInst(exp.value, LLVMType.TypeID.IntegerTyID);
            exp.addUsableInstruction(zextInst);
            instructions.add(zextInst);
        } else if (lval.value.toLLVMType().contains("i8") && !exp.value.toLLVMType().contains("i8")) {
            TruncInst truncInst = new TruncInst(exp.value, LLVMType.TypeID.CharTyID);
            exp.addUsableInstruction(truncInst);
            instructions.add(truncInst);
        }

        instructions.add(new StoreInst(exp.value, lval.value));

        return instructions;
    }

    private LLVMExp translateLVal(ASTNode node) {
        LeafASTNode leaf = (LeafASTNode) node.children.get(0);
        UsableValue var = getLLVMVariable(leaf.token.token);
        assert var != null;
        if (node.children.size() > 3) {
            LLVMType.TypeID baseType;
            LLVMExp exp = translateExp(node.children.get(2));
            if (exp instanceof LLVMConst llvmConst) {
                exp = new LLVMExp(llvmConst);
            }
            if (var.toLLVMType().contains("i32")) {
                baseType = LLVMType.TypeID.IntegerTyID;
            } else {
                baseType = LLVMType.TypeID.CharTyID;
            }
            GetelementptrInst getInst;
            if (var.toLLVMType().contains("**")) {
                LoadInst loadInst = new LoadInst(baseType.toPointerType(), var);
                getInst = new GetelementptrInst(baseType, loadInst, exp.value);
                exp.addUsableInstruction(loadInst);
            } else {
                getInst = new GetelementptrInst(baseType, var, exp.value);
            }
            exp.addUsableInstruction(getInst);
            return exp;
        } else {
            return new LLVMExp(var);
        }
    }

    private LinkedList<LLVMInstruction> translateExpStmt(ASTNode node) {
        if (node.children.get(0) instanceof LeafASTNode) {
            return new LinkedList<>();
        }

        return translateExp(node.children.get(0)).getInstructions();
    }


    private LLVMExp translateExp(ASTNode node) {
        return translateAddExp(node.children.get(0));
    }

    private LLVMExp translateAddExp(ASTNode node) {
        if (node.children.size() == 1) {
            return translateMulExp(node.children.get(0));
        } else {
            LLVMExp left = translateAddExp(node.children.get(0));
            LLVMExp right = translateMulExp(node.children.get(2));
            String token = ((LeafASTNode) node.children.get(1)).token.token;
            if (left instanceof LLVMConst constLeft && right instanceof LLVMConst constRight) {
                if (token.equals("+")) {
                    return constLeft.binaryOperate(LLVMType.InstType.ADD, constRight);
                } else {
                    return constLeft.binaryOperate(LLVMType.InstType.SUB, constRight);
                }
            }

            if (Trimmer.optimize) {
                if (left instanceof LLVMConst constLeft && constLeft.constValue == 0) {
                    return right;
                }

                if (right instanceof LLVMConst constRight && constRight.constValue == 0) {
                    return left;
                }
            }

            if (left instanceof LLVMConst constLeft) {
                left = new LLVMExp(constLeft);
            }
            if (!left.value.toLLVMType().equals("i32")) {
                left.addUsableInstruction(new ZextInst(left.value, LLVMType.TypeID.IntegerTyID));
            }
            if (right instanceof LLVMConst constRight) {
                right = new LLVMExp(constRight);
            }
            if (!right.value.toLLVMType().equals("i32")) {
                right.addUsableInstruction(new ZextInst(right.value, LLVMType.TypeID.IntegerTyID));
            }
            if (token.equals("+")){
                return left.binaryOperate(LLVMType.InstType.ADD, right);
            } else {
                return left.binaryOperate(LLVMType.InstType.SUB, right);
            }
        }
    }

    private LLVMExp translateMulExp(ASTNode node) {
        if (node.children.size() == 1) {
            return translateUnaryExp(node.children.get(0));
        } else {
            LLVMExp left = translateMulExp(node.children.get(0));
            LLVMExp right = translateUnaryExp(node.children.get(2));
            String token = ((LeafASTNode) node.children.get(1)).token.token;
            if (left instanceof LLVMConst constLeft && right instanceof LLVMConst constRight) {
                if (token.equals("*")) {
                    return constLeft.binaryOperate(LLVMType.InstType.MUL, constRight);
                } else if (token.equals("/")) {
                    return constLeft.binaryOperate(LLVMType.InstType.SDIV, constRight);
                } else {
                    return constLeft.binaryOperate(LLVMType.InstType.SREM, constRight);

                }
            }

            if (Trimmer.optimize) {
                switch (token) {
                    case "*" -> {
                        if (left instanceof LLVMConst constLeft) {
                            int val = constLeft.constValue;
                            if (val == 0) {
                                return new LLVMConst(LLVMType.TypeID.IntegerTyID, 0);
                            } else if (val == 1) {
                                return right;
                            } else if (isPowerOf2(val)) {
                                int log = log2(val);
                                LLVMExp exp = new LLVMExp(new LLVMConst(LLVMType.TypeID.IntegerTyID, log));
                                return right.binaryOperate(LLVMType.InstType.SHL, exp);
                            }
                        }

                        if (right instanceof LLVMConst constRight) {
                            int val = constRight.constValue;
                            if (val == 0) {
                                return new LLVMConst(LLVMType.TypeID.IntegerTyID, 0);
                            } else if (val == 1) {
                                return left;
                            } else if (isPowerOf2(val)) {
                                int log = log2(val);
                                LLVMExp exp = new LLVMExp(new LLVMConst(LLVMType.TypeID.IntegerTyID, log));
                                return left.binaryOperate(LLVMType.InstType.SHL, exp);
                            }
                        }
                    }
                    case "/" -> {
                        if (right instanceof LLVMConst constRight) {
                            int val = constRight.constValue;
                            if (val == 1) {
                                return left;
                            }
                        }
                    }
                    case "%" -> {
                        if (right instanceof LLVMConst constRight) {
                            int val = constRight.constValue;
                            if (val == 1) {
                                return new LLVMConst(LLVMType.TypeID.IntegerTyID, 0);
                            }
                        }
                    }
                }

            }

            if (left instanceof LLVMConst constLeft) {
                left = new LLVMExp(constLeft);
            }
            if (!left.value.toLLVMType().equals("i32")) {
                left.addUsableInstruction(new ZextInst(left.value, LLVMType.TypeID.IntegerTyID));
            }
            if (right instanceof LLVMConst constRight) {
                right = new LLVMExp(constRight);
            }
            if (!right.value.toLLVMType().equals("i32")) {
                right.addUsableInstruction(new ZextInst(right.value, LLVMType.TypeID.IntegerTyID));
            }
            if (((LeafASTNode) node.children.get(1)).token.token.equals("*")) {
                return left.binaryOperate(LLVMType.InstType.MUL, right);
            } else if (((LeafASTNode) node.children.get(1)).token.token.equals("/")) {
                return left.binaryOperate(LLVMType.InstType.SDIV, right);
            } else {
                return left.binaryOperate(LLVMType.InstType.SREM, right);
            }
        }
    }

    private boolean isPowerOf2(int val) {
        if (val <= 0) {
            return false;
        }
        return (val & (val - 1)) == 0;
    }

    private int log2(int val) {
        return Integer.numberOfTrailingZeros(val);
    }


    private LLVMExp translateUnaryExp(ASTNode node) {
        if (node.children.size() == 1) {
            return translatePrimaryExp(node.children.get(0));
        } else if (node.children.get(0).isNode("UnaryOp")) {
            LeafASTNode leaf = (LeafASTNode) node.children.get(0).children.get(0);
            LLVMExp unaryExp = translateUnaryExp(node.children.get(1));
            if (unaryExp instanceof LLVMConst constExp) {
                return switch (leaf.token.token) {
                    case "+" -> constExp;
                    case "-" -> constExp.negate();
                    default -> constExp.logicalNot();
                };
            } else {
                return switch (leaf.token.token) {
                    case "+" -> unaryExp;
                    case "-" -> unaryExp.negate();
                    default -> unaryExp.logicalNot();
                };
            }
        } else {
            LLVMExp exp = new LLVMExp();
            LinkedList<LLVMExp> realParams = new LinkedList<>();
            LeafASTNode leaf = (LeafASTNode) node.children.get(0);
            LLVMFunction toCall = functions.get(leaf.token.token);
            if (node.children.get(2).isNode("FuncRParams")) {
                realParams = new LinkedList<>(translateFuncRParams(node.children.get(2)));
            }
            for (LLVMExp realParam : realParams) {
                exp.addFromExp(realParam);
            }
            LinkedList<UsableValue> forCall = new LinkedList<>();
            for (int i = 0; i < toCall.params.size(); i++) {
                boolean toFix = toCall.params.get(i).isDifferentType(realParams.get(i).value);
                if (toFix) {
                    LLVMInstruction fix = toCall.params.get(i).fix(realParams.get(i).value);
                    exp.addUsableInstruction(fix);
                    forCall.add((UsableValue) fix);
                } else {
                    forCall.add(realParams.get(i).value);
                }
            }
            CallInst callInst;
            if (toCall.retType == LLVMType.TypeID.VoidTyID) {
                callInst = new CallInst(toCall.name, forCall);
            } else {
                callInst = new CallInst(toCall.retType, toCall.name, forCall);
            }
            exp.addUsableInstruction(callInst);
            if (toCall.retType == LLVMType.TypeID.CharTyID) {
                exp.addUsableInstruction(new ZextInst(callInst, LLVMType.TypeID.IntegerTyID));
            }
            return exp;
        }
    }

    private LinkedList<LLVMExp> translateFuncRParams(ASTNode node) {
        LinkedList<LLVMExp> realParams = new LinkedList<>();
        for (ASTNode child : node.children) {
            if (child.isNode("Exp")) {
                realParams.add(translateExp(child));
            }
        }
        return realParams;
    }

    private LLVMExp translatePrimaryExp(ASTNode node) {
        if (node.children.size() > 1) {
            return translateExp(node.children.get(1));
        } else if (node.children.get(0).isNode("LVal")) {
            return translateLValAsExp(node.children.get(0));
        } else {
            int val;
            String token = ((LeafASTNode) node.children.get(0).children.get(0)).token.token;
            if (node.children.get(0).isNode("Number")) {
                val = Integer.parseInt(token);
            } else {
                if (token.charAt(1) == '\\') {
                    val = switch (token.charAt(2)) {
                        case '0' -> 0;
                        case 'a' -> 7;
                        case 'b' -> 8;
                        case 't' -> 9;
                        case 'n' -> 10;
                        case 'v' -> 11;
                        case 'f' -> 12;
                        case '\"' -> 34;
                        case '\'' -> 39;
                        case '\\' -> 92;
                        default -> throw new RuntimeException("Unknown escape sequence");
                    };
                } else {
                    val = token.charAt(1);
                }
            }
            return new LLVMConst(LLVMType.TypeID.IntegerTyID, val);
        }
    }

    private LLVMExp translateLValAsExp(ASTNode node) {
        LeafASTNode leaf = (LeafASTNode) node.children.get(0);
        UsableValue value = getLLVMVariable(leaf.token.token);
        LLVMExp indexExp;
        int indexVal = -1;
        if (node.children.size() > 3) {
            indexExp = translateExp(node.children.get(2));
            if (indexExp instanceof LLVMConst constIndex) {
                indexVal = constIndex.constValue;
            }
            boolean isConst = false;
            if (value instanceof LLVMVariable var) {
                isConst = var.isConst;
            }
            if (isConst) {
                ConstInitVal constInitVal = (ConstInitVal) ((LLVMVariable) value).initVal;
                if (indexVal != -1) {
                    int val = constInitVal.getConstValue(indexVal);
                    return new LLVMConst(LLVMType.TypeID.IntegerTyID, val);
                }
            }
             if (indexExp instanceof LLVMConst llvmConst) {
                 indexExp = new LLVMExp(llvmConst);
             }
            LLVMType.TypeID baseType;
            if (value.toLLVMType().contains("i32")) {
                baseType = LLVMType.TypeID.IntegerTyID;
            } else {
                baseType = LLVMType.TypeID.CharTyID;
            }
            GetelementptrInst getInst;
            if (value.toLLVMType().contains("**")) {
                LoadInst loadInst = new LoadInst(baseType.toPointerType(), value);
                getInst = new GetelementptrInst(baseType, loadInst, indexExp.value);
                indexExp.addUsableInstruction(loadInst);
            } else {
                getInst = new GetelementptrInst(baseType, value, indexExp.value);
            }
            indexExp.addUsableInstruction(getInst);
            LoadInst loadInst = new LoadInst(baseType, indexExp.value);
            indexExp.addUsableInstruction(loadInst);
            if (baseType == LLVMType.TypeID.CharTyID) {
                indexExp.addUsableInstruction(new ZextInst(loadInst, LLVMType.TypeID.IntegerTyID));
            }
            return indexExp;
        } else {
//            indexVal = 0;
//            boolean isConst = false;
//            if (value instanceof LLVMVariable var) {
//                isConst = var.isConst;
//            }
//            if (isConst) {
//                ConstInitVal constInitVal = (ConstInitVal) ((LLVMVariable) value).initVal;
//                int val = constInitVal.getConstValue(indexVal);
//                return new LLVMConst(LLVMType.TypeID.IntegerTyID, val);
//            }
            LLVMExp lVal = new LLVMExp(value);
            LLVMType.TypeID baseType;
            if (lVal.value.toLLVMType().contains("i32")) {
                baseType = LLVMType.TypeID.IntegerTyID;
            } else {
                baseType = LLVMType.TypeID.CharTyID;
            }
            if (lVal.value.toLLVMType().charAt(0) == '[') {
                GetelementptrInst getInst = new GetelementptrInst(baseType, lVal.value, new LLVMConst(LLVMType.TypeID.IntegerTyID, 0));
                lVal.addUsableInstruction(getInst);
                return lVal;
            } else if (lVal.value.toLLVMType().contains("**")) {
                LoadInst loadInst = new LoadInst(baseType.toPointerType(), lVal.value);
                lVal.addUsableInstruction(loadInst);
                return lVal;
            } else if (value instanceof LLVMVariable var) {
                // 不是数组，有可能是个常量
                if (var.isConst) {
                    ConstInitVal constInitVal = (ConstInitVal) ((LLVMVariable) value).initVal;
                    int val = constInitVal.getConstValue(0);
                    return new LLVMConst(LLVMType.TypeID.IntegerTyID, val);
                }
            }
            LoadInst loadInst = new LoadInst(baseType, lVal.value);
            lVal.addUsableInstruction(loadInst);
            if (baseType == LLVMType.TypeID.CharTyID) {
                lVal.addUsableInstruction(new ZextInst(loadInst, LLVMType.TypeID.IntegerTyID));
            }
            return lVal;
        }
    }
}
