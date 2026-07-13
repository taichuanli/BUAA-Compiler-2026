package frontEnd;

import java.util.ArrayList;

public class Symbol {
    public int id;
    public int tableId;
    public Token token;
    // public _SymbolType1 symbolType1;
    // public _SymbolType2 symbolType2;
    public SymbolType symbolType;
    public ArrayList<Symbol> params = new ArrayList<>();

    public Symbol(int id, int tableId, Token token, _SymbolType1 symbolType1, _SymbolType2 symbolType2) {
        this.id = id;
        this.tableId = tableId;
        this.token = token;
        this.symbolType = toType(symbolType1, symbolType2);
    }

    public void setParams(ArrayList<Symbol> params) {
        this.params = new ArrayList<>(params);
    }

    public enum _SymbolType1 {
        INT,
        CHAR,
        VOID,
    }

    public enum _SymbolType2 {
        VAR,
        ARRAY,
        FUNC,
        CONST,
        CONSTARRAY,
    }


    public enum SymbolType {
        ConstInt,
        Int,
        IntFunc,
        IntArray,
        ConstIntArray,
        ConstChar,
        Char,
        CharFunc,
        CharArray,
        ConstCharArray,
        VoidFunc,
        ERROR,
    }

    private SymbolType toType(_SymbolType1 symbolType1, _SymbolType2 symbolType2) {
        // char型常量	ConstChar	    char型变量	    Char	    void型函数	VoidFunc
        // int型常量	    ConstInt	    int型变量	    Int	        char型函数	CharFunc
        // char型常量数组	ConstCharArray	char型变量数组	CharArray	int型函数	IntFunc
        // int型常量数组	ConstIntArray	int型变量数组	    IntArray
        switch (symbolType1) {
            case INT -> {
                switch (symbolType2) {
                    case CONST -> {
                        return SymbolType.ConstInt;
                    }
                    case VAR -> {
                        return SymbolType.Int;
                    }
                    case FUNC -> {
                        return SymbolType.IntFunc;
                    }
                    case ARRAY -> {
                        return SymbolType.IntArray;
                    }
                    case CONSTARRAY -> {
                        return SymbolType.ConstIntArray;
                    }
                }
            }
            case CHAR -> {
                switch (symbolType2) {
                    case CONST -> {
                        return SymbolType.ConstChar;
                    }
                    case VAR -> {
                        return SymbolType.Char;
                    }
                    case FUNC -> {
                        return SymbolType.CharFunc;
                    }
                    case ARRAY -> {
                        return SymbolType.CharArray;
                    }
                    case CONSTARRAY -> {
                        return SymbolType.ConstCharArray;
                    }
                }
            }
            case VOID -> {
                switch (symbolType2) {
                    case FUNC -> {
                        return SymbolType.VoidFunc;
                    }
                }
            }
        }
        return SymbolType.ERROR;
    }

    @Override
    public String toString() {
        return tableId + " " + token.token + " " + symbolType;
    }
}
