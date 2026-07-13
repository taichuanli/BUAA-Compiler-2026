package frontEnd;

import frontEnd.lexer.LexType;

public class Token {
    public final LexType type;
    public final String token;
    public final int lineNum;
    public Token(LexType curType, String curToken, int lineNum) {
        this.type = curType;
        this.token = curToken;
        this.lineNum = lineNum;
    }

    public boolean isType(LexType type) {
        return this.type == type;
    }

    @Override
    public String toString() {
        return type + " " + token;
    }
}
