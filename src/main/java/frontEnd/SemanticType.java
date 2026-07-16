package frontEnd;

import frontEnd.Symbol.SymbolType;

/** The five source-language value types used by the 2026 semantic rules. */
public enum SemanticType {
    INT,
    CHAR,
    INT_ARRAY,
    CHAR_ARRAY,
    VOID,
    FUNCTION,
    ERROR;

    public static SemanticType fromSymbol(SymbolType type) {
        return switch (type) {
            case ConstInt, Int -> INT;
            case ConstChar, Char -> CHAR;
            case IntArray, ConstIntArray -> INT_ARRAY;
            case CharArray, ConstCharArray -> CHAR_ARRAY;
            case IntFunc -> INT;
            case CharFunc -> CHAR;
            case VoidFunc -> VOID;
            case ERROR -> ERROR;
        };
    }

    public static boolean isFunction(SymbolType type) {
        return type == SymbolType.IntFunc || type == SymbolType.CharFunc || type == SymbolType.VoidFunc;
    }

    public boolean isScalar() {
        return this == INT || this == CHAR;
    }

    public boolean isArray() {
        return this == INT_ARRAY || this == CHAR_ARRAY;
    }
}
