package cocu.reflang.ast;

import java.io.IOException;

import cocu.io.TreeWriter;

public class ASTInteger implements AST {
	public final int integer;
	
	public ASTInteger(int integer) {
		this.integer = integer;
	}

	public void accept(ASTVisitor visitor) {
		visitor.visitInteger(this);
	}
	
	@Override
	public void writeTo(TreeWriter writer) throws IOException {
		writer.write(integer);
	}
}
