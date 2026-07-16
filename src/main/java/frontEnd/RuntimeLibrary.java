package frontEnd;

import frontEnd.Symbol._SymbolType1;
import frontEnd.Symbol._SymbolType2;
import frontEnd.lexer.LexType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class RuntimeLibrary {
    private record Signature(_SymbolType1 returnType, List<_SymbolType1> parameterTypes) {
    }

    private static final Map<String, Signature> FUNCTIONS = Map.of(
            "get_int", new Signature(_SymbolType1.INT, List.of()),
            "get_char", new Signature(_SymbolType1.CHAR, List.of()),
            "put_int", new Signature(_SymbolType1.VOID, List.of(_SymbolType1.INT)),
            "put_char", new Signature(_SymbolType1.VOID, List.of(_SymbolType1.CHAR))
    );

    private RuntimeLibrary() {
    }

    public static boolean contains(String name) {
        return FUNCTIONS.containsKey(name);
    }

    public static Symbol resolve(Token callToken) {
        Signature signature = FUNCTIONS.get(callToken.token);
        if (signature == null) {
            throw new IllegalArgumentException("Unknown runtime function: " + callToken.token);
        }

        Symbol function = new Symbol(0, 0, callToken, signature.returnType(), _SymbolType2.FUNC);
        ArrayList<Symbol> parameters = new ArrayList<>();
        for (int i = 0; i < signature.parameterTypes().size(); i++) {
            Token parameterToken = new Token(LexType.IDENFR, "$runtime_arg" + i, callToken.lineNum);
            parameters.add(new Symbol(0, 0, parameterToken,
                    signature.parameterTypes().get(i), _SymbolType2.VAR));
        }
        function.setParams(parameters);
        return function;
    }
}
