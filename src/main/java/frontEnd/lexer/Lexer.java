package frontEnd.lexer;

import frontEnd.MyError;
import frontEnd.Reporter;
import frontEnd.Token;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class Lexer {
    private final String source;
    private int curPos; // 姑且认为 int 类型足够
    private String curToken;
    private LexType curType;
    private int lineNum;
    private final HashMap<String, String> reservedWords = new HashMap<>() {
        {
            put("main", "MAINTK");
            put("const", "CONSTTK");
            put("int", "INTTK");
            put("char", "CHARTK");
            put("break", "BREAKTK");
            put("continue", "CONTINUETK");
            put("if", "IFTK");
            put("else", "ELSETK");
            put("for", "FORTK");
            put("getint", "GETINTTK");
            put("getchar", "GETCHARTK");
            put("printf", "PRINTFTK");
            put("return", "RETURNTK");
            put("void", "VOIDTK");
        }
    };

    public Lexer(String source) {
        this.source = source;
        this.curPos = 0;
        this.curToken = null;
        this.curType = null;
        this.lineNum = 1;
    }

    public ArrayList<Token> analyze(String forOutput) {
        ArrayList<Token> res = new ArrayList<>();
        do {
            nextToken();
            if (curType != LexType.NOTE && curToken != null) {
                res.add(new Token(curType, curToken, lineNum));
            }
        } while (curToken != null);

        // 将 res 写入 forOutput 文件
        try (FileWriter writer = new FileWriter(forOutput)) {
            for (Token token : res) {
                writer.write(token + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
        return res;
    }

    public void nextToken() {
        // 更新 curToken 和 curType
        while (curPos < source.length() && (source.charAt(curPos) == ' ' || source.charAt(curPos) == '\t' || source.charAt(curPos) == '\n' || source.charAt(curPos) == '\r')) {
            if (source.charAt(curPos) == '\n') {
                lineNum++;
            }
            curPos++;
        }
        if (curPos >= source.length()) {
            curToken = null;
            return;
        }
        if (Character.isDigit(source.charAt(curPos))) {
            parseNumber();
        } else if (isIdentifierFirstLetter(source.charAt(curPos))) {
            parseIdentifierOrReservedWord();
        } else if (source.charAt(curPos) == '\"') {
            parseString();
        } else if (source.charAt(curPos) == '\'') {
            parseChar();
        } else {
            parseSign();
        }
    }

    private void parseNumber() {
        StringBuilder sb = new StringBuilder();
        if (source.charAt(curPos) == '0') {
            sb.append(source.charAt(curPos));
            curPos++;
        } else {
            while (curPos < source.length() && Character.isDigit(source.charAt(curPos))) {
                sb.append(source.charAt(curPos));
                curPos++;
            }
        }
        curType = LexType.INTCON;
        curToken = sb.toString();
    }

    private boolean isIdentifierFirstLetter(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private void parseIdentifierOrReservedWord() {
        StringBuilder sb = new StringBuilder();
        while (curPos < source.length() && (Character.isLetterOrDigit(source.charAt(curPos)) || source.charAt(curPos) == '_')) {
            sb.append(source.charAt(curPos));
            curPos++;
        }
        String identifier = sb.toString();
        if (reservedWords.containsKey(identifier)) {
            curType = LexType.valueOf(reservedWords.get(identifier));
        } else {
            curType = LexType.IDENFR;
        }
        curToken = identifier;
    }

    private boolean isLegalLetter(char c) {
        return (c >= 32 && c <= 126) || (c == 0);
    }

    private void parseString() {
        StringBuilder sb = new StringBuilder();
        do {
            sb.append(source.charAt(curPos));
            curPos++;
            if (curPos < source.length() && source.charAt(curPos - 1) == '\\') {
                sb.append(source.charAt(curPos));
                curPos++;
            }
        } while (curPos < source.length() && source.charAt(curPos) != '\"' && isLegalLetter(source.charAt(curPos)));
        sb.append(source.charAt(curPos));
        curPos++;
        curType = LexType.STRCON;
        curToken = sb.toString();
    }

    private void parseChar() {
        StringBuilder sb = new StringBuilder();
        sb.append(source.charAt(curPos));
        curPos++;
        if (curPos < source.length() && isLegalLetter(source.charAt(curPos))) {
            sb.append(source.charAt(curPos));
            curPos++;
            if (curPos < source.length() && source.charAt(curPos - 1) == '\\') {
                sb.append(source.charAt(curPos));
                curPos++;
            }
        }
        if (curPos < source.length() && source.charAt(curPos) == '\'') {
            sb.append(source.charAt(curPos));
            curPos++;
            curType = LexType.CHRCON;
            curToken = sb.toString();
        }
    }

    private void parseSign() {
        switch (source.charAt(curPos)) {
            case '!' -> {
                curPos++;
                if (curPos < source.length() && source.charAt(curPos) == '=') {
                    curToken = "!=";
                    curType = LexType.NEQ;
                    curPos++;
                } else {
                    curToken = "!";
                    curType = LexType.NOT;
                }
            }
            case '<' -> {
                curPos++;
                if (curPos < source.length() && source.charAt(curPos) == '=') {
                    curToken = "<=";
                    curType = LexType.LEQ;
                    curPos++;
                } else {
                    curToken = "<";
                    curType = LexType.LSS;
                }
            }
            case '>' -> {
                curPos++;
                if (curPos < source.length() && source.charAt(curPos) == '=') {
                    curToken = ">=";
                    curType = LexType.GEQ;
                    curPos++;
                } else {
                    curToken = ">";
                    curType = LexType.GRE;
                }
            }
            case '=' -> {
                curPos++;
                if (curPos < source.length() && source.charAt(curPos) == '=') {
                    curToken = "==";
                    curType = LexType.EQL;
                    curPos++;
                } else {
                    curToken = "=";
                    curType = LexType.ASSIGN;
                }
            }
            case '+' -> {
                curToken = "+";
                curType = LexType.PLUS;
                curPos++;
            }
            case '-' -> {
                curToken = "-";
                curType = LexType.MINU;
                curPos++;
            }
            case '*' -> {
                curToken = "*";
                curType = LexType.MULT;
                curPos++;
            }
            case '%' -> {
                curToken = "%";
                curType = LexType.MOD;
                curPos++;
            }
            case ';' -> {
                curToken = ";";
                curType = LexType.SEMICN;
                curPos++;
            }
            case ',' -> {
                curToken = ",";
                curType = LexType.COMMA;
                curPos++;
            }
            case '(' -> {
                curToken = "(";
                curType = LexType.LPARENT;
                curPos++;
            }
            case ')' -> {
                curToken = ")";
                curType = LexType.RPARENT;
                curPos++;
            }
            case '[' -> {
                curToken = "[";
                curType = LexType.LBRACK;
                curPos++;
            }
            case ']' -> {
                curToken = "]";
                curType = LexType.RBRACK;
                curPos++;
            }
            case '{' -> {
                curToken = "{";
                curType = LexType.LBRACE;
                curPos++;
            }
            case '}' -> {
                curToken = "}";
                curType = LexType.RBRACE;
                curPos++;
            }
            case '|' -> {
                curPos++;
                if (curPos < source.length() && source.charAt(curPos) == '|') {
                    curToken = "||";
                    curType = LexType.OR;
                    curPos++;
                } else {
                    curToken = "|";
                    curType = LexType.OR;
                    Reporter.REPORTER.add(new MyError(lineNum, "a"));
                }
            }
            case '&' -> {
                curPos++;
                if (curPos < source.length() && source.charAt(curPos) == '&') {
                    curToken = "&&";
                    curType = LexType.AND;
                    curPos++;
                }
                else {
                    curToken = "&";
                    curType = LexType.AND;
                    Reporter.REPORTER.add(new MyError(lineNum, "a"));
                }
            }
            case '/' -> parseNote();
        }
    }

    private void parseNote() {
        StringBuilder sb = new StringBuilder();
        sb.append(source.charAt(curPos++));
        if (curPos < source.length() && source.charAt(curPos) == '/') {
            do {
                sb.append(source.charAt(curPos++));
            } while (curPos < source.length() && source.charAt(curPos) != '\n');
            if (curPos < source.length()) {
                sb.append(source.charAt(curPos++));
                lineNum++;
            }
            curToken = sb.toString();
            curType = LexType.NOTE;
        } else if (curPos < source.length() && source.charAt(curPos) == '*') {
            sb.append(source.charAt(curPos++));
            while (curPos < source.length()) {
                while (curPos < source.length() && source.charAt(curPos) != '*') {
                    if (source.charAt(curPos) == '\n') {
                        lineNum++;
                    }
                    sb.append(source.charAt(curPos++));
                }
                while (curPos < source.length() && source.charAt(curPos) == '*') {
                    sb.append(source.charAt(curPos++));
                }
                if (curPos < source.length() && source.charAt(curPos) == '/') {
                    sb.append(source.charAt(curPos++));
                    curToken = sb.toString();
                    curType = LexType.NOTE;
                    break;
                }
            }
        } else {
            curToken = "/";
            curType = LexType.DIV;
        }
    }
}
