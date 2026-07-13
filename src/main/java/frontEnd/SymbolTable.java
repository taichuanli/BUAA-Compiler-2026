package frontEnd;

import java.util.ArrayList;
import java.util.HashMap;

public class SymbolTable {
    public int id;
    public SymbolTable parentTable;
    public HashMap<String, Symbol> symbols = new HashMap<>();

    public SymbolTable(int scopeId, SymbolTable currTable) {
        this.id = scopeId;
        this.parentTable = currTable;
    }

    public boolean hasSymbol(String name) {
        return symbols.containsKey(name);
    }

    public void addSymbol(Symbol symbol) {
        symbols.put(symbol.token.token, symbol);
    }

    public ArrayList<Symbol> getAllSymbols() {
        return new ArrayList<>(symbols.values());
    }

    public Symbol getSymbol(String token) {
        return symbols.get(token);
    }
}
