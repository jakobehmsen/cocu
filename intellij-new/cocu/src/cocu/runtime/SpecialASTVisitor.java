package cocu.runtime;

import cocu.lang.ast.AST;
import cocu.lang.ast.ASTVisitor;

import java.util.Map;
import java.util.function.Function;

public interface SpecialASTVisitor<T> extends ASTVisitor<T> {
    T visitQuote(AST ast);
    T visitReceive();
    T visitReply(AST envelope, AST value);
    T visitMatch(AST target, Map<Object, AST> table);
    T visitSignal(AST astSignal);
    T visitResumeWith(AST astContext, AST astValue);
    T visitTryCatch(AST astBody, Function<Environment, Closure> closureConstructor);
}
