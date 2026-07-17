package middleEnd.utils;

import frontEnd.ASTNode;
import frontEnd.LeafASTNode;
import frontEnd.LiteralValue;
import frontEnd.Token;
import frontEnd.lexer.LexType;
import middleEnd.ConstInitVal;
import middleEnd.LLVMSymbolTable;
import middleEnd.UsableValue;

import java.util.HashMap;

public class ConstCalculator {
    private record TypedConst(int value, boolean charType) {
        private TypedConst normalize() {
            return charType ? new TypedConst(value & 0xff, true) : this;
        }
    }

    private final HashMap<Integer, LLVMSymbolTable> symbolTables;
    private int scopeId;

    public ConstCalculator(HashMap<Integer, LLVMSymbolTable> symbolTables) {
        this.symbolTables = symbolTables;
    }

    private UsableValue getConstSymbol(String name) {
        int currScopeId = scopeId;
        while (currScopeId > 0) {
            LLVMSymbolTable table = symbolTables.get(currScopeId);
            if (table.symbols.containsKey(name)) {
                return table.symbols.get(name);
            }
            if (table.parentTable == null) {
                break;
            }
            currScopeId = table.parentTable.id;
        }
        throw new RuntimeException("Const not found: " + name);
    }

    private TypedConst getConst(String name, int index) {
        UsableValue value = getConstSymbol(name);
        int currScopeId = scopeId;
        while (currScopeId > 0) {
            LLVMSymbolTable table = symbolTables.get(currScopeId);
            if (table.symbols.containsKey(name)) {
                ConstInitVal initVal = table.getConstInitVal(value);
                return new TypedConst(initVal.getConstValue(index), value.toLLVMType().contains("i8")).normalize();
            }
            currScopeId = table.parentTable.id;
        }
        throw new RuntimeException("Const not found: " + name);
    }

    public int calculateConstExp(ASTNode node, int scopeId) {
        this.scopeId = scopeId;
        return calculateConstExp(node).value;
    }

    private TypedConst calculateConstExp(ASTNode node) {
        return calculateAddExp(node.children.get(0));
    }

    private TypedConst calculateAddExp(ASTNode node) {
        if (node.children.size() == 1) {
            return calculateMulExp(node.children.get(0));
        }
        TypedConst left = calculateAddExp(node.children.get(0));
        TypedConst right = calculateMulExp(node.children.get(2));
        int value = ((LeafASTNode) node.children.get(1)).token.token.equals("+")
                ? left.value + right.value : left.value - right.value;
        return new TypedConst(value, left.charType).normalize();
    }

    private TypedConst calculateMulExp(ASTNode node) {
        if (node.children.size() == 1) {
            return calculateUnaryExp(node.children.get(0));
        }
        TypedConst left = calculateMulExp(node.children.get(0));
        TypedConst right = calculateUnaryExp(node.children.get(2));
        String operator = ((LeafASTNode) node.children.get(1)).token.token;
        int value = switch (operator) {
            case "*" -> left.value * right.value;
            case "/" -> left.value / right.value;
            default -> left.value % right.value;
        };
        return new TypedConst(value, left.charType).normalize();
    }

    private TypedConst calculateUnaryExp(ASTNode node) {
        if (node.children.size() == 1) {
            return calculatePrimaryExp(node.children.get(0));
        }
        if (node.children.get(0) instanceof LeafASTNode leaf && leaf.token.isType(LexType.LPARENT)) {
            TypedConst value = calculateUnaryExp(node.children.get(node.children.size() - 1));
            LeafASTNode target = (LeafASTNode) node.children.get(1).children.get(0);
            return new TypedConst(value.value, target.token.isType(LexType.CHARTK)).normalize();
        }
        if (node.children.get(0) instanceof LeafASTNode leaf) {
            throw new RuntimeException("Function call is not a constant expression: " + leaf.token.token);
        }
        TypedConst value = calculateUnaryExp(node.children.get(1));
        String operator = ((LeafASTNode) node.children.get(0).children.get(0)).token.token;
        return switch (operator) {
            case "+" -> value;
            case "-" -> new TypedConst(-value.value, value.charType).normalize();
            default -> new TypedConst(value.value == 0 ? 1 : 0, false);
        };
    }

    private TypedConst calculatePrimaryExp(ASTNode node) {
        if (node.children.size() == 3) {
            return calculateConstExp(node.children.get(1));
        }
        if (node.children.get(0).isNode("LVal")) {
            ASTNode lval = node.children.get(0);
            String name = ((LeafASTNode) lval.children.get(0)).token.token;
            int index = lval.children.size() == 1 ? 0 : calculateConstExp(lval.children.get(2)).value;
            return getConst(name, index);
        }
        Token token = ((LeafASTNode) node.children.get(0).children.get(0)).token;
        if (token.isType(LexType.INTCON)) {
            return new TypedConst(Integer.parseInt(token.token), false);
        }
        return new TypedConst(LiteralValue.parse(token), true);
    }
}
