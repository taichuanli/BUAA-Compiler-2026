package backEnd;

public class MIPSComment extends MIPSInst {
    private final String comment;
    public MIPSComment(String comment) {
        this.comment = comment;
    }

    @Override
    public String toString() {
        return "# " + comment;
    }
}
