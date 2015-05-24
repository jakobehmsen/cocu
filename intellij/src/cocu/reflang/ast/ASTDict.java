package cocu.reflang.ast;

import java.io.IOException;

import cocu.io.TreeWriter;

public class ASTDict implements AST {
	public static class Entry {
		public final String id;
		public final int type;
		public final int arity;
		public final AST value;
		
		public Entry(String id, int type, int arity, AST value) {
			this.id = id;
			this.type = type;
			this.arity = arity;
			this.value = value;
		}
	}
	
	public final AST prototype;
	public final Entry[] entries;

	public ASTDict(AST prototype, Entry[] entries) {
		this.prototype = prototype;
		this.entries = entries;
	}

	@Override
	public void accept(ASTVisitor visitor) {
		visitor.visitDict(this);
	}
	
	@Override
	public void writeTo(TreeWriter writer) throws IOException {
		writer.write("#[");
		for(int i = 0; i < entries.length; i++) {
			if(i > 0)
				writer.writeln();
			writer.write(entries[i].id);
			writer.write("/");
			writer.write(entries[i].arity);
			switch(entries[i].type) {
			case ASTSlotAssignment.TYPE_REGULAR:
				writer.write(" = ");
				break;
			case ASTSlotAssignment.TYPE_PROTO:
				writer.write(" ^= ");
				break;
			case ASTSlotAssignment.TYPE_QUOTED:
				writer.write(" => ");
				break;
			}
			entries[i].value.writeTo(writer);
		}
		writer.write("]");
	}
}
