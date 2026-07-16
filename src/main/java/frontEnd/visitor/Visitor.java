package frontEnd.visitor;

import frontEnd.*;
import frontEnd.Symbol.SymbolType;
import frontEnd.Symbol._SymbolType1;
import frontEnd.Symbol._SymbolType2;
import frontEnd.lexer.LexType;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

public class Visitor {
    private final ASTNode root;
    private final Stack<SymbolTable> symbolTableStack = new Stack<>();
    private final HashMap<Integer, SymbolTable> allSymbolTables = new HashMap<>();
    private SymbolTable currTable = null;
    private int symbolId = 1;
    private int scopeId = 1;

    public Visitor(ASTNode node) {
        this.root = node;
    }

    private SemanticType basicType(_SymbolType1 type) {
        return type == _SymbolType1.CHAR ? SemanticType.CHAR : SemanticType.INT;
    }

    private SemanticType declaredType(_SymbolType1 type, boolean array) {
        if (array) {
            return type == _SymbolType1.CHAR ? SemanticType.CHAR_ARRAY : SemanticType.INT_ARRAY;
        }
        return basicType(type);
    }

    private int firstLine(ASTNode node) {
        if (node instanceof LeafASTNode leaf) {
            return leaf.token.lineNum;
        }
        for (ASTNode child : node.children) {
            int line = firstLine(child);
            if (line > 0) {
                return line;
            }
        }
        return 1;
    }

    private SemanticType sameOperandType(SemanticType left, SemanticType right, Token operator) {
        if (left == SemanticType.ERROR || right == SemanticType.ERROR) {
            return SemanticType.ERROR;
        }
        if (!left.isScalar() || left != right) {
            Reporter.REPORTER.report(operator.lineNum, ErrorCode.TYPE_MISMATCH);
            return SemanticType.ERROR;
        }
        return left;
    }

    private Symbol getSymbol(Token token) {
        SymbolTable table = currTable;
        while (table != null) {
            if (table.hasSymbol(token.token)) {
                return table.getSymbol(token.token);
            }

            table = table.parentTable;
        }

        if (RuntimeLibrary.contains(token.token)) {
            return RuntimeLibrary.resolve(token);
        }

        return new Symbol(0, 0, token, _SymbolType1.VOID, _SymbolType2.VAR);
    }

    private boolean checkErrorB(Token token) {
        if (currTable.hasSymbol(token.token)) {
            Reporter.REPORTER.report(token.lineNum, ErrorCode.NAME_REDEFINED);
            return false;
        }
        return true;
    }

    private void checkErrorC(Token token) {
        if (RuntimeLibrary.contains(token.token)) {
            return;
        }

        SymbolTable table = currTable;
        while (table != null) {
            if (table.hasSymbol(token.token)) {
                return;
            }

            table = table.parentTable;
        }

        Reporter.REPORTER.report(token.lineNum, ErrorCode.NAME_UNDEFINED);
    }


