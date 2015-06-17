package cocu.lang.ast;

public interface AST {
    <T> T accept(ASTVisitor<? extends T> visitor);
}
