package cocu.reflang.ast;

import java.io.IOException;

import cocu.io.TreeWriter;

public class ASTBehavior implements AST {
	public final int localCount;
	public final AST body;
	
	public ASTBehavior(int localCount, AST body) {
		this.localCount = localCount;
		this.body = body;
	}

	@Override
	public void accept(ASTVisitor visitor) {
		visitor.visitBehavior(this);
	}

	@Override
	public void writeTo(TreeWriter writer) throws IOException {
		writer.write("(");
		writer.write(localCount);
		writer.write("|");
		writer.indentnl();
		body.writeTo(writer);
		writer.dedentnl();
		writer.write(")");
	}
}
