package frontEnd;

import frontEnd.Symbol._SymbolType1;
import frontEnd.Symbol._SymbolType2;
import frontEnd.lexer.LexType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class RuntimeLibrary {
    private record Parameter(_SymbolType1 type, _SymbolType2 kind) {
    }

    private record Signature(_SymbolType1 returnType, List<Parameter> parameters) {
    }

    private static Parameter scalar(_SymbolType1 type) {
        return new Parameter(type, _SymbolType2.VAR);
    }

    private static Parameter array(_SymbolType1 type) {
        return new Parameter(type, _SymbolType2.ARRAY);
    }

    private static final Map<String, Signature> FUNCTIONS = Map.of(
            "get_int", new Signature(_SymbolType1.INT, List.of()),
            "get_char", new Signature(_SymbolType1.CHAR, List.of()),
            "get_string", new Signature(_SymbolType1.VOID,
                    List.of(array(_SymbolType1.CHAR), scalar(_SymbolType1.INT))),
            "put_int", new Signature(_SymbolType1.VOID, List.of(scalar(_SymbolType1.INT))),
            "put_char", new Signature(_SymbolType1.VOID, List.of(scalar(_SymbolType1.CHAR))),
            "put_string", new Signature(_SymbolType1.VOID, List.of(array(_SymbolType1.CHAR)))
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
        for (int i = 0; i < signature.parameters().size(); i++) {
            Parameter parameter = signature.parameters().get(i);
            Token parameterToken = new Token(LexType.IDENFR, "$runtime_arg" + i, callToken.lineNum);
            parameters.add(new Symbol(0, 0, parameterToken,
                    parameter.type(), parameter.kind()));
        }
        function.setParams(parameters);
        return function;
    }
}
