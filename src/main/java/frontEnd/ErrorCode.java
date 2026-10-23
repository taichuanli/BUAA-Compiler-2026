package frontEnd;

public enum ErrorCode {
    ILLEGAL_LOGICAL_OPERATOR("a"),
    NAME_REDEFINED("b"),
    NAME_UNDEFINED("c"),
    FUNCTION_ARGUMENT_COUNT_MISMATCH("d"),
    TYPE_MISMATCH("e"),
    VOID_FUNCTION_RETURNS_VALUE("f"),
    MISSING_RETURN("g"),
    CONST_ASSIGNMENT("h"),
    MISSING_SEMICOLON("i"),
    MISSING_RIGHT_PARENTHESIS("j"),
    MISSING_RIGHT_BRACKET("k"),
    PRINTF_ARGUMENT_COUNT_MISMATCH("l"),
    INVALID_CONTROL_FLOW("m"),
    DUPLICATED_CASE("n"),
    // 期中新增：switch 表达式的分支缺少 '->'，或语句形式 switch 的 case 缺少 ':'
    MISSING_BRANCH_SEPARATOR("o");

    private final String value;

    ErrorCode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
