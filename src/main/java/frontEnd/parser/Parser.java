package frontEnd.parser;

import frontEnd.*;
import frontEnd.lexer.LexType;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Parser {
    private final ArrayList<Token> tokens;
    private int index = 0;
    private final StringBuilder sb = new StringBuilder();
    private final boolean OUTPUT = true;
    public Parser(ArrayList<Token> tokens) {
        this.tokens = tokens;
    }

    private void checkSemicn() {
        if (!curToken().isType(LexType.SEMICN)) {
            Reporter.REPORTER.add(new MyError(tokenWithOffset(-1).lineNum, "i"));
        }
    }

    private void checkRparent() {
        if (!curToken().isType(LexType.RPARENT)) {
            Reporter.REPORTER.add(new MyError(tokenWithOffset(-1).lineNum, "j"));
        }
    }

    private void checkRbrack() {
        if (!curToken().isType(LexType.RBRACK)) {
            Reporter.REPORTER.add(new MyError(tokenWithOffset(-1).lineNum, "k"));
        }
    }

    private boolean isActuallyEmpty(ASTNode node) {
        while (!(node instanceof LeafASTNode)) {
            if (!node.children.isEmpty()) {
                node = node.children.get(0);
            } else {
                return true;
            }
        }
        return false;
    }

    public ASTNode analyze(String forOutput) {
        ASTNode root = parseCompUnit();
        try (FileWriter writer = new FileWriter(forOutput)) {
            writer.write(sb.toString());
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
        return root;
    }

    private Token curToken() {
        return tokens.get(index);
    }

    private Token tokenWithOffset(int offset) {
        return tokens.get(index + offset);
    }

    private void nextToken() {
        index++;
    }

    private boolean isReachable(int offset) {
        return index + offset < tokens.size();
    }

    private ASTNode parseCompUnit() {
        // <CompUnit> ::= {<Decl>} {<FuncDef>} <MainFuncDef>
        ASTNode node = new ASTNode("CompUnit");
        // {<Decl>}
        while (isDecl()) {
            node.addChild(parseDecl());
        }
        // {<FuncDef>}
        while (isFuncDef()) {
            node.addChild(parseFuncDef());
        }
        // <MainFuncDef>
        node.addChild(parseMainFuncDef());

        if (OUTPUT) {
            sb.append(node.print()).append("\n");
        }
        return node;
    }

    private boolean isDecl() {
        // 用于区分 <Decl> <Stmt> <FuncDef>
        // <Decl> ::= <ConstDecl> | <VarDecl>
        // FIRST(ConstDecl) = {'const'}
        // FIRST(VarDecl) = {'int', 'char'}
        // FIRST(Stmt) 中 不含以上两种
        // FIRST(FuncDef) = {'int', 'char', 'void'}
        if (curToken().isType(LexType.CONSTTK))
            return true;

        if (!curToken().isType(LexType.INTTK) && !curToken().isType(LexType.CHARTK))
            return false;

        if (curToken().isType(LexType.VOIDTK))
            return false;

        // 区分 VarDecl 和 FuncDef
        return isReachable(2) && !tokenWithOffset(2).isType(LexType.LPARENT);
    }

    private ASTNode parseDecl() {
        // <Decl> ::= <ConstDecl> | <VarDecl>
        ASTNode node = new ASTNode("Decl");
        if (curToken().isType(LexType.CONSTTK)) {
            node.addChild(parseConstDecl());
        } else {
            node.addChild(parseVarDecl());
        }

        return node;
    }

    private boolean isFuncDef() {
        // 用于区分 <Decl> <FuncDef> <MainFuncDef>
        if (curToken().isType(LexType.VOIDTK))
            return true;

        if (curToken().isType(LexType.CHARTK))
            return isReachable(2) && tokenWithOffset(2).isType(LexType.LPARENT);

        if (curToken().isType(LexType.INTTK))
            // 区分 <FuncDef> <MainFuncDef>
            return isReachable(1) && !tokenWithOffset(1).isType(LexType.MAINTK);

        return false;
    }

    private ASTNode parseFuncDef() {
        // <FuncDef> ::= <FuncType> <Ident> '(' [<FuncFParams>] ')' <Block>
        ASTNode node = new ASTNode("FuncDef");
        node.addChild(parseFuncType());
        node.addChild(parseIdent());
        node.addChild(parseTokenType(LexType.LPARENT));
        if (!curToken().isType(LexType.RPARENT) && !curToken().isType(LexType.LBRACE)) {
            node.addChild(parseFuncFParams());
        }
        checkRparent();
        node.addChild(parseTokenType(LexType.RPARENT));
        node.addChild(parseBlock());

        if (OUTPUT) {
            sb.append(node.print()).append("\n");
        }
        return node;
    }

    private ASTNode parseFuncType() {
        // <FuncType> ::= 'void' | 'int' | 'char'
        ASTNode node = new ASTNode("FuncType");
        if (curToken().isType(LexType.VOIDTK) || curToken().isType(LexType.INTTK) || curToken().isType(LexType.CHARTK)) {
            node.addChild(parseTokenType(curToken().type));
            if (OUTPUT) {
                sb.append(node.print()).append("\n");
            }
            return node;
        }
        return null;
    }

    private ASTNode parseBlock() {
        // <Block> ::= '{' { <BlockItem> } '}'
        ASTNode node = new ASTNode("Block");
        node.addChild(parseTokenType(LexType.LBRACE));
        while (!curToken().isType(LexType.RBRACE)) {
            node.addChild(parseBlockItem());
        }
        node.addChild(parseTokenType(LexType.RBRACE));

        if (OUTPUT) {
            sb.append(node.print()).append("\n");
        }
        return node;
    }

    private ASTNode parseBlockItem() {
        // <BlockItem> ::= <Decl> | <Stmt>
        ASTNode node = new ASTNode("BlockItem");
        if (isDecl()) {
            node.addChild(parseDecl());
        } else {
            node.addChild(parseStmt());
        }
        return node;
    }

    private boolean isLValEQL() {
        int lineNum = curToken().lineNum;
        for (int i = 1; isReachable(i); i++) {
            if (tokenWithOffset(i).isType(LexType.ASSIGN))
                return true;

            if (tokenWithOffset(i).lineNum != lineNum || tokenWithOffset(i).isType(LexType.SEMICN))
                break;
        }
        return false;
    }

    private ASTNode parseStmt() {
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
        ASTNode node = new ASTNode("Stmt");
        if (curToken().isType(LexType.LBRACE)) {
            node.addChild(parseBlock());
        } else if (curToken().isType(LexType.IFTK)) {
            node.addChild(parseTokenType(LexType.IFTK));
            node.addChild(parseTokenType(LexType.LPARENT));
            node.addChild(parseCond());
            checkRparent();
            node.addChild(parseTokenType(LexType.RPARENT));
            node.addChild(parseStmt());
            if (curToken().isType(LexType.ELSETK)) {
                node.addChild(parseTokenType(LexType.ELSETK));
                node.addChild(parseStmt());
            }
        } else if (curToken().isType(LexType.FORTK)) {
            node.addChild(parseTokenType(LexType.FORTK));
            node.addChild(parseTokenType(LexType.LPARENT));
            if (!curToken().isType(LexType.SEMICN)) {
                node.addChild(parseForStmt());
            }
            checkSemicn();
            node.addChild(parseTokenType(LexType.SEMICN));
            if (!curToken().isType(LexType.SEMICN)) {
                node.addChild(parseCond());
            }
            checkSemicn();
            node.addChild(parseTokenType(LexType.SEMICN));
            if (!curToken().isType(LexType.RPARENT)) {
                node.addChild(parseForStmt());
            }
            checkRparent();
            node.addChild(parseTokenType(LexType.RPARENT));
            node.addChild(parseStmt());
        } else if (curToken().isType(LexType.BREAKTK)) {
            node.addChild(parseTokenType(LexType.BREAKTK));
            checkSemicn();
            node.addChild(parseTokenType(LexType.SEMICN));
        } else if (curToken().isType(LexType.CONTINUETK)) {
            node.addChild(parseTokenType(LexType.CONTINUETK));
            checkSemicn();
            node.addChild(parseTokenType(LexType.SEMICN));
        } else if (curToken().isType(LexType.RETURNTK)) {
            node.addChild(parseTokenType(LexType.RETURNTK));
            if (!curToken().isType(LexType.SEMICN)) {
                ASTNode expNode = parseExp();
                if (!isActuallyEmpty(expNode)) {
                    node.addChild(expNode);
                }
            }
            checkSemicn();
            node.addChild(parseTokenType(LexType.SEMICN));
        } else if (curToken().isType(LexType.PRINTFTK)) {
            node.addChild(parseTokenType(LexType.PRINTFTK));
            node.addChild(parseTokenType(LexType.LPARENT));
            node.addChild(parseStringConst());
            while (curToken().isType(LexType.COMMA)) {
                node.addChild(parseTokenType(LexType.COMMA));
                node.addChild(parseExp());
            }
            checkRparent();
            node.addChild(parseTokenType(LexType.RPARENT));
            checkSemicn();
            node.addChild(parseTokenType(LexType.SEMICN));
        } else if (curToken().isType(LexType.IDENFR)) {
            // 可能是 LVal '=', 也可能是 Exp
            // <LVal> ::= <Ident> | <Ident> '[' <Exp> ']'
            // <Exp> ::= <AddExp> ::= <MulExp> ::= <UnaryExp> ::= <PrimaryExp> | <Ident> '(' [ FuncRParams ] ')'
            // <PrimaryExp> ::= '(' <Exp> ')' | <LVal> | <Number> | <Character>
            // case 1: <Ident> '(' [ FuncRParams ] ')'
            if (isReachable(1) && tokenWithOffset(1).isType(LexType.LPARENT)) {
                // <Ident> '(' [ FuncRParams ] ')'，解析 Exp
                node.addChild(parseExp());
            } else {
                // 区分 <LVal> 来自 <PrimaryExp> 还是 <LVal> '=' <Exp>
                // <LVal> ::= <Ident> | <Ident> '[' <Exp> ']'
//                int offset = 1;
//                if (isReachable(1) && tokenWithOffset(1).isType(LexType.LBRACK)) {
//                    // 进行中括号匹配，假设中间一定是合法 <Exp>, 且可构成 <Ident> '[' <Exp> ']' 作为 <LVal>
//                    int cnt = 0, lineNum = tokenWithOffset(1).lineNum;
//                    do {
//                        if (tokenWithOffset(offset).isType(LexType.LBRACK)) {
//                            cnt++;
//                        } else if (tokenWithOffset(offset).isType(LexType.RBRACK)) {
//                            cnt--;
//                        } else if (tokenWithOffset(offset).isType(LexType.SEMICN) || tokenWithOffset(offset).isType(LexType.ASSIGN)) {
//                            break;
//                        }
//                        offset++;
//                    } while (isReachable(offset) && cnt != 0 && tokenWithOffset(offset).lineNum == lineNum);
//                }
                // 找到 <LVal> 结束处，判断 tokenWithOffset(offset) 是否为 '='
//                if (tokenWithOffset(offset).isType(LexType.ASSIGN)) {
                if (isLValEQL()) {
                    node.addChild(parseLVal());
                    node.addChild(parseTokenType(LexType.ASSIGN));
                    // | <LVal> '=' 'getint' '(' ')' ';'
                    // | <LVal> '=' 'getchar' '(' ')' ';'
                    // | <LVal> '=' <Exp> ';'
                    if (curToken().isType(LexType.GETINTTK)) {
                        node.addChild(parseTokenType(LexType.GETINTTK));
                        node.addChild(parseTokenType(LexType.LPARENT));
                        checkRparent();
                        node.addChild(parseTokenType(LexType.RPARENT));
                    } else if (curToken().isType(LexType.GETCHARTK)) {
                        node.addChild(parseTokenType(LexType.GETCHARTK));
                        node.addChild(parseTokenType(LexType.LPARENT));
                        checkRparent();
                        node.addChild(parseTokenType(LexType.RPARENT));
                    } else {
                        node.addChild(parseExp());
                    }
                } else {
                    // <Exp>
                    node.addChild(parseExp());
                }
            }
            checkSemicn();
            node.addChild(parseTokenType(LexType.SEMICN));
        } else if (curToken().isType(LexType.SEMICN)) {
            checkSemicn();
            node.addChild(parseTokenType(LexType.SEMICN));
        } else {
            node.addChild(parseExp());
            checkSemicn();
            node.addChild(parseTokenType(LexType.SEMICN));
        }

        if (OUTPUT) {
            sb.append(node.print()).append("\n");
        }
        return node;
    }

    private ASTNode parseCond() {
        // <Cond> ::= <LOrExp>
        ASTNode node = new ASTNode("Cond");
        node.addChild(parseLOrExp());

        if (OUTPUT) {
            sb.append(node.print()).append("\n");
        }
        return node;
    }

    private ASTNode parseLOrExp() {
        // <LOrExp> ::= <LAndExp> { '||' <LAndExp> }
        ASTNode node = new ASTNode("LOrExp");
        node.addChild(parseLAndExp());
        while (curToken().isType(LexType.OR)) {
            // node 成为新的第一个孩子结点
            if (OUTPUT) {
                sb.append(node.print()).append("\n");
            }
            ASTNode newNode = new ASTNode("LOrExp");
            newNode.addChild(node);
            newNode.addChild(parseTokenType(LexType.OR));
            newNode.addChild(parseLAndExp());
            node = newNode;
        }

        if (OUTPUT) {
            sb.append(node.print()).append("\n");
        }
        return node;
    }

    private ASTNode parseLAndExp() {
        // <LAndExp> ::= <EqExp> { '&&' <EqExp> }
        ASTNode node = new ASTNode("LAndExp");
        node.addChild(parseEqExp());
        while (curToken().isType(LexType.AND)) {
            // node 成为新的第一个孩子结点
            if (OUTPUT) {
                sb.append(node.print()).append("\n");
            }
            ASTNode newNode = new ASTNode("LAndExp");
            newNode.addChild(node);
            newNode.addChild(parseTokenType(LexType.AND));
            newNode.addChild(parseEqExp());
            node = newNode;
        }

        if (OUTPUT) {
            sb.append(node.print()).append("\n");
        }
        return node;
    }

    private ASTNode parseEqExp() {
        // <EqExp> ::= <RelExp> { ('==' | '!=') <RelExp> }
        ASTNode node = new ASTNode("EqExp");
        node.addChild(parseRelExp());
        while (curToken().isType(LexType.EQL) || curToken().isType(LexType.NEQ)) {
            // node 成为新的第一个孩子结点
            if (OUTPUT) {
                sb.append(node.print()).append("\n");
            }
            ASTNode newNode = new ASTNode("EqExp");
            newNode.addChild(node);
            newNode.addChild(parseTokenType(curToken().type));
            newNode.addChild(parseRelExp());
            node = newNode;
        }

        if (OUTPUT) {
            sb.append(node.print()).append("\n");
        }
        return node;
    }

    private ASTNode parseRelExp() {
        // <RelExp> ::= <AddExp> { ('<' | '<=' | '>' | '>=' ) <AddExp> }
        ASTNode node = new ASTNode("RelExp");
        node.addChild(parseAddExp());
        while (curToken().isType(LexType.LSS) || curToken().isType(LexType.LEQ) || curToken().isType(LexType.GRE) || curToken().isType(LexType.GEQ)) {
            // node 成为新的第一个孩子结点
            if (OUTPUT) {
                sb.append(node.print()).append("\n");
            }
            ASTNode newNode = new ASTNode("RelExp");
            newNode.addChild(node);
            newNode.addChild(parseTokenType(curToken().type));
            newNode.addChild(parseAddExp());
            node = newNode;
        }

        if (OUTPUT) {
            sb.append(node.print()).append("\n");
        }
        return node;
    }

    private ASTNode parseForStmt() {
        // <ForStmt> ::=  <LVal> '=' <Exp>
        ASTNode node = new ASTNode("ForStmt");
        node.addChild(parseLVal());
        node.addChild(parseTokenType(LexType.ASSIGN));
        node.addChild(parseExp());

        if (OUTPUT) {
            sb.append(node.print()).append("\n");
        }
        return node;
    }

    private ASTNode parseStringConst() {
         // <StringConst> ::= <Str>
         ASTNode node = new ASTNode("StringConst");
         node.addChild(parseTokenType(LexType.STRCON));
         return node;
    }

    private ASTNode parseMainFuncDef() {
        // <MainFuncDef> ::= 'int' 'main' '(' ')' <Block>
        ASTNode node = new ASTNode("MainFuncDef");
        node.addChild(parseTokenType(LexType.INTTK));
        node.addChild(parseTokenType(LexType.MAINTK));
        node.addChild(parseTokenType(LexType.LPARENT));
        checkRparent();
        node.addChild(parseTokenType(LexType.RPARENT));
        node.addChild(parseBlock());

        if (OUTPUT) {
            sb.append(node.print()).append("\n");
        }
        return node;
    }

    private ASTNode parseConstDecl() {
        // <ConstDecl> ::= 'const' <BType> <ConstDef> { ',' <ConstDef> } ';'
        ASTNode node = new ASTNode("ConstDecl");
        // 'const'
        node.addChild(parseTokenType(LexType.CONSTTK));
        // <BType>
        node.addChild(parseBType());
        // <ConstDef>
        node.addChild(parseConstDef());
        // { ',' <ConstDef> }
        while (curToken().isType(LexType.COMMA)) {
            node.addChild(parseTokenType(LexType.COMMA));
            node.addChild(parseConstDef());
        }
        // ';'
        checkSemicn();
        node.addChild(parseTokenType(LexType.SEMICN));


        if (OUTPUT) {
            sb.append(node.print()).append("\n");
        }
        return node;
    }

    private ASTNode parseBType() {
        // <BType> ::= 'int' | 'char'
        ASTNode node = new ASTNode("BType");
        if (curToken().isType(LexType.INTTK) || curToken().isType(LexType.CHARTK)) {
            node.addChild(parseTokenType(curToken().type));
            return node;
        }
        return null;
    }

    private ASTNode parseConstDef() {
        // <ConstDef> ::= <Ident> '=' <ConstInitVal> | <Ident> '[' <ConstExp> ']' '=' <ConstInitVal>
        ASTNode node = new ASTNode("ConstDef");
        // <Ident>
        node.addChild(parseIdent());
        // [ '[' <ConstExp> ']' ]
        if (curToken().isType(LexType.LBRACK)) {
            node.addChild(parseTokenType(LexType.LBRACK));
            node.addChild(parseConstExp());
            checkRbrack();
            node.addChild(parseTokenType(LexType.RBRACK));
        }
        // '='
        node.addChild(parseTokenType(LexType.ASSIGN));
        // <ConstInitVal>
        node.addChild(parseConstInitVal());


        if (OUTPUT) {
            sb.append(node.print()).append("\n");
        }
        return node;
    }

    private ASTNode parseIdent() {
        return parseTokenType(LexType.IDENFR);
    }

    private ASTNode parseConstExp() {
        // <ConstExp> ::= <AddExp>
        ASTNode node = new ASTNode("ConstExp");
        node.addChild(parseAddExp());

        if (OUTPUT) {
            sb.append(node.print()).append("\n");
        }
        return node;
    }

    private ASTNode parseExp() {
        // <Exp> ::= <AddExp>
        ASTNode node = new ASTNode("Exp");
        node.addChild(parseAddExp());

        if (OUTPUT) {
            sb.append(node.print()).append("\n");
        }
        return node;
    }

    private ASTNode parseAddExp() {
        // <AddExp> ::= <MulExp> {<AddOp> <MulExp>}
        ASTNode node = new ASTNode("AddExp");
        node.addChild(parseMulExp());

        while (isAddOp()) {
            // node 成为新的第一个孩子结点
            if (OUTPUT) {
                sb.append(node.print()).append("\n");
            }
            ASTNode newNode = new ASTNode("AddExp");
            newNode.addChild(node);
            newNode.addChild(parseTokenType(curToken().type));
            newNode.addChild(parseMulExp());
            node = newNode;
        }

        if (OUTPUT) {
            sb.append(node.print()).append("\n");
        }
        return node;
    }

    private boolean isAddOp() {
        return curToken().isType(LexType.PLUS) || curToken().isType(LexType.MINU);
    }

    private ASTNode parseMulExp() {
        // <MulExp> ::= <UnaryExp> {<MulOp> <UnaryExp>}
        ASTNode node = new ASTNode("MulExp");
        node.addChild(parseUnaryExp());

        while (isMulOp()) {
            // node 成为新的第一个孩子结点
            if (OUTPUT) {
                sb.append(node.print()).append("\n");
            }
            ASTNode newNode = new ASTNode("MulExp");
            newNode.addChild(node);
            newNode.addChild(parseTokenType(curToken().type));
            newNode.addChild(parseUnaryExp());
            node = newNode;
        }
        if (OUTPUT) {
            sb.append(node.print()).append("\n");
        }
        return node;
    }

    private boolean isMulOp() {
        return curToken().isType(LexType.MULT) || curToken().isType(LexType.DIV) || curToken().isType(LexType.MOD);
    }

    private ASTNode parseUnaryExp() {
        // <UnaryExp> ::= <PrimaryExp> | <UnaryOp> <UnaryExp> | <Ident> '(' [<FuncRParams>] ')'
        ASTNode node = new ASTNode("UnaryExp");
        if (curToken().isType(LexType.IDENFR) && isReachable(1) && tokenWithOffset(1).isType(LexType.LPARENT)) {
            node.addChild(parseIdent());
            node.addChild(parseTokenType(LexType.LPARENT));
            if (!curToken().isType(LexType.RPARENT)) {
                node.addChild(parseFuncRParams());
            }
            checkRparent();
            node.addChild(parseTokenType(LexType.RPARENT));
        } else if (isUnaryOp()) {
            node.addChild(parseUnaryOp());
            node.addChild(parseUnaryExp());
        } else {
            node.addChild(parsePrimaryExp());
        }

        if (OUTPUT) {
            sb.append(node.print()).append("\n");
        }
        return node;
    }

    private ASTNode parseFuncRParams() {
        // FuncRParams ::= <Exp> {',' <Exp>}
        ASTNode node = new ASTNode("FuncRParams");
        ASTNode firstNode = parseExp();
        if (!isActuallyEmpty(firstNode)) {
            node.addChild(firstNode);
        }
        while (curToken().isType(LexType.COMMA)) {
            node.addChild(parseTokenType(LexType.COMMA));
            node.addChild(parseExp());
        }
        if (OUTPUT) {
            sb.append(node.print()).append("\n");
        }
        return node;
    }

    private ASTNode parseFuncFParams() {
        // FuncFParams ::= <FuncFParam> {',' <FuncFParam>}
        ASTNode node = new ASTNode("FuncFParams");
        node.addChild(parseFuncFParam());
        while (curToken().isType(LexType.COMMA)) {
            node.addChild(parseTokenType(LexType.COMMA));
            node.addChild(parseFuncFParam());
        }

        if (OUTPUT) {
            sb.append(node.print()).append("\n");
        }
        return node;
    }

    private ASTNode parseFuncFParam() {
        // FuncFParam ::= <BType> <Ident> [ '[' ']' ]
        ASTNode node = new ASTNode("FuncFParam");
        node.addChild(parseBType());
        node.addChild(parseIdent());
        if (curToken().isType(LexType.LBRACK)) {
            node.addChild(parseTokenType(LexType.LBRACK));
            checkRbrack();
            node.addChild(parseTokenType(LexType.RBRACK));
        }
        if (OUTPUT) {
            sb.append(node.print()).append("\n");
        }
        return node;
    }

    private boolean isUnaryOp() {
        return curToken().isType(LexType.PLUS) || curToken().isType(LexType.MINU) || curToken().isType(LexType.NOT);
    }

    private ASTNode parseUnaryOp() {
        ASTNode node = new ASTNode("UnaryOp");
        if (curToken().isType(LexType.PLUS)) {
            node.addChild(parseTokenType(LexType.PLUS));
        } else if (curToken().isType(LexType.MINU)) {
            node.addChild(parseTokenType(LexType.MINU));
        } else {
            node.addChild(parseTokenType(LexType.NOT));
        }

        if (OUTPUT) {
            sb.append(node.print()).append("\n");
        }
        return node;
    }

    private ASTNode parsePrimaryExp() {
        // <PrimaryExp> ::= '(' <Exp> ')' | <LVal> | <Number> | <Character>
        ASTNode node = new ASTNode("PrimaryExp");
        if (curToken().isType(LexType.LPARENT)) {
            node.addChild(parseTokenType(LexType.LPARENT));
            node.addChild(parseExp());
            checkRparent();
            node.addChild(parseTokenType(LexType.RPARENT));
        } else if (curToken().isType(LexType.IDENFR)) {
            node.addChild(parseLVal());
        } else if (curToken().isType(LexType.INTCON)) {
            node.addChild(parseNumber());
        } else {
            node.addChild(parseChar());
        }

        if (OUTPUT) {
            sb.append(node.print()).append("\n");
        }
        return node;
    }

    private ASTNode parseLVal() {
        // <LVal> ::= <Ident> [ '[' <Exp> ']' ]
        ASTNode node = new ASTNode("LVal");
        node.addChild(parseIdent());
        if (curToken().isType(LexType.LBRACK)) {
            node.addChild(parseTokenType(LexType.LBRACK));
            node.addChild(parseExp());
            checkRbrack();
            node.addChild(parseTokenType(LexType.RBRACK));
        }

        if (OUTPUT) {
            sb.append(node.print()).append("\n");
        }
        return node;
    }

    private ASTNode parseNumber() {
        // <Number> ::= <IntConst>
        ASTNode node = new ASTNode("Number");
        node.addChild(parseTokenType(LexType.INTCON));
        if (OUTPUT) {
            sb.append(node.print()).append("\n");
        }
        return node;
    }

    private ASTNode parseChar() {
        // <Character> ::= <CharConst>
        ASTNode node = new ASTNode("Character");
        node.addChild(parseTokenType(LexType.CHRCON));
        if (OUTPUT) {
            sb.append(node.print()).append("\n");
        }
        return node;
    }

    private ASTNode parseConstInitVal() {
        // <ConstInitVal> ::= <ConstExp> | '{' [ <ConstExp> { ',' <ConstExp> } ] '}' | StringConst
        ASTNode node = new ASTNode("ConstInitVal");
        if (curToken().isType(LexType.LBRACE)) {
            node.addChild(parseTokenType(LexType.LBRACE));
            if (!curToken().isType(LexType.RBRACE)) {
                node.addChild(parseConstExp());
                while (curToken().isType(LexType.COMMA)) {
                    node.addChild(parseTokenType(LexType.COMMA));
                    node.addChild(parseConstExp());
                }
            }
            node.addChild(parseTokenType(LexType.RBRACE));
        } else if (curToken().isType(LexType.STRCON)) {
            // node.addChild(parseTokenType(LexType.STRCON));
            node.addChild(parseStringConst());
        } else {
            node.addChild(parseConstExp());
        }

        if (OUTPUT) {
            sb.append(node.print()).append("\n");
        }
        return node;
    }

    private ASTNode parseVarDecl() {
        // <VarDecl> ::= <BType> <VarDef> { ',' <VarDef> } ';'
        ASTNode node = new ASTNode("VarDecl");
        node.addChild(parseBType());
        node.addChild(parseVarDef());
        while (curToken().isType(LexType.COMMA)) {
            node.addChild(parseTokenType(LexType.COMMA));
            node.addChild(parseVarDef());
        }

        checkSemicn();
        node.addChild(parseTokenType(LexType.SEMICN));

        if (OUTPUT) {
            sb.append(node.print()).append("\n");
        }
        return node;
    }

    private ASTNode parseVarDef() {
        // <VarDef> ::= <Ident> [ '[' <ConstExp> ']' ] [ '=' <InitVal> ]
        ASTNode node = new ASTNode("VarDef");
        node.addChild(parseIdent());
        if (curToken().isType(LexType.LBRACK)) {
            node.addChild(parseTokenType(LexType.LBRACK));
            node.addChild(parseConstExp());
            checkRbrack();
            node.addChild(parseTokenType(LexType.RBRACK));
        }
        if (curToken().isType(LexType.ASSIGN)) {
            node.addChild(parseTokenType(LexType.ASSIGN));
            node.addChild(parseInitVal());
        }

        if (OUTPUT) {
            sb.append(node.print()).append("\n");
        }
        return node;
    }

    private ASTNode parseInitVal() {
        // <InitVal> ::= <StringConst> | '{' [ <Exp> { ',' <Exp> } ] '}' | <Exp>
        ASTNode node = new ASTNode("InitVal");
        if (curToken().isType(LexType.STRCON)) {
            // node.addChild(parseTokenType(LexType.STRCON));
            node.addChild(parseStringConst());
        } else if (curToken().isType(LexType.LBRACE)) {
            node.addChild(parseTokenType(LexType.LBRACE));
            if (!curToken().isType(LexType.RBRACE)) {
                node.addChild(parseExp());
                while (curToken().isType(LexType.COMMA)) {
                    node.addChild(parseTokenType(LexType.COMMA));
                    node.addChild(parseExp());
                }
            }
            node.addChild(parseTokenType(LexType.RBRACE));
        } else {
            node.addChild(parseExp());
        }
        if (OUTPUT) {
            sb.append(node.print()).append("\n");
        }
        return node;
    }

    private ASTNode parseTokenType(LexType type) {
         if (!curToken().isType(type))
             return null;
        if (OUTPUT)
            sb.append(curToken()).append("\n");
        nextToken();
        return new LeafASTNode(tokenWithOffset(-1));
    }
}
