package frontEnd;

public class LeafASTNode extends ASTNode{
    public final Token token;
    public LeafASTNode(Token token) {
        super("LEAF");
        this.token = token;
        this.children = null;
    }
}