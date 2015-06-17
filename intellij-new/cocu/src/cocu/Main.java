package cocu;

import cocu.lang.Parser;
import cocu.lang.ast.AST;
import cocu.lang.ast.ASTVisitor;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        Parser parser = new Parser();
        AST ast = parser.parse("1 \"a string\"");

        ast.accept(new ASTVisitor<Object>() {
            @Override
            public Object visitProgram(List<AST> expressions) {
                expressions.forEach(e -> e.accept(this));

                return null;
            }

            @Override
            public Object visitInteger(int value) {
                return null;
            }

            @Override
            public Object visitString(String value) {
                return null;
            }
        });
    }
}
