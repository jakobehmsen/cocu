package cocu.lang.ast;

import java.util.List;

public interface ASTVisitor<T> {
    T visitProgram(List<AST> expressions);

    T visitInteger(int value);

    T visitString(String value);

    T visitVariableDefinition(boolean isDeclaration, String id, AST value);

    T visitEnvironmentMessage(String selector, List<AST> args);

    T visitSpawn(AST environment, List<AST> expressions);

    T visitVariableUsage(String id);

    T visitMessageSend(AST receiver, String selector, List<AST> args);

    T visitClosure(List<String> parameters, AST body);
}
