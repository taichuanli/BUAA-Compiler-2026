import backEnd.MIPSGenerator;
import frontEnd.*;
import frontEnd.lexer.Lexer;
import frontEnd.parser.Parser;
import frontEnd.visitor.Visitor;
import middleEnd.IRGenerator;
import middleEnd.LLVMModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class Compiler {
    private static final String InputPath = "testfile.txt";
    private static final String LexerPath = "lexer.txt";
    private static final String ParserPath = "parser.txt";
    private static final String VisitorPath = "symbol.txt";
    private static final String IRPath = "llvm_ir.txt";
    private static final String MIPSPath = "mips.txt";

    public static void main(String[] args) {
        Reporter.REPORTER.reset();
        try {
            String content = new String(Files.readAllBytes(Paths.get(InputPath)));
            Lexer lexer = new Lexer(content);
            ArrayList<Token> tokens = lexer.analyze(LexerPath);
            Parser parser = new Parser(tokens);
            ASTNode node = parser.analyze(ParserPath);
            Visitor visitor = new Visitor(node);
            HashMap<Integer, SymbolTable> symbolTables = visitor.analyze(VisitorPath);
            node = Trimmer.instance.trim(node);
            Reporter.REPORTER.report();
            if (Reporter.REPORTER.hasError()) {
                return;
            }
            IRGenerator irGenerator = new IRGenerator(node, symbolTables);
            LLVMModule LLVMModule = irGenerator.translate(IRPath);
            MIPSGenerator mipsGenerator = new MIPSGenerator(LLVMModule);
            mipsGenerator.translate(MIPSPath);
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }
}
