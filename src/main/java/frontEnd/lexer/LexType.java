package frontEnd.lexer;

// 枚举类
public enum LexType {
    PLUS,           // "+"
    MINU,           // "-"
    MULT,           // "*"
    DIV,            // "/"
    MOD,            // "%"
    SEMICN,         // ";"
    COMMA,          // ","
    COLON,          // ":"
    LPARENT,        // "("
    RPARENT,        // ")"
    LBRACK,         // "["
    RBRACK,         // "]"
    LBRACE,         // "{"
    RBRACE,         // "}"
    NOT,            // "!"
    NEQ,            // "!="
    LEQ,            // "<="
    LSS,            // "<"
    GEQ,            // ">="
    GRE,            // ">"
    EQL,            // "=="
    ASSIGN,         // "="
    OR,             // "||"
    AND,            // "&&"
    INTCON,
    STRCON,
    CHRCON,
    IDENFR,
    MAINTK,
    CONSTTK,
    INTTK,
    CHARTK,
    BREAKTK,
    CONTINUETK,
    IFTK,
    ELSETK,
    WHILETK,
    SWITCHTK,
    CASETK,
    DEFAULTTK,
    STATICTK,
    PRINTFTK,
    RETURNTK,
    VOIDTK,
    NOTE,
}
