package middleEnd;

public class GlobalString extends Value implements UsableValue {
    private final int no;
    public final String string;
    private final String type;
    public GlobalString(int no, String string, int length) {
        super();
        this.no = no;
        this.string = string;
        this.type = String.format("[%d x i8]", length);
    }

    @Override
    public String toValueIR() {
        return String.format("@.str%d", no);
    }

    @Override
    public String toLLVMType() {
        return type + "*";
    }

    @Override
    public int toAlign() {
        return 1;
    }

    @Override
    public void setVirtualRegNo(int regNo) {
        throw new RuntimeException("GlobalString cannot be assigned a register");
    }

    @Override
    public int getMemorySize() {
        // 全局变量分配在 .data 段
        return 0;
    }

    @Override
    public String toString() {
        return String.format("%s = private unnamed_addr constant %s c\"%s\", align %d", toValueIR(), type, string, toAlign());
    }
}