    public HashMap<Integer, SymbolTable> analyze(String forOutput) {
        visitCompUnit(root);
        ArrayList<Symbol> symbols = new ArrayList<>();
        for (SymbolTable table : allSymbolTables.values()) {
            symbols.addAll(table.getAllSymbols());
        }

        // symbols 排序后输出
        symbols.sort((a, b) -> {
            if (a.tableId != b.tableId) {
                return a.tableId - b.tableId;
            } else
                return a.id - b.id;
        });

        try (FileWriter writer = new FileWriter(forOutput)) {
            for (Symbol symbol : symbols) {
                writer.write(symbol + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
        return allSymbolTables;
    }

    private void enterScope() {
        SymbolTable newTable = new SymbolTable(scopeId++, currTable);
        symbolTableStack.push(newTable);
        allSymbolTables.put(newTable.id, newTable);
        currTable = newTable;
    }

    private void exitScope() {
        symbolTableStack.pop();
        try {
            currTable = symbolTableStack.peek();
        } catch (EmptyStackException e) {
            currTable = null;
        }
    }

    private void visitCompUnit(ASTNode node) {
        // <CompUnit> ::= {<Decl>} {<FuncDef>} <MainFuncDef>
        enterScope();
        ArrayList<ASTNode> children = node.children;
        int i = 0;
        while (i < children.size() && children.get(i).isNode("Decl")) {
            visitDecl(children.get(i));
            i++;
        }
        while (i < children.size() && children.get(i).isNode("FuncDef")) {
            visitFuncDef(children.get(i));
            i++;
        }

        visitMainFuncDef(children.get(i));

        exitScope();
    }
    private void visitMainFuncDef(ASTNode node) {
        // <MainFuncDef> ::= 'int' 'main' '(' ')' <Block>
        enterScope();
        visitBlock(node.children.get(node.children.size() - 1), SemanticType.INT, true, false, false);
        exitScope();
    }

    private void visitDecl(ASTNode node) {
        // <Decl> ::= <ConstDecl> | <VarDecl>
        node = node.children.get(0);
        if (node.isNode("ConstDecl")) {
            visitConstDecl(node);
        } else if (node.isNode("VarDecl")) {
            visitVarDecl(node);
        }
    }


    private void visitConstDecl(ASTNode node) {
        // <ConstDecl> ::= 'const' <BType> <ConstDef> { ',' <ConstDef> } ';'
        // <BType> ::= 'int' | 'char'
        _SymbolType1 symbolType1 = null;
        LeafASTNode leafNode = (LeafASTNode) node.children.get(1).children.get(0);
        if (leafNode.token.isType(LexType.INTTK)) {
            // int
            symbolType1 = _SymbolType1.INT;
        } else if (leafNode.token.isType(LexType.CHARTK)) {
            // char
            symbolType1 = _SymbolType1.CHAR;
        }

        for (ASTNode child : node.children) {
            if (child.name.equals("ConstDef")) {
                visitConstDef(child, symbolType1);
            }
        }
    }
    private void visitConstDef(ASTNode node, _SymbolType1 symbolType1) {
        // <ConstDef> ::= <Ident> '=' <ConstInitVal> | <Ident> '[' <ConstExp> ']' '=' <ConstInitVal>
        Token token = ((LeafASTNode) node.children.get(0)).token;
        _SymbolType2 symbolType2 = _SymbolType2.CONST;
        if (node.children.get(1) instanceof LeafASTNode bracket && bracket.token.isType(LexType.LBRACK)) {
            symbolType2 = _SymbolType2.CONSTARRAY;
            SemanticType lengthType = visitConstExp(node.children.get(2));
            if (lengthType != SemanticType.INT && lengthType != SemanticType.ERROR) {
                Reporter.REPORTER.report(bracket.token.lineNum, ErrorCode.TYPE_MISMATCH);
            }
        }
        if (checkErrorB(token)) {
            currTable.addSymbol(new Symbol(symbolId++, currTable.id, token, symbolType1, symbolType2));
        }

        visitConstInitVal(node.children.get(node.children.size() - 1), declaredType(symbolType1, symbolType2 == _SymbolType2.CONSTARRAY));
    }

    private void visitConstInitVal(ASTNode node, SemanticType declared) {
        // <ConstInitVal> ::= <ConstExp> | '{' [ <ConstExp> { ',' <ConstExp> } ] '}' | StringConst
        SemanticType expected = declared.isArray()
                ? (declared == SemanticType.CHAR_ARRAY ? SemanticType.CHAR : SemanticType.INT)
                : declared;
        boolean mismatch = false;
        for (ASTNode child : node.children) {
            if (child.isNode("ConstExp")) {
                SemanticType actual = visitConstExp(child);
                mismatch |= actual != SemanticType.ERROR && actual != expected;
            } else if (child.isNode("StringConst") && declared != SemanticType.CHAR_ARRAY) {
                mismatch = true;
            }
        }
        if (mismatch) {
            Reporter.REPORTER.report(firstLine(node), ErrorCode.TYPE_MISMATCH);
        }
    }

    private SemanticType visitConstExp(ASTNode node) {
        // <ConstExp> ::= <AddExp>
        return visitAddExp(node.children.get(0));
    }

    private void visitVarDecl(ASTNode node) {
        // <VarDecl> ::= [ 'static' ] <BType> <VarDef> { ',' <VarDef> } ';'
        // <BType> ::= 'int' | 'char'
        _SymbolType1 symbolType1 = null;
        boolean isStatic = node.children.get(0) instanceof LeafASTNode leaf
                && leaf.token.isType(LexType.STATICTK);
        ASTNode bType = node.children.stream().filter(child -> child.isNode("BType")).findFirst().orElseThrow();
        LeafASTNode leafNode = (LeafASTNode) bType.children.get(0);
        if (leafNode.token.isType(LexType.INTTK)) {
            // int
            symbolType1 = _SymbolType1.INT;
        } else if (leafNode.token.isType(LexType.CHARTK)) {
            // char
            symbolType1 = _SymbolType1.CHAR;
        }

        for (ASTNode child : node.children) {
            if (child.name.equals("VarDef")) {
                visitVarDef(child, symbolType1, isStatic);
            }
        }

    }

    private void visitVarDef(ASTNode node, _SymbolType1 symbolType1, boolean isStatic) {
        // <VarDef> ::= <Ident> [ '[' <ConstExp> ']' ] [ '=' <InitVal> ]
        Token token = ((LeafASTNode) node.children.get(0)).token;
        _SymbolType2 symbolType2 = _SymbolType2.VAR;
        if (node.children.size() > 1 && node.children.get(1) instanceof LeafASTNode bracket
                && bracket.token.isType(LexType.LBRACK)) {
            symbolType2 = _SymbolType2.ARRAY;
            SemanticType lengthType = visitConstExp(node.children.get(2));
            if (lengthType != SemanticType.INT && lengthType != SemanticType.ERROR) {
                Reporter.REPORTER.report(bracket.token.lineNum, ErrorCode.TYPE_MISMATCH);
            }
        }
        if (checkErrorB(token)) {
            Symbol symbol = new Symbol(symbolId++, currTable.id, token, symbolType1, symbolType2);
            symbol.setStatic(isStatic);
            currTable.addSymbol(symbol);
        }

        if (node.children.get(node.children.size() - 1).isNode("InitVal")) {
            visitInitVal(node.children.get(node.children.size() - 1), declaredType(symbolType1, symbolType2 == _SymbolType2.ARRAY));
        }
    }

    private void visitInitVal(ASTNode node, SemanticType declared) {
        // <InitVal> ::= <StringConst> | '{' [ <Exp> { ',' <Exp> } ] '}' | <Exp>
        SemanticType expected = declared.isArray()
                ? (declared == SemanticType.CHAR_ARRAY ? SemanticType.CHAR : SemanticType.INT)
                : declared;
        boolean mismatch = false;
        for (ASTNode child : node.children) {
            if (child.isNode("Exp")) {
                SemanticType actual = visitExp(child);
                mismatch |= actual != SemanticType.ERROR && actual != expected;
            } else if (child.isNode("StringConst") && declared != SemanticType.CHAR_ARRAY) {
                mismatch = true;
            }
        }
        if (mismatch) {
            Reporter.REPORTER.report(firstLine(node), ErrorCode.TYPE_MISMATCH);
        }
    }


    private void visitFuncDef(ASTNode node) {
        // <FuncDef> ::= <FuncType> <Ident> '(' [<FuncFParams>] ')' <Block>
        // <FuncType> ::= 'void' | 'int' | 'char'
        Token token = ((LeafASTNode) node.children.get(1)).token;

        boolean checkErrorB = checkErrorB(token);

        _SymbolType2 symbolType2 = _SymbolType2.FUNC;
        _SymbolType1 symbolType1 = null;
        LeafASTNode leafNode = (LeafASTNode) node.children.get(0).children.get(0);
        if (leafNode.token.isType(LexType.VOIDTK)) {
            // void
            symbolType1 = _SymbolType1.VOID;
        } else if (leafNode.token.isType(LexType.INTTK)) {
            // int
            symbolType1 = _SymbolType1.INT;
        } else if (leafNode.token.isType(LexType.CHARTK)) {
            // char
            symbolType1 = _SymbolType1.CHAR;
        }
        Symbol symbol = null;
        if (checkErrorB) {
            symbol = new Symbol(symbolId++, currTable.id, token, symbolType1, symbolType2);
            currTable.addSymbol(symbol);
        }

        SemanticType returnType = symbolType1 == _SymbolType1.VOID
                ? SemanticType.VOID : basicType(symbolType1);
        boolean checkErrorG = returnType != SemanticType.VOID;
        enterScope();
        if (node.children.get(3).isNode("FuncFParams")) {
            ArrayList<Symbol> paras = visitFParams(node.children.get(3));
            if (checkErrorB) {
                symbol.setParams(paras);
            }
        }
        ASTNode blockNode = node.children.get(node.children.size() - 1);
        visitBlock(blockNode, returnType, checkErrorG, false, false);
        exitScope();
    }

    private ArrayList<Symbol> visitFParams(ASTNode node) {
        // FuncFParams ::= <FuncFParam> {',' <FuncFParam>}
        ArrayList<Symbol> paras = new ArrayList<>();
        for (ASTNode child : node.children) {
            if (child.isNode("FuncFParam")) {
                paras.add(visitFuncFParam(child));
            }
        }
        return paras;
    }

    private Symbol visitFuncFParam(ASTNode node) {
        // FuncFParam ::= <BType> <Ident> [ '[' ']' ]
        ArrayList<ASTNode> children = node.children;

        Token token = ((LeafASTNode) children.get(1)).token;
        boolean checkErrorB = checkErrorB(token);

        _SymbolType1 symbolType1 = null;
        LeafASTNode leafNode = (LeafASTNode) children.get(0).children.get(0);
        if (leafNode.token.isType(LexType.INTTK)) {
            // int
            symbolType1 = _SymbolType1.INT;
        } else if (leafNode.token.isType(LexType.CHARTK)) {
            // char
            symbolType1 = _SymbolType1.CHAR;
        }

        Symbol symbol;
        if (children.size() == 2) {
            symbol = new Symbol(symbolId++, currTable.id, token, symbolType1, _SymbolType2.VAR);
            // <BType> <Ident>
        } else {
            // <BType> <Ident> '[' ']'
            symbol = new Symbol(symbolId++, currTable.id, token, symbolType1, _SymbolType2.ARRAY);
        }
        if (checkErrorB) {
            currTable.addSymbol(symbol);
        }

        return symbol;

    }

    private void visitBlock(ASTNode node, SemanticType returnType, boolean checkErrorG,
                            boolean canBreak, boolean canContinue) {
        // <Block> ::= '{' { <BlockItem> } '}'
        boolean isReturn = false;
        for (ASTNode child : node.children) {
            if (child.isNode("BlockItem")) {
                isReturn = visitBlockItem(child, returnType, canBreak, canContinue);
            }
        }
        if (checkErrorG && !isReturn) {
            Token RBrace;
            int i = node.children.size() - 1;
            RBrace = ((LeafASTNode) node.children.get(i)).token;
            Reporter.REPORTER.report(RBrace.lineNum, ErrorCode.MISSING_RETURN);
        }
    }

    /**
     *
     * @param node <BlockItem> 节点
     * @param checkErrorF 是否检查 return 语句
     * @return 是否是 return 语句
     */
    private boolean visitBlockItem(ASTNode node, SemanticType returnType,
                                   boolean canBreak, boolean canContinue) {
        // <BlockItem> ::= <Stmt> | <Decl>
        ASTNode child = node.children.get(0);
        if (child.isNode("Stmt")) {
            return visitStmt(child, returnType, canBreak, canContinue);
        } else {
            visitDecl(child);
            return false;
        }
    }

    private boolean visitStmt(ASTNode node, SemanticType returnType,
                              boolean canBreak, boolean canContinue) {
        /*
           <Stmt> ::= <Block>
                    | 'if' '(' <Cond> ')' <Stmt> [ 'else' <Stmt> ]
                    | 'while' '(' <Cond> ')' <Stmt>
                    | 'switch' '(' <Exp> ')' '{' { <CaseStmt> } '}'
                    | 'break' ';'
                    | 'continue' ';'
                    | 'return' [ <Exp> ] ';'
                    | 'printf' '(' <StringConst> { ',' Exp } ')' ';'
                    | <LVal> '=' <Exp> ';'
                    | [Exp] ';'
         */
        ArrayList<ASTNode> children = node.children;
        if (children.get(0).isNode("Block")) {
            enterScope();
            visitBlock(children.get(0), returnType, false, canBreak, canContinue);
            exitScope();
        } else if (children.get(0).isNode("LEAF")) {
            Token token = ((LeafASTNode) children.get(0)).token;
            if (token.isType(LexType.IFTK)) {
                int i = 2;
                visitCond(children.get(i++));
                while (i < children.size()) {
                    if (children.get(i).isNode("Stmt")) {
                        visitStmt(children.get(i), returnType, canBreak, canContinue);
                    }
                    i++;
                }
            } else if (token.isType(LexType.WHILETK)) {
                visitCond(children.get(2));
                visitStmt(children.get(children.size() - 1), returnType, true, true);
            } else if (token.isType(LexType.SWITCHTK)) {
                visitSwitchStmt(children, returnType, canContinue);
            } else if (token.isType(LexType.BREAKTK)) {
                if (!canBreak) {
                    Reporter.REPORTER.report(token.lineNum, ErrorCode.INVALID_CONTROL_FLOW);
                }
            } else if (token.isType(LexType.CONTINUETK)) {
                if (!canContinue) {
                    Reporter.REPORTER.report(token.lineNum, ErrorCode.INVALID_CONTROL_FLOW);
                }
            } else if (token.isType(LexType.RETURNTK)) {
                if (1 < children.size() && children.get(1).isNode("Exp")) {
                    SemanticType actual = visitExp(children.get(1));
                    if (returnType == SemanticType.VOID) {
                        Reporter.REPORTER.report(token.lineNum, ErrorCode.VOID_FUNCTION_RETURNS_VALUE);
                    } else if (actual != SemanticType.ERROR && actual != returnType) {
                        Reporter.REPORTER.report(token.lineNum, ErrorCode.TYPE_MISMATCH);
                    }
                } else if (returnType != SemanticType.VOID) {
                    Reporter.REPORTER.report(token.lineNum, ErrorCode.TYPE_MISMATCH);
                }
                return true;
            } else if (token.isType(LexType.PRINTFTK)) {
                ArrayList<SemanticType> expected = new ArrayList<>();
                ArrayList<SemanticType> actual = new ArrayList<>();
                String str = ((LeafASTNode) children.get(2).children.get(0)).token.token;
                // 找到 %d 或 %c
                for (int i = 0; i < str.length(); i++) {
                    if (str.charAt(i) == '%' && i + 1 < str.length()) {
                        if (str.charAt(i + 1) == 'd') {
                            expected.add(SemanticType.INT);
                        } else if (str.charAt(i + 1) == 'c') {
                            expected.add(SemanticType.CHAR);
                        } else if (str.charAt(i + 1) == 's') {
                            expected.add(SemanticType.CHAR_ARRAY);
                        }
                    }
                }

                for (ASTNode child : children) {
                    if (child.isNode("Exp")) {
                        actual.add(visitExp(child));
                    }
                }

                if (expected.size() != actual.size()) {
                    Reporter.REPORTER.report(token.lineNum, ErrorCode.PRINTF_ARGUMENT_COUNT_MISMATCH);
                } else {
                    for (int i = 0; i < expected.size(); i++) {
                        if (actual.get(i) != SemanticType.ERROR && expected.get(i) != actual.get(i)) {
                            Reporter.REPORTER.report(token.lineNum, ErrorCode.TYPE_MISMATCH);
                            break;
                        }
                    }
                }
            }
        } else if (children.get(0).isNode("LVal")) {
            SemanticType leftType = visitLVal(children.get(0));
            ASTNode ident = children.get(0).children.get(0);
            SymbolType symbolType = getSymbol(((LeafASTNode) ident).token).symbolType;

            if (isConst(symbolType)) {
                Reporter.REPORTER.report(((LeafASTNode) ident).token.lineNum, ErrorCode.CONST_ASSIGNMENT);
            }

            if (children.get(2).isNode("Exp")) {
                SemanticType rightType = visitExp(children.get(2));
                if (leftType != SemanticType.ERROR && rightType != SemanticType.ERROR && leftType != rightType) {
                    Token assign = ((LeafASTNode) children.get(1)).token;
                    Reporter.REPORTER.report(assign.lineNum, ErrorCode.TYPE_MISMATCH);
                }
            }
        } else if (children.get(0).isNode("Exp")) {
            visitExp(children.get(0));
        }
        return false;
    }

    private boolean isConst(SymbolType symbolType) {
        return switch (symbolType) {
            case ConstInt, ConstChar, ConstIntArray, ConstCharArray -> true;
            default -> false;
        };
    }

    private SemanticType visitCond(ASTNode node) {
        // <Cond> ::= <LOrExp>
        return visitLOrExp(node.children.get(0));
    }

    private void visitSwitchStmt(ArrayList<ASTNode> children, SemanticType returnType,
                                 boolean canContinue) {
        Token switchToken = ((LeafASTNode) children.get(0)).token;
        SemanticType switchType = visitExp(children.get(2));
        if (switchType != SemanticType.ERROR && !switchType.isScalar()) {
            Reporter.REPORTER.report(switchToken.lineNum, ErrorCode.TYPE_MISMATCH);
        }

        HashSet<Integer> caseValues = new HashSet<>();
        boolean hasDefault = false;
        for (ASTNode child : children) {
            if (!child.isNode("CaseStmt")) {
                continue;
            }
            Token label = ((LeafASTNode) child.children.get(0)).token;
            if (label.isType(LexType.DEFAULTTK)) {
                if (hasDefault) {
                    Reporter.REPORTER.report(label.lineNum, ErrorCode.DUPLICATED_CASE);
                }
                hasDefault = true;
            } else {
                ASTNode number = child.children.get(1);
                Token literal = ((LeafASTNode) number.children.get(0)).token;
                int value = literalValue(literal);
                if (!caseValues.add(value)) {
                    Reporter.REPORTER.report(label.lineNum, ErrorCode.DUPLICATED_CASE);
                } else {
                    SemanticType caseType = literal.isType(LexType.CHRCON)
                            ? SemanticType.CHAR : SemanticType.INT;
                    if (switchType != SemanticType.ERROR && switchType != caseType) {
                        Reporter.REPORTER.report(label.lineNum, ErrorCode.TYPE_MISMATCH);
                    }
                }
            }
            for (ASTNode statement : child.children) {
                if (statement.isNode("Stmt")) {
                    visitStmt(statement, returnType, true, canContinue);
                }
            }
        }
    }

    private int literalValue(Token token) {
        if (token.isType(LexType.INTCON)) {
            return Integer.parseInt(token.token);
        }
        if (token.token.charAt(1) != '\\') {
            return token.token.charAt(1);
        }
        return switch (token.token.charAt(2)) {
            case '0' -> 0;
            case 'a' -> 7;
            case 'b' -> 8;
            case 't' -> 9;
            case 'n' -> 10;
            case 'v' -> 11;
            case 'f' -> 12;
            case '"' -> 34;
            case '\'' -> 39;
            case '\\' -> 92;
            default -> token.token.charAt(2);
        };
    }

    private SemanticType visitLOrExp(ASTNode node) {
        // <LOrExp> ::= <LOrExp> '||' <LAndExp>  | <LAndExp>
        if (node.children.size() == 1) {
            return visitLAndExp(node.children.get(0));
        } else {
            SemanticType left = visitLOrExp(node.children.get(0));
            SemanticType right = visitLAndExp(node.children.get(2));
            sameOperandType(left, right, ((LeafASTNode) node.children.get(1)).token);
            return SemanticType.INT;
        }
    }

    private SemanticType visitLAndExp(ASTNode node) {
        // <LAndExp> ::= <LAndExp> '&&' <EqExp>  | <EqExp>
        if (node.children.size() == 1) {
            return visitEqExp(node.children.get(0));
        } else {
            SemanticType left = visitLAndExp(node.children.get(0));
            SemanticType right = visitEqExp(node.children.get(2));
            sameOperandType(left, right, ((LeafASTNode) node.children.get(1)).token);
            return SemanticType.INT;
        }
    }

    private SemanticType visitEqExp(ASTNode node) {
        // <EqExp> ::= <EqExp> ('==' | '!=') <RelExp> | <RelExp>
        if (node.children.size() == 1) {
            return visitRelExp(node.children.get(0));
        } else {
            SemanticType left = visitEqExp(node.children.get(0));
            SemanticType right = visitRelExp(node.children.get(2));
            sameOperandType(left, right, ((LeafASTNode) node.children.get(1)).token);
            return SemanticType.INT;
        }
    }

    private SemanticType visitRelExp(ASTNode node) {
        // <RelExp> ::= <RelExp> ('<' | '<=' | '>' | '>=' ) <AddExp> | <AddExp>
        if (node.children.size() == 1) {
            return visitAddExp(node.children.get(0));
        } else {
            SemanticType left = visitRelExp(node.children.get(0));
            SemanticType right = visitAddExp(node.children.get(2));
            sameOperandType(left, right, ((LeafASTNode) node.children.get(1)).token);
            return SemanticType.INT;
        }
    }

    private SemanticType visitAddExp(ASTNode node) {
        // <AddExp> ::= <AddExp> <AddOp> <MulExp> | <MulExp>
        if (node.children.size() == 1) {
            return visitMulExp(node.children.get(0));
        } else {
            SemanticType left = visitAddExp(node.children.get(0));
            SemanticType right = visitMulExp(node.children.get(2));
            return sameOperandType(left, right, ((LeafASTNode) node.children.get(1)).token);
        }
    }

    private SemanticType visitMulExp(ASTNode node) {
        // <MulExp> ::= <MulExp> <MulOp> <UnaryExp> | <UnaryExp>
        if (node.children.size() == 1) {
            return visitUnaryExp(node.children.get(0));
        } else {
            SemanticType left = visitMulExp(node.children.get(0));
            SemanticType right = visitUnaryExp(node.children.get(2));
            return sameOperandType(left, right, ((LeafASTNode) node.children.get(1)).token);
        }
    }

    private SemanticType visitUnaryExp(ASTNode node) {
        // <UnaryExp> ::= <PrimaryExp> | <UnaryOp> <UnaryExp> | <Ident> '(' [<FuncRParams>] ')' | '(' <BType> ')' <UnaryExp>
        if (node.children.get(0).isNode("PrimaryExp")) {
            return visitPrimaryExp(node.children.get(0));
        } else if (node.children.get(0).isNode("UnaryOp")) {
            SemanticType operand = visitUnaryExp(node.children.get(1));
            Token operator = ((LeafASTNode) node.children.get(0).children.get(0)).token;
            if (operand != SemanticType.ERROR && !operand.isScalar()) {
                Reporter.REPORTER.report(operator.lineNum, ErrorCode.TYPE_MISMATCH);
                return SemanticType.ERROR;
            }
            return operator.isType(LexType.NOT) ? SemanticType.INT : operand;
        } else if (node.children.get(0).isNode("LEAF")) {
            Token token = ((LeafASTNode) node.children.get(0)).token;

            if (token.isType(LexType.LPARENT)) {
                LeafASTNode target = (LeafASTNode) node.children.get(1).children.get(0);
                visitUnaryExp(node.children.get(node.children.size() - 1));
                return target.token.isType(LexType.CHARTK) ? SemanticType.CHAR : SemanticType.INT;
            }

            checkErrorC(token);
            Symbol function = getSymbol(token);

            ArrayList<SemanticType> paras;
            if (2 < node.children.size() && node.children.get(2).isNode("FuncRParams")) {
                paras = visitFuncRParams(node.children.get(2));
            } else {
                paras = new ArrayList<>();
            }

            if (function.symbolType != SymbolType.ERROR && !SemanticType.isFunction(function.symbolType)) {
                Reporter.REPORTER.report(token.lineNum, ErrorCode.TYPE_MISMATCH);
                return SemanticType.ERROR;
            }
            if (function.symbolType == SymbolType.ERROR) {
                return SemanticType.ERROR;
            }

            if (paras.size() != function.params.size()) {
                Reporter.REPORTER.report(token.lineNum, ErrorCode.FUNCTION_ARGUMENT_COUNT_MISMATCH);
            } else {
                boolean flag = false;
                for (int i = 0; i < paras.size(); i++) {
                    SemanticType formal = SemanticType.fromSymbol(function.params.get(i).symbolType);
                    if (paras.get(i) != SemanticType.ERROR && formal != paras.get(i)) {
                        flag = true;
                    }
                }
                if (flag) {
                    Reporter.REPORTER.report(token.lineNum, ErrorCode.TYPE_MISMATCH);
                }
            }

            return SemanticType.fromSymbol(function.symbolType);
        }
        return SemanticType.ERROR;
    }

    private ArrayList<SemanticType> visitFuncRParams(ASTNode node) {
        // FuncRParams ::= <Exp> {',' <Exp>}
        ArrayList<SemanticType> paras = new ArrayList<>();
        for (ASTNode child : node.children) {
            if (child.isNode("Exp")) {
                paras.add(visitExp(child));
            }
        }
        return paras;
    }

    private SemanticType visitPrimaryExp(ASTNode node) {
        // <PrimaryExp> ::= '(' <Exp> ')' | <LVal> | <Number>
        if (node.children.size() > 1) {
            return visitExp(node.children.get(1));
        } else if (node.children.get(0).isNode("LVal")) {
            return visitLVal(node.children.get(0));
        } else if (node.children.get(0).isNode("Number")) {
            Token number = ((LeafASTNode) node.children.get(0).children.get(0)).token;
            return number.isType(LexType.CHRCON) ? SemanticType.CHAR : SemanticType.INT;
        }
        return SemanticType.ERROR;
    }

    private SemanticType visitLVal(ASTNode node) {
        // <LVal> ::= <Ident> [ '[' <Exp> ']' ]
        Token token = ((LeafASTNode) node.children.get(0)).token;
        checkErrorC(token);

        Symbol symbol = getSymbol(token);
        if (symbol.symbolType == SymbolType.ERROR) {
            return SemanticType.ERROR;
        }
        if (SemanticType.isFunction(symbol.symbolType)) {
            Reporter.REPORTER.report(token.lineNum, ErrorCode.TYPE_MISMATCH);
            return SemanticType.ERROR;
        }

        SemanticType base = SemanticType.fromSymbol(symbol.symbolType);
        if (node.children.size() == 1) {
            return base;
        } else {
            Token bracket = ((LeafASTNode) node.children.get(1)).token;
            SemanticType index = visitExp(node.children.get(2));
            if (!base.isArray()) {
                Reporter.REPORTER.report(bracket.lineNum, ErrorCode.TYPE_MISMATCH);
                return SemanticType.ERROR;
            }
            if (index != SemanticType.ERROR && index != SemanticType.INT) {
                Reporter.REPORTER.report(bracket.lineNum, ErrorCode.TYPE_MISMATCH);
            }
            return base == SemanticType.CHAR_ARRAY ? SemanticType.CHAR : SemanticType.INT;
        }

    }

    private SemanticType visitExp(ASTNode node) {
        // <Exp> ::= <AddExp>
        return visitAddExp(node.children.get(0));
    }
}
