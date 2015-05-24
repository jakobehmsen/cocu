package cocu.reflang.ast;

import java.io.IOException;

import cocu.io.TreeWriter;

public class ASTImplicitReceiver implements AST {
	public static final ASTImplicitReceiver INSTANCE = new ASTImplicitReceiver();
	
	private ASTImplicitReceiver() { }

	@Override
	public void writeTo(TreeWriter writer) throws IOException { }

	@Override
	public void accept(ASTVisitor visitor) {
		visitor.visitImplicitReceiver(this);
	}
}
