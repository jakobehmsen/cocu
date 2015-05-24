package cocu.reflang.ast;

import java.io.IOException;

import cocu.io.TreeWriter;

public class ASTProgram implements AST {
	public final AST body;
	
	public ASTProgram(AST body) {
		this.body = body;
	}

	@Override
	public void accept(ASTVisitor visitor) {
		visitor.visitProgram(this);
	}
	
	@Override
	public void writeTo(TreeWriter writer) throws IOException {
		body.writeTo(writer);
	}
}
