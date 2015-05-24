package cocu.reflang.ast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.omg.CORBA.IntHolder;

import cocu.reflang.MessageCollector;
import cocu.reflang.OrdinalAllocator;
import cocu.reflang.antlr4.CocuBaseVisitor;
import cocu.reflang.antlr4.CocuLexer;
import cocu.reflang.antlr4.CocuParser.AccessContext;
import cocu.reflang.antlr4.CocuParser.ArrayContext;
import cocu.reflang.antlr4.CocuParser.AssignmentContext;
import cocu.reflang.antlr4.CocuParser.AssignmentOperatorContext;
import cocu.reflang.antlr4.CocuParser.BehaviorParamsContext;
import cocu.reflang.antlr4.CocuParser.BinaryMessageArgContext;
import cocu.reflang.antlr4.CocuParser.BinaryMessageChainContext;
import cocu.reflang.antlr4.CocuParser.BinaryMessageContext;
import cocu.reflang.antlr4.CocuParser.ClosureContext;
import cocu.reflang.antlr4.CocuParser.DictContext;
import cocu.reflang.antlr4.CocuParser.DictEntryContext;
import cocu.reflang.antlr4.CocuParser.ExpressionContext;
import cocu.reflang.antlr4.CocuParser.ExpressionReceiverContext;
import cocu.reflang.antlr4.CocuParser.GroupingContext;
import cocu.reflang.antlr4.CocuParser.IdContext;
import cocu.reflang.antlr4.CocuParser.IndexAccessContext;
import cocu.reflang.antlr4.CocuParser.IndexAssignmentContext;
import cocu.reflang.antlr4.CocuParser.IntegerContext;
import cocu.reflang.antlr4.CocuParser.InterfaceIdContext;
import cocu.reflang.antlr4.CocuParser.MessageExchangeContext;
import cocu.reflang.antlr4.CocuParser.MultiKeyMessageArgContext;
import cocu.reflang.antlr4.CocuParser.MultiKeyMessageContext;
import cocu.reflang.antlr4.CocuParser.MultiKeyMessageTailContext;
import cocu.reflang.antlr4.CocuParser.ParArgContext;
import cocu.reflang.antlr4.CocuParser.ProgramContext;
import cocu.reflang.antlr4.CocuParser.PseudoVarContext;
import cocu.reflang.antlr4.CocuParser.SelectorContext;
import cocu.reflang.antlr4.CocuParser.SelfMultiKeyMessageContext;
import cocu.reflang.antlr4.CocuParser.SelfSingleKeyMessageContext;
import cocu.reflang.antlr4.CocuParser.SingleKeyMessageContext;
import cocu.reflang.antlr4.CocuParser.SlotAccessContext;
import cocu.reflang.antlr4.CocuParser.SlotAssignmentContext;
import cocu.reflang.antlr4.CocuParser.SpawnContext;
import cocu.reflang.antlr4.CocuParser.StringContext;
import cocu.reflang.antlr4.CocuParser.UnaryMessageContext;
import cocu.reflang.antlr4.CocuParser.VariableDeclarationContext;

public class ANTLRToAST extends CocuBaseVisitor<ASTBuilder> {
	private OrdinalAllocator idToParameterOrdinalMap;
	private OrdinalAllocator idToVariableOrdinalMap;
	private MessageCollector errors;
	private Set<String> accessFields;
	private Set<String> assignFields;
	
	public ANTLRToAST(OrdinalAllocator idToParameterOrdinalMap, OrdinalAllocator idToVariableOrdinalMap, MessageCollector errors, Set<String> accessFields, Set<String> assignFields) {
		this.idToParameterOrdinalMap = idToParameterOrdinalMap;
		this.idToVariableOrdinalMap = idToVariableOrdinalMap;
		this.errors = errors;
		this.accessFields = accessFields;
		this.assignFields = assignFields;
	}

	@Override
	public ASTBuilder visitProgram(ProgramContext ctx) {
		ASTBuilder bodyBuilder = groupingBuilder(ctx.expression());
		return () -> new ASTProgram(bodyBuilder.build());
	}
	
	@Override
	public ASTBuilder visitGrouping(GroupingContext ctx) {
		return groupingBuilder(ctx.expression());
	}
	
