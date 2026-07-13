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

    private Symbol getSymbol(Token token) {
        SymbolTable table = currTable;
        while (table != null) {
            if (table.hasSymbol(token.token)) {
                return table.getSymbol(token.token);
            }

            table = table.parentTable;
        }

        return new Symbol(0, 0, token, _SymbolType1.VOID, _SymbolType2.VAR);
    }

    private boolean checkErrorB(Token token) {
        if (currTable.hasSymbol(token.token)) {
            Reporter.REPORTER.add(new MyError(token.lineNum, "b"));
            return false;
        }
        return true;
    }

    private void checkErrorC(Token token) {
        SymbolTable table = currTable;
        while (table != null) {
            if (table.hasSymbol(token.token)) {
                return;
            }

            table = table.parentTable;
        }

        Reporter.REPORTER.add(new MyError(token.lineNum, "c"));
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
        visitBlock(node.children.get(node.children.size() - 1), false, true, false);
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
        if (checkErrorB(token)) {
            _SymbolType2 symbolType2 = _SymbolType2.CONST;
            if (node.children.get(1).isNode("LEAF")) {
                ASTNode child = node.children.get(1);
                if (((LeafASTNode) child).token.isType(LexType.LBRACK)) {
                    symbolType2 = _SymbolType2.CONSTARRAY;
                    visitConstExp(node.children.get(2));
                }
            }
            currTable.addSymbol(new Symbol(symbolId++, currTable.id, token, symbolType1, symbolType2));
        }

        visitConstInitVal(node.children.get(node.children.size() - 1));
    }

    private void visitConstInitVal(ASTNode node) {
        // <ConstInitVal> ::= <ConstExp> | '{' [ <ConstExp> { ',' <ConstExp> } ] '}' | StringConst
        for (ASTNode child : node.children) {
            if (child.isNode("ConstExp")) {
                visitConstExp(child);
            }
        }
    }

    private void visitConstExp(ASTNode node) {
        // <ConstExp> ::= <AddExp>
        visitAddExp(node.children.get(0));
    }

    private void visitVarDecl(ASTNode node) {
        // <VarDecl> ::= <BType> <VarDef> { ',' <VarDef> } ';'
        // <BType> ::= 'int' | 'char'
        _SymbolType1 symbolType1 = null;
        LeafASTNode leafNode = (LeafASTNode) node.children.get(0).children.get(0);
        if (leafNode.token.isType(LexType.INTTK)) {
            // int
            symbolType1 = _SymbolType1.INT;
        } else if (leafNode.token.isType(LexType.CHARTK)) {
            // char
            symbolType1 = _SymbolType1.CHAR;
        }

        for (ASTNode child : node.children) {
            if (child.name.equals("VarDef")) {
                visitVarDef(child, symbolType1);
            }
        }

    }

    private void visitVarDef(ASTNode node, _SymbolType1 symbolType1) {
        // <VarDef> ::= <Ident> [ '[' <ConstExp> ']' ] [ '=' <InitVal> ]
        Token token = ((LeafASTNode) node.children.get(0)).token;
        if (checkErrorB(token)) {
            _SymbolType2 symbolType2 = _SymbolType2.VAR;
            if (node.children.size() > 1 && node.children.get(1).isNode("LEAF")) {
                ASTNode child = node.children.get(1);
                if (((LeafASTNode) child).token.isType(LexType.LBRACK)) {
                    symbolType2 = _SymbolType2.ARRAY;
                    visitConstExp(node.children.get(2));
                }
            }

            currTable.addSymbol(new Symbol(symbolId++, currTable.id, token, symbolType1, symbolType2));
        }

        if (node.children.get(node.children.size() - 1).isNode("InitVal")) {
            visitInitVal(node.children.get(node.children.size() - 1));
        }
    }

    private void visitInitVal(ASTNode node) {
        // <InitVal> ::= <StringConst> | '{' [ <Exp> { ',' <Exp> } ] '}' | <Exp>
        for (ASTNode child : node.children) {
            if (child.isNode("Exp")) {
                visitExp(child);
            }
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

        boolean checkErrorF = false;
        boolean checkErrorG = false;
        if (symbolType1 == _SymbolType1.VOID) {
            checkErrorF = true;
        } else {
            checkErrorG = true;
        }
        enterScope();
        if (node.children.get(3).isNode("FuncFParams")) {
            ArrayList<Symbol> paras = visitFParams(node.children.get(3));
            if (checkErrorB) {
                symbol.setParams(paras);
            }
        }
        ASTNode blockNode = node.children.get(node.children.size() - 1);
        visitBlock(blockNode, checkErrorF, checkErrorG, false);
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

    private void visitBlock(ASTNode node, boolean checkErrorF, boolean checkErrorG, boolean inLoop) {
        // <Block> ::= '{' { <BlockItem> } '}'
        boolean isReturn = false;
        for (ASTNode child : node.children) {
            if (child.isNode("BlockItem")) {
                isReturn = visitBlockItem(child, checkErrorF, inLoop);
            }
        }
        if (checkErrorG && !isReturn) {
            Token RBrace;
            int i = node.children.size() - 1;
            RBrace = ((LeafASTNode) node.children.get(i)).token;
            Reporter.REPORTER.add(new MyError(RBrace.lineNum, "g"));
        }
    }

    /**
     *
     * @param node <BlockItem> 节点
     * @param checkErrorF 是否检查 return 语句
     * @return 是否是 return 语句
     */
    private boolean visitBlockItem(ASTNode node, boolean checkErrorF, boolean inLoop) {
        // <BlockItem> ::= <Stmt> | <Decl>
        ASTNode child = node.children.get(0);
        if (child.isNode("Stmt")) {
            return visitStmt(child, checkErrorF, inLoop);
        } else {
            visitDecl(child);
            return false;
        }
    }

    private boolean visitStmt(ASTNode node, boolean checkErrorF, boolean inLoop) {
        /*
           <Stmt> ::= <Block>
                    | 'if' '(' <Cond> ')' <Stmt> [ 'else' <Stmt> ]
                    | 'for' '(' [ <ForStmt> ] ';' [ <Cond> ] ';' [ <ForStmt> ] ')' <Stmt>
                    | 'break' ';'
                    | 'continue' ';'
                    | 'return' [ <Exp> ] ';'
                    | 'printf' '(' <StringConst> { ',' Exp } ')' ';'
                    | <LVal> '=' 'getint' '(' ')' ';'
                    | <LVal> '=' 'getchar' '(' ')' ';'
                    | <LVal> '=' <Exp> ';'
                    | [Exp] ';'
         */
        ArrayList<ASTNode> children = node.children;
        if (children.get(0).isNode("Block")) {
            enterScope();
            visitBlock(children.get(0), checkErrorF, false, inLoop);
            exitScope();
        } else if (children.get(0).isNode("LEAF")) {
            Token token = ((LeafASTNode) children.get(0)).token;
            if (token.isType(LexType.IFTK)) {
                int i = 2;
                visitCond(children.get(i++));
                while (i < children.size()) {
                    if (children.get(i).isNode("Stmt")) {
                        visitStmt(children.get(i), checkErrorF, inLoop);
                    }
                    i++;
                }
            } else if (token.isType(LexType.FORTK)) {
                int i = 2;
                if (i < children.size() && children.get(i).isNode("ForStmt")) {
                    visitForStmt(children.get(i));
                    i++;
                }
                i++;
                if (i < children.size() && children.get(i).isNode("Cond")) {
                    visitCond(children.get(i));
                    i++;
                }
                i++;
                if (i < children.size() && children.get(i).isNode("ForStmt")) {
                    visitForStmt(children.get(i));
                    i++;
                }
                i++;
                visitStmt(children.get(i), checkErrorF, true);
            } else if (token.isType(LexType.BREAKTK) || token.isType(LexType.CONTINUETK)) {
                if (!inLoop) {
                    Reporter.REPORTER.add(new MyError(token.lineNum, "m"));
                }
            } else if (token.isType(LexType.RETURNTK)) {
                if (1 < children.size() && children.get(1).isNode("Exp")) {
                    visitExp(children.get(1));
                    if (checkErrorF) {
                        Reporter.REPORTER.add(new MyError(token.lineNum, "f"));
                    }
                }
                return true;
            } else if (token.isType(LexType.PRINTFTK)) {
                int cnt1 = 0, cnt2 = 0;
                String str = ((LeafASTNode) children.get(2).children.get(0)).token.token;
                // 找到 %d 或 %c
                for (int i = 0; i < str.length(); i++) {
                    if (str.charAt(i) == '%' && i + 1 < str.length()) {
                        if (str.charAt(i + 1) == 'd' || str.charAt(i + 1) == 'c') {
                            cnt1++;
                        }
                    }
                }

                for (ASTNode child : children) {
                    if (child.isNode("Exp")) {
                        visitExp(child);
                        cnt2++;
                    }
                }

                if (cnt1 != cnt2) {
                    Reporter.REPORTER.add(new MyError(token.lineNum, "l"));
                }
            }
        } else if (children.get(0).isNode("LVal")) {
            SymbolType symbolType = visitLVal(children.get(0));
            ASTNode ident = children.get(0).children.get(0);

            if (isConst(symbolType)) {
                Reporter.REPORTER.add(new MyError(((LeafASTNode) ident).token.lineNum, "h"));
            }

            if (children.get(2).isNode("Exp")) {
                visitExp(children.get(2));
            }
        } else if (children.get(0).isNode("Exp")) {
            visitExp(children.get(0));
        }
        return false;
    }

    private boolean isConst(SymbolType symbolType) {
        return switch (symbolType) {
            case ConstInt, ConstChar -> true;
            default -> false;
        };
    }

    private void visitForStmt(ASTNode node) {
        // <ForStmt> ::=  <LVal> '=' <Exp>
        SymbolType symbolType = visitLVal(node.children.get(0));
        ASTNode ident = node.children.get(0).children.get(0);

        if (isConst(symbolType)) {
            Reporter.REPORTER.add(new MyError(((LeafASTNode) ident).token.lineNum, "h"));
        }

        visitExp(node.children.get(2));
    }

    private void visitCond(ASTNode node) {
        // <Cond> ::= <LOrExp>
        visitLOrExp(node.children.get(0));
    }

    private void visitLOrExp(ASTNode node) {
        // <LOrExp> ::= <LOrExp> '||' <LAndExp>  | <LAndExp>
        if (node.children.size() == 1) {
            visitLAndExp(node.children.get(0));
        } else {
            visitLOrExp(node.children.get(0));
            visitLAndExp(node.children.get(2));
        }
    }

    private void visitLAndExp(ASTNode node) {
        // <LAndExp> ::= <LAndExp> '&&' <EqExp>  | <EqExp>
        if (node.children.size() == 1) {
            visitEqExp(node.children.get(0));
        } else {
            visitLAndExp(node.children.get(0));
            visitEqExp(node.children.get(2));
        }
    }

    private void visitEqExp(ASTNode node) {
        // <EqExp> ::= <EqExp> ('==' | '!=') <RelExp> | <RelExp>
        if (node.children.size() == 1) {
            visitRelExp(node.children.get(0));
        } else {
            visitEqExp(node.children.get(0));
            visitRelExp(node.children.get(2));
        }
    }

    private void visitRelExp(ASTNode node) {
        // <RelExp> ::= <RelExp> ('<' | '<=' | '>' | '>=' ) <AddExp> | <AddExp>
        if (node.children.size() == 1) {
            visitAddExp(node.children.get(0));
        } else {
            visitRelExp(node.children.get(0));
            visitAddExp(node.children.get(2));
        }
    }

    private SymbolType visitAddExp(ASTNode node) {
        // <AddExp> ::= <AddExp> <AddOp> <MulExp> | <MulExp>
        if (node.children.size() == 1) {
            return visitMulExp(node.children.get(0));
        } else {
            visitAddExp(node.children.get(0));
            visitMulExp(node.children.get(2));
            return SymbolType.Int;
        }
    }

    private SymbolType visitMulExp(ASTNode node) {
        // <MulExp> ::= <MulExp> <MulOp> <UnaryExp> | <UnaryExp>
        if (node.children.size() == 1) {
            return visitUnaryExp(node.children.get(0));
        } else {
            visitMulExp(node.children.get(0));
            visitUnaryExp(node.children.get(2));
            return SymbolType.Int;
        }
    }

    private SymbolType visitUnaryExp(ASTNode node) {
        // <UnaryExp> ::= <PrimaryExp> | <UnaryOp> <UnaryExp> | <Ident> '(' [<FuncRParams>] ')'
        if (node.children.get(0).isNode("PrimaryExp")) {
            return visitPrimaryExp(node.children.get(0));
        } else if (node.children.get(0).isNode("UnaryOp")) {
            return visitUnaryExp(node.children.get(1));
        } else if (node.children.get(0).isNode("LEAF")) {
            Token token = ((LeafASTNode) node.children.get(0)).token;

            checkErrorC(token);

            ArrayList<SymbolType> paras;
            if (2 < node.children.size() && node.children.get(2).isNode("FuncRParams")) {
                paras = visitFuncRParams(node.children.get(2));
            } else {
                paras = new ArrayList<>();
            }

            if (paras.size() != getSymbol(token).params.size()) {
                Reporter.REPORTER.add(new MyError(token.lineNum, "d"));
            } else {
                boolean flag = false;
                for (int i = 0; i < paras.size(); i++) {
                    if (!canAccept(getSymbol(token).params.get(i).symbolType, paras.get(i))) {
                        flag = true;
                    }
                }
                if (flag) {
                    Reporter.REPORTER.add(new MyError(token.lineNum, "e"));
                }
            }

            return getSymbol(token).symbolType;
        }
        return SymbolType.ERROR;
    }

    private boolean isArray(SymbolType symbolType) {
        return switch (symbolType) {
            case IntArray, ConstIntArray, CharArray, ConstCharArray -> true;
            default -> false;
        };
    }

    private boolean canAccept(SymbolType formal, SymbolType real) {
        if (isArray(formal) != isArray(real)) {
            return false;
        }

        if (formal == SymbolType.CharArray || formal == SymbolType.ConstCharArray) {
            return real == SymbolType.CharArray || real == SymbolType.ConstCharArray;
        }

        if (formal == SymbolType.IntArray || formal == SymbolType.ConstIntArray) {
            return real == SymbolType.IntArray || real == SymbolType.ConstIntArray;
        }

        return true;
    }

    private ArrayList<SymbolType> visitFuncRParams(ASTNode node) {
        // FuncRParams ::= <Exp> {',' <Exp>}
        ArrayList<SymbolType> paras = new ArrayList<>();
        for (ASTNode child : node.children) {
            if (child.isNode("Exp")) {
                paras.add(visitExp(child));
            }
        }
        return paras;
    }

    private SymbolType visitPrimaryExp(ASTNode node) {
        // <PrimaryExp> ::= '(' <Exp> ')' | <LVal> | <Number> | <Character>
        if (node.children.size() > 1) {
            return visitExp(node.children.get(1));
        } else if (node.children.get(0).isNode("LVal")) {
            return visitLVal(node.children.get(0));
        } else if (node.children.get(0).isNode("Number")) {
            return SymbolType.ConstInt;
        } else if (node.children.get(0).isNode("Character")) {
            return SymbolType.ConstChar;
        }
        return SymbolType.ERROR;
    }

    private SymbolType visitLVal(ASTNode node) {
        // <LVal> ::= <Ident> [ '[' <Exp> ']' ]
        Token token = ((LeafASTNode) node.children.get(0)).token;
        checkErrorC(token);

        if (node.children.size() == 1) {
            return getSymbol(token).symbolType;
        } else {
            visitExp(node.children.get(2));
            String origin = String.valueOf(getSymbol(token).symbolType);
            origin = origin.substring(0, origin.length() - 5);
            return SymbolType.valueOf(origin);
        }

    }

    private SymbolType visitExp(ASTNode node) {
        // <Exp> ::= <AddExp>
        return visitAddExp(node.children.get(0));
    }
}
