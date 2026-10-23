package frontEnd.lexer;

// 枚举类
// 类别码以期中考核给出的《完整类别码表》为准：
// 表中没有 for / getint / getchar，因此三者不再作为保留字，仅保留枚举项供尚未迁移的中后端代码编译。
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
    ARROW,          // "->"
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
    YIELDTK,
    PRINTFTK,
    RETURNTK,
    VOIDTK,
    NOTE,
    // 以下为 2024 文法的遗留记号，2026 文法中不是保留字，Parser 已不再产生它们。
    FORTK,
    GETINTTK,
    GETCHARTK,
}
