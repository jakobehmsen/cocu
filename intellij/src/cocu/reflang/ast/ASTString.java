package cocu.reflang.ast;

import java.io.IOException;

import cocu.io.TreeWriter;

public class ASTString implements AST {
	public final String string;
	
	public ASTString(String string) {
		this.string = string;
	}

	public void accept(ASTVisitor visitor) {
		visitor.visitString(this);
	}
	
	@Override
	public void writeTo(TreeWriter writer) throws IOException {
		writer.write("\"");
		writer.write(string);
		writer.write("\"");
	}
}
