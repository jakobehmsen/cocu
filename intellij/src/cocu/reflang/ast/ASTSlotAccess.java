package cocu.reflang.ast;

import java.io.IOException;

import cocu.io.TreeWriter;

public class ASTSlotAccess implements AST {
	public final AST receiver;
	public final String id;
	public final int arity;

	public ASTSlotAccess(AST receiver, String id, int arity) {
		this.receiver = receiver;
		this.id = id;
		this.arity = arity;
	}

	@Override
	public void accept(ASTVisitor visitor) {
		visitor.visitSlotAccess(this);
	}
	
	@Override
	public void writeTo(TreeWriter writer) throws IOException {
		receiver.writeTo(writer);
		writer.write("@");
		writer.write(id);
		writer.write("/");
		writer.write(arity);
	}
}
