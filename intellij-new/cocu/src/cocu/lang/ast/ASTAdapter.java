package cocu.lang.ast;

import java.util.List;

public class ASTAdapter<T> implements ASTVisitor<T> {
    @Override
    public T visitProgram(List<AST> expressions) {
        return null;
    }

    @Override
    public T visitInteger(int value) {
        return null;
    }

    @Override
    public T visitString(String value) {
        return null;
    }

    @Override
    public T visitVariableDefinition(boolean isDeclaration, String id, AST value) {
        return null;
    }

    @Override
    public T visitEnvironmentMessage(String selector, List<AST> args) {
        return null;
    }

    @Override
    public T visitSpawn(AST environment, List<AST> expressions) {
        return null;
    }

    @Override
    public T visitVariableUsage(String id) {
        return null;
    }

    @Override
    public T visitMessageSend(AST receiver, String selector, List<AST> args) {
        return null;
    }

    @Override
    public T visitClosure(List<String> parameters, AST body) {
        return null;
    }
}
