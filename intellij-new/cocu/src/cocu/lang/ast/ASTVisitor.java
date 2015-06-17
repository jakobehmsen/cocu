package cocu.lang.ast;

import java.util.List;

public interface ASTVisitor<T> {
    T visitProgram(List<AST> expressions);

    T visitInteger(int value);

    T visitString(String value);
}
