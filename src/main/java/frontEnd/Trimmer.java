package frontEnd;

public class Trimmer {
    public static Trimmer instance = new Trimmer();
    public static boolean optimize = true;

    private Trimmer() {

    }

    public ASTNode trim(ASTNode root) {
        return trimCompUnit(root);
    }

    private ASTNode trimCompUnit(ASTNode node) {
        ASTNode ret = new ASTNode(node.name);
        for (ASTNode child : node.children) {
            if (child.isNode("Decl")) {
                ret.addChild(trimDecl(child));
            } else {
                ret.addChild(trimFuncDef(child));
            }
        }
        return ret;
    }

    private ASTNode trimDecl(ASTNode node) {
        return node;
    }

    private ASTNode trimFuncDef(ASTNode node) {
        ASTNode ret = new ASTNode(node.name);
        for (ASTNode child : node.children) {
            if (child.isNode("Block")) {
                ret.addChild(trimBlock(child));
            } else {
                ret.addChild(child);
            }
        }
        return ret;
    }

    private ASTNode trimBlock(ASTNode node) {
        ASTNode ret = new ASTNode(node.name);
        for (ASTNode child : node.children) {
            if (child.isNode("BlockItem")) {
                ret.addChild(trimBlockItem(child));
            } else {
                ret.addChild(child);
            }
        }
        return ret;
    }

    private ASTNode trimBlockItem(ASTNode node) {
        ASTNode ret = new ASTNode(node.name);
        for (ASTNode child : node.children) {
            if (child.isNode("Stmt")) {
                ret.addChild(trimStmt(child));
            } else {
                ret.addChild(trimDecl(child));
            }
        }
        return ret;
    }

    private ASTNode trimStmt(ASTNode node) {
        ASTNode ret = new ASTNode(node.name);

        for (ASTNode child : node.children) {
            if (child.isNode("Cond")) {
                ret.addChild(trimCond(child));
            } else if (child.isNode("Block")) {
                ret.addChild(trimBlock(child));
            } else if (child.isNode("Stmt")) {
                ret.addChild(trimStmt(child));
            } else {
                ret.addChild(child);
            }
        }

        return ret;
    }

    private ASTNode trimCond(ASTNode node) {
        ASTNode ret = new ASTNode(node.name);
        ret.addChild(trimLOrExp(node.children.get(0)));
        return ret;
    }

    private ASTNode trimLOrExp(ASTNode node) {
        // from: LOrExp → LAndExp | LOrExp '||' LAndExp
        // to  : LOrExp → LAndExp { '||' LAndExp }
        ASTNode ret = new ASTNode(node.name);
        if (node.children.size() == 1) {
            ret.addChild(trimLAndExp(node.children.get(0)));
        } else {
            ret = trimLOrExp(node.children.get(0));
            ret.addChild(node.children.get(1));
            ret.addChild(trimLAndExp(node.children.get(2)));
        }
        return ret;
    }

    private ASTNode trimLAndExp(ASTNode node) {
        // from: LAndExp → EqExp | LAndExp '&&' EqExp
        // to  : LAndExp → EqExp { '&&' EqExp }
        ASTNode ret = new ASTNode(node.name);
        if (node.children.size() == 1) {
            ret.addChild(trimEqExp(node.children.get(0)));
        } else {
            ret = trimLAndExp(node.children.get(0));
            ret.addChild(node.children.get(1));
            ret.addChild(trimEqExp(node.children.get(2)));
        }
        return ret;
    }

    private ASTNode trimEqExp(ASTNode node) {
        return node;
    }
}