	private ASTBuilder groupingBuilder(List<ExpressionContext> expressions) {
		ASTBuilder[] itemBuilders = new ASTBuilder[expressions.size()];
		for(int i = 0; i < itemBuilders.length; i++)
			itemBuilders[i] = expressions.get(i).accept(this);
		return new ASTReducer(itemBuilders, asts -> new ASTGrouping(asts));
	}
	
	@Override
	public ASTBuilder visitInterfaceId(InterfaceIdContext ctx) {
		String id = ctx.id().getText();
		idToVariableOrdinalMap = idToVariableOrdinalMap.newInnerStart();
		ASTBuilder bodyBuilder = ctx.expression().accept(this);
		idToVariableOrdinalMap = idToVariableOrdinalMap.getOuter();
		
		return () -> new ASTInterfaceId(id, bodyBuilder.build());
	}
	
	@Override
	public ASTBuilder visitMessageExchange(MessageExchangeContext ctx) {
		return messageExchangeBuilder(ctx.receiver(), ctx.messageChain().stream().map(x -> (ParserRuleContext)x).collect(Collectors.toList()), ctx.messageEnd());
	}
	
	private ASTBuilder messageExchangeBuilder(ParserRuleContext receiver, List<ParserRuleContext> chain, ParserRuleContext end) {
		ASTBuilder receiverBuilder = receiver.accept(this);
		
		for(ParserRuleContext chainPart: chain) {
			ASTBuilderFromReceiver messageBuilder = (ASTBuilderFromReceiver)chainPart.accept(this);
			receiverBuilder = messageBuilder.createBuilder(receiverBuilder);
		}
		
		if(end != null) {
			ASTBuilderFromReceiver messageBuilder = (ASTBuilderFromReceiver)end.accept(this);
			receiverBuilder = messageBuilder.createBuilder(receiverBuilder);
		}
		
		return receiverBuilder;
	}
	
	@Override
	public ASTBuilder visitExpression(ExpressionContext ctx) {
		return messageExchangeBuilder(
			ctx.expressionReceiver(), ctx.expressionChain().stream().map(x -> (ParserRuleContext)x).collect(Collectors.toList()), null);
	}
	
	@Override
	public ASTBuilder visitMultiKeyMessageArg(MultiKeyMessageArgContext ctx) {
		if(ctx.selfSingleKeyMessage() != null) {
			return ctx.selfSingleKeyMessage().accept(this);
		} else {
			return messageExchangeBuilder(
				ctx.multiKeyMessageArgReceiver(), 
				ctx.multiKeyMessageArgChain().stream().map(x -> (ParserRuleContext)x).collect(Collectors.toList()), 
				ctx.multiKeyMessageArgEnd());
		}
	}
	
	@Override
	public ASTBuilder visitBinaryMessageArg(BinaryMessageArgContext ctx) {
		return messageExchangeBuilder(
			ctx.receiver(), ctx.binaryMessageArgChain().stream().map(x -> (ParserRuleContext)x).collect(Collectors.toList()), ctx.binaryMessageArgEnd());
	}
	
	@Override
	public ASTBuilder visitVariableDeclaration(VariableDeclarationContext ctx) {
		String id = ctx.id().getText();
		
		if(!idToVariableOrdinalMap.isDeclaredLocally(id) && !idToParameterOrdinalMap.isDeclared(id)) {
			idToVariableOrdinalMap.declare(id);

			if(ctx.expression() != null)
				return assignVariableBuilder(ctx.expression(), id);
		} else {
			appendError(ctx, "Variable '" + ctx.id().getText() + "' is already declared in this scope.");
		}
		
		return () -> ASTEmpty.INSTANCE;
	}
	
