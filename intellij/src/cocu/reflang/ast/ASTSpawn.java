package cocu.reflang.ast;

import java.io.IOException;

import cocu.io.TreeWriter;

public class ASTSpawn implements AST {
	public final AST environment;
	public final ASTBehavior behavior;
	
	public ASTSpawn(AST environment, ASTBehavior behavior) {
		this.environment = environment;
		this.behavior = behavior;
	}

	@Override
	public void writeTo(TreeWriter writer) throws IOException {
		writer.write("#(");
		behavior.writeTo(writer);
		writer.write(")");
	}

	@Override
	public void accept(ASTVisitor visitor) {
		visitor.visitSpawn(this);
	}
}
