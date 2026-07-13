package middleEnd;

public class LLVMType {
    public enum TypeID {
        VoidTyID("void"),
        IntegerTyID("i32"),
        IntegerPtrTyID("i32*"),
        IntegerPtrPtrTyID("i32**"),
        CharTyID("i8"),
        CharPtrTyID("i8*"),
        CharPtrPtrTyID("i8**"),
        I1("i1"),
        ;

        private final String s;

        TypeID(String s) {
            this.s = s;
        }

        @Override
        public String toString() {
            return s;
        }

        public int toAlign() {
            return switch (s) {
                // case "i32" -> 4;
                case "i8" -> 1;
                // case "i32*", "i8*" -> 8;
                default -> 4;
            };
        }

        public TypeID toPointerType() {
            return switch (this) {
                case IntegerPtrTyID -> IntegerPtrPtrTyID;
                case IntegerTyID -> IntegerPtrTyID;
                case CharPtrTyID -> CharPtrPtrTyID;
                case CharTyID -> CharPtrTyID;
                default -> this;
            };
        }
    }

    public enum InstType {
        LOAD("load"),
        STORE("store"),
        ALLOCA("alloca"),
        GETELEMENTPTR("getelementptr"),

        ADD("add"),
        SUB("sub"),
        MUL("mul"),
        SDIV("sdiv"),
        SREM("srem"),

        LABEL("label"),
        BRANCH("br"),
        RETURN("ret"),

        ZEXT("zext"),
        TRUNC("trunc"),
        CALL("call"),

        ICMP_EQ("icmp eq"),
        ICMP_NE("icmp ne"),
        ICMP_SGT("icmp sgt"),
        ICMP_SGE("icmp sge"),
        ICMP_SLT("icmp slt"),
        ICMP_SLE("icmp sle"),

        SHL("shl"),

        ;
        private final String s;
        InstType(String s) {
            this.s = s;
        }
        @Override
        public String toString() {
            return s;
        }
    }
}
