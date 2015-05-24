package cocu.reflang.ast;

import java.io.IOException;

import cocu.io.TreeWriter;

public class ASTLocalAccess implements AST {
	public final int ordinal;
	
	public ASTLocalAccess(int ordinal) {
		this.ordinal = ordinal;
	}

	@Override
	public void accept(ASTVisitor visitor) {
		visitor.visitLocalAccess(this);
	}
	
	@Override
	public void writeTo(TreeWriter writer) throws IOException {
		writer.write("&");
		writer.write(ordinal);
	}
}
