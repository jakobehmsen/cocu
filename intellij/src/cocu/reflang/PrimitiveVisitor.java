package cocu.reflang;

import cocu.reflang.ast.AST;

public interface PrimitiveVisitor {
	void visitPrimitive(String id, AST[] args);
}