	@Override
	public ASTBuilder visitBinaryMessageChain(BinaryMessageChainContext ctx) {
		// Generation of left-to-right evaluation logic of binary message chain
		ArrayList<ASTBuilderFromReceiver> messageBuilders = new ArrayList<ASTBuilderFromReceiver>();
		
		for(BinaryMessageContext binMsgCtx: ctx.binaryMessage()) {
			String id = binMsgCtx.BIN_OP().getText();
			ASTBuilderFromReceiver messageBuilder = multiKeyMessageFromReceiverBuilder(id, Arrays.asList(binMsgCtx.binaryMessageArg()));
			messageBuilders.add(messageBuilder);
		}
		
		return new ASTBuilderFromReceiver() {
			@Override
			public ASTBuilder createBuilder(ASTBuilder receiver) {
				ASTBuilder builder = null;
				for(ASTBuilderFromReceiver messageBuilder: messageBuilders) {
					builder = messageBuilder.createBuilder(receiver);
					receiver = builder;
				}
				return builder;
			}
		};
	}
	
	@Override
	public ASTBuilder visitIndexAccess(IndexAccessContext ctx) {
		String id = "[]";
		
		return multiKeyMessageFromReceiverBuilder(id, Arrays.asList(ctx.expression()));
	}
	

	@Override 
	public ASTBuilder visitIndexAssignment(IndexAssignmentContext ctx) { 
//		return visitChildren(ctx);
		String id = "[]";
		
		return multiKeyMessageFromReceiverBuilder(id, ctx.expression().stream().map(x -> (ParserRuleContext)x).collect(Collectors.toList()));
	}
	
	@Override
	public ASTBuilder visitSelfMultiKeyMessage(SelfMultiKeyMessageContext ctx) {
		return multiKeyMessageFromReceiverBuilder(ctx.multiKeyMessage());
	}
	
	@Override
	public ASTBuilderFromReceiver visitMultiKeyMessage(MultiKeyMessageContext ctx) {
		return multiKeyMessageFromReceiverBuilder(ctx);
	}
	
	private ASTBuilderFromReceiver multiKeyMessageFromReceiverBuilder(MultiKeyMessageContext ctx) {
		String id = 
			ctx.multiKeyMessageHead().ID_UNCAP().getText() + 
			ctx.multiKeyMessageTail().stream().map(x -> x.ID_CAP().getText()).collect(Collectors.joining());
		ArrayList<ParserRuleContext> args = new ArrayList<ParserRuleContext>();
		
		for(ParserRuleContext argCtx: ctx.multiKeyMessageHead().multiKeyMessageArgs().multiKeyMessageArg()) {
			if(ctx.multiKeyMessageHead().multiKeyMessageModifier().modifier.getType() == CocuLexer.SINGLE_QUOTE) {
				// Wrap arg into closure
				argCtx = wrapIntoClosure(ctx, ctx.invokingState, ctx.multiKeyMessageHead().multiKeyMessageModifier().behaviorParams(), argCtx);
			}
			args.add(argCtx);
		}

		for(MultiKeyMessageTailContext tailCtx: ctx.multiKeyMessageTail()) {
			for(ParserRuleContext argCtx: tailCtx.multiKeyMessageArgs().multiKeyMessageArg()) {
				if(tailCtx.multiKeyMessageModifier().modifier.getType() == CocuLexer.SINGLE_QUOTE) {
					// Wrap arg into closure
					argCtx = wrapIntoClosure(ctx, ctx.invokingState, tailCtx.multiKeyMessageModifier().behaviorParams(), argCtx);
				}
				args.add(argCtx);
			}
		}
		
		return multiKeyMessageFromReceiverBuilder(id, args);
	}	
	
	protected ASTBuilderFromReceiver multiKeyMessageFromReceiverBuilder(String id, List<ParserRuleContext> args) {
		int parameterCount = args.size();
		
		ASTBuilder[] argumentBuilders = new ASTBuilder[parameterCount];
		for(int i = 0; i < argumentBuilders.length; i++) {
			// For each argument, a local scope should be allocated
			idToVariableOrdinalMap = idToVariableOrdinalMap.newInnerStart();
			argumentBuilders[i] = args.get(i).accept(this);
			idToVariableOrdinalMap = idToVariableOrdinalMap.getOuter();
		}
		
		return new ASTBuilderFromReceiver() {
			@Override
			public ASTBuilder createBuilder(ASTBuilder receiverBuilder) {
				return new ASTReducer(argumentBuilders, argumentAsts -> new ASTMessageExchange(receiverBuilder.build(), new ASTMessage(id, argumentAsts))); 
			}
		};
	}
	
