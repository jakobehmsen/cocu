package cocu.reflang.ast;

import java.io.IOException;

import cocu.io.TreeWriter;

public class ASTThis implements AST {
	public static final ASTThis INSTANCE = new ASTThis();
	
	@Override
	public void accept(ASTVisitor visitor) {
		visitor.visitThis(this);
	}
	
	@Override
	public void writeTo(TreeWriter writer) throws IOException {
		writer.write("this");
	}
}
