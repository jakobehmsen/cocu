package cocu;

import cocu.lang.Parser;
import cocu.lang.ast.AST;
import cocu.lang.ast.ASTVisitor;

import java.util.Hashtable;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        Parser parser = new Parser();
        AST ast = parser.parse("var i = 1");

        Object res = ast.accept(new ASTVisitor<Object>() {
            private Hashtable<String, Object> variables = new Hashtable<>();

            @Override
            public Object visitProgram(List<AST> expressions) {
                expressions.subList(0, expressions.size() - 1).forEach(e -> e.accept(this));

                return expressions.get(expressions.size() - 1).accept(this);
            }

            @Override
            public Object visitInteger(int value) {
                return value;
            }

            @Override
            public Object visitString(String value) {
                return value;
            }

            @Override
            public Object visitVariableDefinition(boolean isDeclaration, String id, AST value) {
                if(value != null) {
                    Object v = value.accept(this);
                    variables.put(id, value);
                    return v;
                }

                return null;
            }
        });

        System.out.println(res);
    }
}