	private ParserRuleContext wrapIntoClosure(ParserRuleContext parent, int invokingState, BehaviorParamsContext paramsCtx, ParserRuleContext argCtx) {
		ClosureContext argClosureCtx = new ClosureContext(parent, invokingState);
		argClosureCtx.addChild(paramsCtx);
		ExpressionContext body = new ExpressionContext(argClosureCtx, invokingState);
		ExpressionReceiverContext bodyReceiver = new ExpressionReceiverContext(body, invokingState);
		bodyReceiver.addChild(argCtx);
		body.addChild(bodyReceiver);
		argClosureCtx.addChild(body);
		return argClosureCtx;
	}
	
	@Override
	public ASTBuilder visitSelfSingleKeyMessage(SelfSingleKeyMessageContext ctx) {
		return singleKeyMessageBuilderFromReceiver(ctx.singleKeyMessage());
	}
	
	@Override
	public ASTBuilder visitSingleKeyMessage(SingleKeyMessageContext ctx) {
		return singleKeyMessageBuilderFromReceiver(ctx);
	}

	private ASTBuilderFromReceiver singleKeyMessageBuilderFromReceiver(SingleKeyMessageContext ctx) {
		String id = ctx.ID_UNCAP().getText();
		ArrayList<ParserRuleContext> args = new ArrayList<ParserRuleContext>();
		ParserRuleContext argCtx = ctx.multiKeyMessageArg();
		if(ctx.multiKeyMessageModifier().modifier.getType() == CocuLexer.SINGLE_QUOTE) {
			// Wrap arg into closure
			argCtx = wrapIntoClosure(ctx, ctx.invokingState, ctx.multiKeyMessageModifier().behaviorParams(), argCtx);
		}
		args.add(argCtx);
		
		return multiKeyMessageFromReceiverBuilder(id, args);
	}

	@Override
	public ASTBuilder visitUnaryMessage(UnaryMessageContext ctx) {
		String id = ctx.ID_UNCAP().getText();
		ArrayList<ParserRuleContext> args = new ArrayList<ParserRuleContext>();;
		
		return multiKeyMessageFromReceiverBuilder(id, args);
	}
	
	@Override
	public ASTBuilder visitAssignment(AssignmentContext ctx) {
		String id = ctx.id().getText();				

		if(ctx.assignmentOperator().op.getType() == CocuLexer.ASSIGN) {
			if(idToVariableOrdinalMap.isDeclared(id))
				return assignVariableBuilder(ctx.expression(), id);
		}
		
		return slotAssignmentFromReceiver(id, ctx.assignmentOperator(), ctx.expression());
	}
	
	public ASTBuilderFromReceiver slotAssignmentFromReceiver(String id, AssignmentOperatorContext opCtx, ParserRuleContext valueCtx) {
		List<IdContext> paramIds = opCtx.behaviorParams().id(); 
		switch(opCtx.op.getType()) {
		case CocuLexer.ASSIGN: {
			ASTBuilder valueBuilder = valueCtx.accept(this);
			return new ASTBuilderFromReceiver() {
				@Override
				public ASTBuilder createBuilder(ASTBuilder receiver) {
					int type = ASTSlotAssignment.TYPE_REGULAR;
					return () -> new ASTSlotAssignment(type, receiver.build(), id, paramIds.size(), valueBuilder.build());
				}
			};
		} case CocuLexer.ASSIGN_PROTO: {
			ASTBuilder valueBuilder = valueCtx.accept(this);
			return new ASTBuilderFromReceiver() {
				@Override
				public ASTBuilder createBuilder(ASTBuilder receiver) {
					return () -> new ASTSlotAssignment(ASTSlotAssignment.TYPE_PROTO, receiver.build(), id, paramIds.size(), valueBuilder.build());
				}
			};
		} case CocuLexer.ASSIGN_QUOTED: {
			return new ASTBuilderFromReceiver() {
				@Override
				public ASTBuilder createBuilder(ASTBuilder receiver) {
					OrdinalAllocator bodyIdToParameterOrdinalMap = new OrdinalAllocator();
					ASTBuilder valueBuilder = assignQuotedBuilder(paramIds, valueCtx, bodyIdToParameterOrdinalMap);
					int arity = bodyIdToParameterOrdinalMap.sizeExceptEnd();
					return () -> new ASTSlotAssignment(ASTSlotAssignment.TYPE_QUOTED, receiver.build(), id, arity, valueBuilder.build());
				}
			};
		}
		}
		
		return null;
	}
	
