import frontEnd.Reporter;
import frontEnd.Token;
import frontEnd.lexer.Lexer;
import frontEnd.parser.Parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Compiler {
    private static final String InputPath = "testfile.txt";
    private static final String LexerPath = "lexer.txt";
    private static final String ParserPath = "parser.txt";

    public static void main(String[] args) {
        Reporter.REPORTER.reset();
        try {
            String content = Files.readString(Paths.get(InputPath));
            Lexer lexer = new Lexer(content);
            ArrayList<Token> tokens = lexer.analyze(LexerPath);
            Parser parser = new Parser(tokens);
            parser.analyze(ParserPath);
            Reporter.REPORTER.report();
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }
}
