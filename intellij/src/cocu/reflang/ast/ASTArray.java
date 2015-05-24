package cocu.reflang.ast;

import java.io.IOException;

import cocu.io.TreeWriter;

public class ASTArray implements AST {
	public final AST[] items;
	
	public ASTArray(AST[] items) {
		this.items = items;
	}

	@Override
	public void accept(ASTVisitor visitor) {
		visitor.visitArray(this);
	}
	
	@Override
	public void writeTo(TreeWriter writer) throws IOException {
		writer.write("#(");
		for(int i = 0; i < items.length; i++) {
			if(i > 0)
				writer.write(" ");
			items[i].writeTo(writer);
		}
		writer.write(")");
	}
}