	public ASTBuilder visitAccess(AccessContext ctx) {
		String id = ctx.id().getText();
		
		if(idToParameterOrdinalMap.isDeclared(id))
			return accessParameterBuilder(id);
		
		if(idToVariableOrdinalMap.isDeclared(id))
			return accessVariableBuilder(id);
		
		return () -> {
			if(Character.isUpperCase(id.charAt(0)) || accessFields.contains(id))
				return new ASTSlotAccess(ASTImplicitReceiver.INSTANCE, id, 0);
			else {
				// Message to self
				return new ASTMessageExchange(ASTImplicitReceiver.INSTANCE, new ASTMessage(id, new AST[0]));
			}
		};
	}

	private ASTBuilder assignVariableBuilder(ParserRuleContext valueCtx, String id) {
		return assignLocalBuilder(valueCtx, id, idToVariableOrdinalMap);
	}

	private ASTBuilder assignLocalBuilder(ParserRuleContext valueCtx, String id, OrdinalAllocator ordinalMap) {
		IntHolder ordinalHolder = new IntHolder();
		ASTBuilder valueBuilder = valueCtx.accept(this);
		ordinalMap.ordinalFor(id, ordinal -> ordinalHolder.value = ordinal);
		
		return () -> new ASTLocalAssignment(ordinalHolder.value, valueBuilder.build());
	}

	private ASTBuilder accessParameterBuilder(String id) {
		return accessLocalBuilder(id, idToParameterOrdinalMap);
	}

	private ASTBuilder accessVariableBuilder(String id) {
		return accessLocalBuilder(id, idToVariableOrdinalMap);
	}

	private ASTBuilder accessLocalBuilder(String id, OrdinalAllocator ordinalMap) {
		IntHolder ordinalHolder = new IntHolder();
		ordinalMap.ordinalFor(id, ordinal -> ordinalHolder.value = ordinal);
		
		return () -> new ASTLocalAccess(ordinalHolder.value);
	}

	private ASTBuilder assignQuotedBuilder(List<IdContext> paramIds, ParserRuleContext valueCtx, OrdinalAllocator bodyIdToParameterOrdinalMap) {
		ANTLRToAST functionBodyInterceptor = new ANTLRToAST(bodyIdToParameterOrdinalMap, new OrdinalAllocator(), errors, accessFields, assignFields);
		for(IdContext parameterIdNode: paramIds) {
			String parameterId = parameterIdNode.getText();
			functionBodyInterceptor.idToParameterOrdinalMap.declare(parameterId);
		}
		
		ASTBuilder bodyBuilder = valueCtx.accept(functionBodyInterceptor);
		
		/*Start from 1 since zero is used for self*/
		int parameterOffset = 1;
		functionBodyInterceptor.idToParameterOrdinalMap.generate(parameterOffset);
		int variableOffset = parameterOffset + functionBodyInterceptor.idToParameterOrdinalMap.size();
		functionBodyInterceptor.idToVariableOrdinalMap.generate(variableOffset);
		int localCount = 1 + functionBodyInterceptor.idToParameterOrdinalMap.size() + functionBodyInterceptor.idToVariableOrdinalMap.size();
		
		return () -> new ASTBehavior(localCount, bodyBuilder.build());
	}
	
	@Override
	public ASTBuilder visitParArg(ParArgContext ctx) {
		String id = ctx.id().getText();
		idToParameterOrdinalMap.declare(id);
		return accessParameterBuilder(id);
	}
	
	@Override
	public ASTBuilder visitSlotAccess(SlotAccessContext ctx) {
		return new ASTBuilderFromReceiver() {
			@Override
			public ASTBuilder createBuilder(ASTBuilder receiver) {
				String id = getSelectorId(ctx.selector());
				return () -> new ASTSlotAccess(receiver.build(), id, 0);
			}
		};
	}
	
