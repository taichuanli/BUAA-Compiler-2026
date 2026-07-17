package frontEnd;

import frontEnd.lexer.LexType;

public final class LiteralValue {
    private LiteralValue() {
    }

    public static int parse(Token token) {
        if (token.isType(LexType.INTCON)) {
            return Integer.parseInt(token.token);
        }
        String value = token.token;
        if (value.charAt(1) != '\\') {
            return value.charAt(1);
        }
        return switch (value.charAt(2)) {
            case '0' -> 0;
            case 'a' -> 7;
            case 'b' -> 8;
            case 't' -> 9;
            case 'n' -> 10;
            case 'v' -> 11;
            case 'f' -> 12;
            case 'r' -> 13;
            case '"' -> 34;
            case '\'' -> 39;
            case '\\' -> 92;
            default -> value.charAt(2);
        };
    }
}
