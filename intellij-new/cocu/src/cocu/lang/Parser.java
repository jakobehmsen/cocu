package cocu.lang;

import cocu.lang.antlr4.CocuBaseVisitor;
import cocu.lang.antlr4.CocuLexer;
import cocu.lang.antlr4.CocuParser;
import cocu.lang.ast.AST;
import cocu.lang.ast.ASTVisitor;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

public class Parser {
    public AST parse(String input) {
        try {
            return parse(new ByteArrayInputStream(input.getBytes()));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public AST parse(InputStream input) throws IOException {
        CharStream charStream = new ANTLRInputStream(input);
        CocuLexer lexer = new CocuLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        CocuParser parser = new CocuParser(tokenStream);

        CocuParser.ProgramContext programCtx = parser.program();

        return antlr4ToAst(programCtx);
    }

    private AST antlr4ToAst(ParserRuleContext ctx) {
        return ctx.accept(new CocuBaseVisitor<AST>() {
            @Override
            public AST visitProgram(@NotNull CocuParser.ProgramContext ctx) {
                List<AST> expressions = ctx.expression().stream().map(x -> x.accept(this)).collect(Collectors.toList());

                return new AST() {
                    @Override
                    public <T> T accept(ASTVisitor<? extends T> visitor) {
                        return visitor.visitProgram(expressions);
                    }
                };
            }

            @Override
            public AST visitInteger(@NotNull CocuParser.IntegerContext ctx) {
                int value = Integer.parseInt(ctx.getText());

                return new AST() {
                    @Override
                    public <T> T accept(ASTVisitor<? extends T> visitor) {
                        return visitor.visitInteger(value);
                    }
                };
            }

            @Override
            public AST visitString(@NotNull CocuParser.StringContext ctx) {
                String rawString = ctx.getText();
                String value = rawString.substring(1, rawString.length() - 1)
                    .replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t");

                return new AST() {
                    @Override
                    public <T> T accept(ASTVisitor<? extends T> visitor) {
                        return visitor.visitString(value);
                    }
                };
            }
        });
    }
}