	@Override
	public ASTBuilder visitSlotAssignment(SlotAssignmentContext ctx) {
		String id = getSelectorId(ctx.selector());
		return slotAssignmentFromReceiver(id, ctx.assignmentOperator(), ctx.expression());
	}
	
	@Override
	public ASTBuilder visitDict(DictContext ctx) {
//		return dictBuilder(ctx.dictEntry());
		
		ASTBuilder prototypeBuilder;
		if(ctx.explicitPrototype != null)
			prototypeBuilder = ctx.explicitPrototype.accept(this);
		else
			prototypeBuilder = () -> ASTThis.INSTANCE;
		
		List<DictEntryContext> entryCtxs = ctx.dictEntry();
		
		HashSet<String> fields = new HashSet<String>();
		ANTLRToAST fieldsVisitor = new ANTLRToAST(idToParameterOrdinalMap, idToVariableOrdinalMap, errors, accessFields, fields);
		ANTLRToAST methodsVisitor = new ANTLRToAST(idToParameterOrdinalMap, idToVariableOrdinalMap, errors, fields, fields);
		@SuppressWarnings("unchecked")
		Function<AST, ASTDict.Entry>[] entryConstructors = (Function<AST, ASTDict.Entry>[])new Function<?, ?>[entryCtxs.size()]; 
		ASTBuilder[] valueBuilders = new ASTBuilder[entryCtxs.size()];
		
		for(int i = 0; i < valueBuilders.length; i++) {
			DictEntryContext entryCtx = entryCtxs.get(i);
			String id = getSelectorId(entryCtx.selector());
			List<IdContext> paramIds = entryCtx.assignmentOperator() != null && entryCtx.assignmentOperator().behaviorParams() != null
				? entryCtx.assignmentOperator().behaviorParams().id() : Collections.emptyList();
			
			int opType = entryCtx.assignmentOperator() != null ? entryCtx.assignmentOperator().op.getType() : CocuLexer.ASSIGN;
			ParserRuleContext valueCtx = entryCtx.expression() != null ? entryCtx.expression() : createANTLRNil(entryCtx, entryCtx.invokingState); 
			
			switch(opType) {
			case CocuLexer.ASSIGN:
				fields.add(id);
				int type = ASTSlotAssignment.TYPE_REGULAR;
				entryConstructors[i] = valueAst -> new ASTDict.Entry(id, type, paramIds.size(), valueAst);
				valueBuilders[i] = valueCtx.accept(fieldsVisitor);
				break;
			case CocuLexer.ASSIGN_PROTO:
				fields.add(id);
				entryConstructors[i] = valueAst -> new ASTDict.Entry(id, ASTSlotAssignment.TYPE_PROTO, paramIds.size(), valueAst);
				valueBuilders[i] = valueCtx.accept(fieldsVisitor);
				break;
			case CocuLexer.ASSIGN_QUOTED:
				OrdinalAllocator bodyIdToParameterOrdinalMap = new OrdinalAllocator();
				valueBuilders[i] = methodsVisitor.assignQuotedBuilder(paramIds, valueCtx, bodyIdToParameterOrdinalMap);
				int arity = bodyIdToParameterOrdinalMap.sizeExceptEnd();
				entryConstructors[i] = valueAst -> new ASTDict.Entry(id, ASTSlotAssignment.TYPE_QUOTED, arity, valueAst);
				break;
			}
		}
		
		return new ASTReducer(valueBuilders, valueAsts -> {
			AST prototype = prototypeBuilder.build();
			ASTDict.Entry[] entries = new ASTDict.Entry[valueBuilders.length];
			for(int i = 0; i < entries.length; i++)
				entries[i] = entryConstructors[i].apply(valueAsts[i]);
			return new ASTDict(prototype, entries);
		});
	}
	
	private ParserRuleContext createANTLRNil(ParserRuleContext parent, int invokingState) {
		PseudoVarContext nilContext = new PseudoVarContext(parent, invokingState);
		TerminalNodeImpl pseudoVarNode = new TerminalNodeImpl(new CommonToken(CocuLexer.PSEUDO_VAR, "null"));
		nilContext.addChild(pseudoVarNode);
		return nilContext;
	}

