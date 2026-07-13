package frontEnd;

import java.util.ArrayList;

public class ASTNode {
    public String name;
    // public ASTNode parent = null;
    public ArrayList<ASTNode> children;
//    public ASTNode(String name, ASTNode parent) {
//        this.name = name;
//        this.parent = parent;
//    }

    public ASTNode(String name) {
        this.name = name;
        this.children = new ArrayList<>();
    }

    public boolean isNode(String name) {
        return this.name.equals(name);
    }

//    private void setParent(ASTNode parent) {
//        this.parent = parent;
//    }

    public void addChild(ASTNode child) {
        if (child == null) return;
        if (this instanceof LeafASTNode) return;
        children.add(child);
//        child.setParent(this);
    }

    public String print() {
        if (this instanceof LeafASTNode) return "";
        return "<" + name + ">";
    }
}
