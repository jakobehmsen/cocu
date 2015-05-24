package cocu.reflang.ast;

import java.io.IOException;

import cocu.io.TreeWriter;

public class ASTClosure implements AST {
	public final int parameterOffset;
	public final int parameterCount;
	public final AST body;
	
	public ASTClosure(int parameterOffset, int parameterCount, AST body) {
		this.parameterOffset = parameterOffset;
		this.parameterCount = parameterCount;
		this.body = body;
	}

	@Override
	public void accept(ASTVisitor visitor) {
		visitor.visitClosure(this);
	}

	@Override
	public void writeTo(TreeWriter writer) throws IOException {
		writer.write("{");
		writer.write(parameterOffset);
		writer.write(",");
		writer.write(parameterCount);
		writer.write("|");
		writer.indentnl();
		body.writeTo(writer);
		writer.dedentnl();
		writer.write("}");
	}
}
