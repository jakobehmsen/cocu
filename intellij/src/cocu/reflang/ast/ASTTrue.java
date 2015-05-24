package cocu.reflang.ast;

import java.io.IOException;

import cocu.io.TreeWriter;

public class ASTTrue implements AST {
	public static final ASTTrue INSTANCE = new ASTTrue();
	
	@Override
	public void accept(ASTVisitor visitor) {
		visitor.visitTrue(this);
	}
	
	@Override
	public void writeTo(TreeWriter writer) throws IOException {
		writer.write("true");
	}
}