	@Override
	public ASTBuilder visitClosure(ClosureContext ctx) {
		OrdinalAllocator newIdToParameterOrdinalMap = idToParameterOrdinalMap.newInnerEnd();
		OrdinalAllocator newIdToVariableOrdinalMap = idToVariableOrdinalMap.newInnerEnd();
		ANTLRToAST closureBodyVisitor = new ANTLRToAST(newIdToParameterOrdinalMap, newIdToVariableOrdinalMap, errors, accessFields, assignFields);
		
		for(IdContext parameterIdNode: ctx.behaviorParams().id()) {
			String parameterId = parameterIdNode.getText();
			closureBodyVisitor.idToParameterOrdinalMap.declare(parameterId);
		}
		
		ASTBuilder bodyBuilder = closureBodyVisitor.groupingBuilder(ctx.expression());
		
		int closureParameterCount = closureBodyVisitor.idToParameterOrdinalMap.sizeExceptEnd();
		IntHolder closureParameterOffsetHolder = new IntHolder();
		newIdToParameterOrdinalMap.getLocalParameterOffset(offset -> closureParameterOffsetHolder.value = offset);
		
		return () -> new ASTClosure(closureParameterOffsetHolder.value, closureParameterCount, bodyBuilder.build());
	}
	
	private static String getSelectorId(SelectorContext ctx) {
		return ctx.getText();
	}
	
	@Override
	public ASTBuilder visitPseudoVar(PseudoVarContext ctx) {
		switch(ctx.PSEUDO_VAR().getText()) {
		case "this": return () -> ASTThis.INSTANCE;
		case "null": return () -> ASTNull.INSTANCE;
		case "true": return () -> ASTTrue.INSTANCE;
		case "false": return () -> ASTFalse.INSTANCE;
		case "frame": return () -> ASTFrame.INSTANCE;
		}
		
		return null;
	}
	
	@Override
	public ASTBuilder visitInteger(IntegerContext ctx) {
		return () -> new ASTInteger(Integer.parseInt(ctx.INT().getText()));
	}
	
	@Override
	public ASTBuilder visitString(StringContext ctx) {
		String rawString = ctx.getText();
		String string = rawString.substring(1, rawString.length() - 1)
			.replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t");
		return () -> new ASTString(string);
	}
	
	@Override
	public ASTBuilder visitArray(ArrayContext ctx) {
		ASTBuilder[] itemBuilders = new ASTBuilder[ctx.expression().size()];
		for(int i = 0; i < itemBuilders.length; i++)
			itemBuilders[i] = ctx.expression(i).accept(this);
		return new ASTReducer(itemBuilders, asts -> new ASTArray(asts));
	}
	
	@Override
	public ASTBuilder visitSpawn(SpawnContext ctx) {
		ANTLRToAST forkBodyInterceptor = new ANTLRToAST(new OrdinalAllocator(), new OrdinalAllocator(), errors, accessFields, assignFields);
		
		ASTBuilder environmentBuilder;
		if(ctx.explicitPrototype != null)
			environmentBuilder = ctx.explicitPrototype.accept(this);
		else
			environmentBuilder = () -> ASTThis.INSTANCE;
			
		ASTBuilder bodyBuilder = forkBodyInterceptor.groupingBuilder(ctx.expression());
		
		/*Start from 1 since zero is used for self*/
		int parameterOffset = 1;
		forkBodyInterceptor.idToParameterOrdinalMap.generate(parameterOffset);
		int variableOffset = parameterOffset + forkBodyInterceptor.idToParameterOrdinalMap.size();
		forkBodyInterceptor.idToVariableOrdinalMap.generate(variableOffset);
		int localCount = 1 + forkBodyInterceptor.idToParameterOrdinalMap.size() + forkBodyInterceptor.idToVariableOrdinalMap.size();
		
		return () -> new ASTSpawn(environmentBuilder.build(), new ASTBehavior(localCount, bodyBuilder.build()));
	}
	
	private void appendError(ParserRuleContext ctx, String message) {
		appendError(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), message);
	}
	
	private void appendError(int line, int charPositionInLine, String message) {
		errors.appendMessage(line, charPositionInLine, message);
	}
}
