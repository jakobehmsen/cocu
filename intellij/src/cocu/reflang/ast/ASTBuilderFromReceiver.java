package cocu.reflang.ast;

public abstract class ASTBuilderFromReceiver implements ASTBuilder {
	@Override
	public AST build() {
//		return createBuilder(() -> ASTThis.INSTANCE).build();
		return createBuilder(() -> ASTImplicitReceiver.INSTANCE).build();
	}
	
	public abstract ASTBuilder createBuilder(ASTBuilder receiver);
}
