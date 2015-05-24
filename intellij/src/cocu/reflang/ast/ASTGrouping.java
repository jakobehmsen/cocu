package cocu.reflang.ast;

import java.io.IOException;

import cocu.io.TreeWriter;

public class ASTGrouping implements AST {
	public final AST[] items;
	
	public ASTGrouping(AST[] items) {
		this.items = items;
	}

	@Override
	public void accept(ASTVisitor visitor) {
		visitor.visitGrouping(this);
	}

	@Override
	public void writeTo(TreeWriter writer) throws IOException {
		if(writer.depth() > 0)
			writer.write("(");
		
		if(items.length > 1) {
			if(writer.depth() > 0)
				writer.indentnl();
			for(int i = 0; i < items.length; i++) {
				if(i > 0)
					writer.writeln();
				items[i].writeTo(writer);
			}
			if(writer.depth() > 0)
				writer.dedentnl();
		} else if(items.length == 1) {
			items[0].writeTo(writer);
		}
		
		if(writer.depth() > 0)
			writer.write(")");
	}
}
