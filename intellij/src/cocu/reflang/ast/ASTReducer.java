package cocu.reflang.ast;

import java.util.function.Function;

public class ASTReducer implements ASTBuilder {
	private ASTBuilder[] builders;
	private Function<AST[], AST> reducer;
	
	public ASTReducer(ASTBuilder[] builders, Function<AST[], AST> reducer) {
		this.builders = builders;
		this.reducer = reducer;
	}

	@Override
	public AST build() {
		AST[] asts = new AST[builders.length];
		for(int i = 0; i < asts.length; i++)
			asts[i] = builders[i].build();
		return reducer.apply(asts);
	}
}
