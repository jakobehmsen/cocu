package cocu.reflang.ast;

import java.io.IOException;

import cocu.io.TreeWriter;

public class ASTMessage implements AST {
	public final String id;
	public final AST[] arguments;
	
	public ASTMessage(String id, AST[] arguments) {
		this.id = id;
		this.arguments = arguments;
	}

	@Override
	public void accept(ASTVisitor visitor) {
		visitor.visitMessage(this);
	}
	
	@Override
	public void writeTo(TreeWriter writer) throws IOException {
		writer.write("(");
		writer.write(id);
		for(int i = 0; i < arguments.length; i++) {
			writer.write(" ");
			arguments[i].writeTo(writer);
		}
		writer.write(")");
	}
}
