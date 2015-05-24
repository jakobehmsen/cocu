package cocu.reflang.ast;

import java.io.IOException;

import cocu.io.TreeWriter;

public class ASTLocalAssignment implements AST {
	public final int ordinal;
	public final AST value;
	
	public ASTLocalAssignment(int ordinal, AST value) {
		this.ordinal = ordinal;
		this.value = value;
	}

	@Override
	public void accept(ASTVisitor visitor) {
		visitor.visitLocalAssignment(this);
	}
	
	@Override
	public void writeTo(TreeWriter writer) throws IOException {
		writer.write("&");
		writer.write(ordinal);
		writer.write(" = ");
		value.writeTo(writer);
	}
}
