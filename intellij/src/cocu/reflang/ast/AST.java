package cocu.reflang.ast;

import cocu.io.WritableTree;

public interface AST extends WritableTree {
	void accept(ASTVisitor visitor);
}
