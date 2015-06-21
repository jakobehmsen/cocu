package cocu.runtime;

import cocu.lang.ast.AST;
import cocu.lang.ast.ASTVisitor;

public interface SpecialAST extends AST {
    default <T> T accept(ASTVisitor<? extends T> visitor) {
        throw new UnsupportedOperationException();
    }

    <T> T acceptSpecial(SpecialASTVisitor<? extends T> visitor);
}