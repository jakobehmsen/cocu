package cocu.reflang.ast;

import java.io.IOException;

import cocu.io.TreeWriter;

public class ASTInterfaceId implements AST {
	public final String id;
	public final AST body;
	
	public ASTInterfaceId(String id, AST body) {
		this.id = id;
		this.body = body;
	}

	@Override
	public void accept(ASTVisitor visitor) {
		visitor.visitInterfaceId(this);
	}
	
	@Override
	public void writeTo(TreeWriter writer) throws IOException {
		writer.write("$");
		writer.write(id);
		writer.indentnl();
		body.writeTo(writer);
		writer.dedentnl();
	}
}
