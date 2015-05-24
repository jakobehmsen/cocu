package cocu.reflang.ast;

import java.io.IOException;

import cocu.io.TreeWriter;

public class ASTMessageExchange implements AST {
	public final AST receiver;
	public final ASTMessage message;
	
	public ASTMessageExchange(AST receiver, ASTMessage message) {
		this.receiver = receiver;
		this.message = message;
	}

	@Override
	public void accept(ASTVisitor visitor) {
		visitor.visitMessageExchange(this);
	}
	
	@Override
	public void writeTo(TreeWriter writer) throws IOException {
		writer.write("(");
		receiver.writeTo(writer);
		writer.write(")");
		writer.write(".");
		message.writeTo(writer);
	}
}
