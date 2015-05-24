package cocu.reflang;

import java.util.ArrayList;

import cocu.reflang.ast.AST;
import cocu.reflang.ast.ASTClosure;
import cocu.reflang.ast.ASTString;
import cocu.reflang.ast.ASTToCode;
import cocu.runtime.Instruction;

public interface PrimitiveVisitorFactory {
	PrimitiveVisitor create(ASTToCode visitor, CodeEmitter instructions, boolean mustBeExpression);
	
	public static class ConstInstruction implements PrimitiveVisitorFactory {
		private Instruction instruction;
		private boolean doesReturn;

		public ConstInstruction(Instruction instruction, boolean doesReturn) {
			this.instruction = instruction;
			this.doesReturn = doesReturn;
		}
		
		@Override
		public PrimitiveVisitor create(ASTToCode visitor, CodeEmitter instructions, boolean mustBeExpression) {
			return new PrimitiveVisitor() {
				@Override
				public void visitPrimitive(String id, AST[] args) {
					for(AST arg: args)
						visitor.visitAsExpression(arg);
					
					instructions.addSingle(instruction);
					if(mustBeExpression && !doesReturn)
						instructions.addSingle(new Instruction(Instruction.OPCODE_LOAD_NULL));
					else if(!mustBeExpression && doesReturn)
						instructions.addSingle(new Instruction(Instruction.OPCODE_POP));
					// What if !mustBeExpression && doesReturn?
				}
			};
		}
	}	
	
	public static class IsSet implements PrimitiveVisitorFactory {
		@Override
		public PrimitiveVisitor create(ASTToCode visitor, CodeEmitter instructions, boolean mustBeExpression) {
			return new PrimitiveVisitor() {
				@Override
				public void visitPrimitive(String id, AST[] args) {
					args[0].accept(visitor);
					String slotId = ((ASTString)args[1]).string;
					instructions.addSingle(new Instruction(Instruction.OPCODE_IS_DEFINED, slotId, 0));
					if(!mustBeExpression)
						instructions.addSingle(new Instruction(Instruction.OPCODE_LOAD_NULL));
				}
			};
		}
	}
	
	public static class Util {
		public static void acceptClosureBodyOrCall(ASTToCode visitor, AST expression, CodeEmitter instructions, boolean mustBeExpression) {
			if(expression instanceof ASTClosure) {
				ASTClosure closure = (ASTClosure)expression;
				visitor.visit(closure.body, mustBeExpression);
			} else {
				instructions.addSingle(new Instruction(Instruction.OPCODE_CALL_CLOSURE_0));
			}
		}
		
		public static String[] split(String str, String regex) {
			String[] rawSplit = str.split(regex);
			ArrayList<String> splitBuilder = new ArrayList<String>();
			for(String rawStr: rawSplit) {
				if(rawStr.trim().length() != 0)
					splitBuilder.add(rawStr);
			}
			return splitBuilder.toArray(new String[splitBuilder.size()]);
		}
	}

	public static class IfElse implements PrimitiveVisitorFactory {
		@Override
		public PrimitiveVisitor create(ASTToCode visitor, CodeEmitter instructions, boolean mustBeExpression) {
			return new PrimitiveVisitor() {
				@Override
				public void visitPrimitive(String id, AST[] args) {
					AST condition = args[0];
					AST trueBlock = args[1];
					AST falseBlock = args.length > 2 ? args[2] : null;
					
					Util.acceptClosureBodyOrCall(visitor, condition, instructions, true);
					
					Label labelElse = new Label();
					Label labelEnd = new Label();
					
					instructions.jump(jump -> new Instruction(Instruction.OPCODE_IF_FALSE, jump), labelElse);
					
					Util.acceptClosureBodyOrCall(visitor, trueBlock, instructions, mustBeExpression);
					
					instructions.jump(jump -> new Instruction(Instruction.OPCODE_JUMP, jump), labelEnd);

					instructions.label(labelElse);

					if(falseBlock != null) {
						Util.acceptClosureBodyOrCall(visitor, falseBlock, instructions, mustBeExpression);
					} else {
						if(mustBeExpression)
							instructions.addSingle(new Instruction(Instruction.OPCODE_LOAD_NULL));
					}

					instructions.label(labelEnd);
				}

			};
		}
	}
	
	public static class While implements PrimitiveVisitorFactory {
		@Override
		public PrimitiveVisitor create(ASTToCode visitor, CodeEmitter instructions, boolean mustBeExpression) {
			return new PrimitiveVisitor() {
				@Override
				public void visitPrimitive(String id, AST[] args) {
					AST condition = args[0];
					AST body = args[1];
					
					Label labelLoop = new Label();
					Label labelEnd = new Label();

					instructions.label(labelLoop);
					Util.acceptClosureBodyOrCall(visitor, condition, instructions, true);
					
					instructions.jump(jump -> new Instruction(Instruction.OPCODE_IF_FALSE, jump), labelEnd);
					
					Util.acceptClosureBodyOrCall(visitor, body, instructions, false);
					
					instructions.jump(jump -> new Instruction(Instruction.OPCODE_JUMP, jump), labelLoop);
					
					instructions.label(labelEnd);
					
					if(mustBeExpression)
						instructions.addSingle(new Instruction(Instruction.OPCODE_LOAD_NULL));
				}
			};
		}
	}
	
	public static class Eval implements PrimitiveVisitorFactory {
		@Override
		public PrimitiveVisitor create(ASTToCode visitor, CodeEmitter instructions, boolean mustBeExpression) {
			return new PrimitiveVisitor() {
				@Override
				public void visitPrimitive(String id, AST[] args) {
					// Append eval arguments first
					AST closureArg = args[0];
					for(int i = 1; i < args.length; i++)
						visitor.visitAsExpression(args[i]);
					// Then closure
					visitor.visitAsExpression(closureArg);
					
					int closureArgCount = args.length - 1;
					
					switch(closureArgCount) {
					case 0:
						instructions.addSingle(new Instruction(Instruction.OPCODE_CALL_CLOSURE_0));
						break;
					case 1:
						instructions.addSingle(new Instruction(Instruction.OPCODE_CALL_CLOSURE_1));
						break;
					case 2:
						instructions.addSingle(new Instruction(Instruction.OPCODE_CALL_CLOSURE_2));
						break;
					case 3:
						instructions.addSingle(new Instruction(Instruction.OPCODE_CALL_CLOSURE_3));
						break;
					default:
						instructions.addSingle(new Instruction(Instruction.OPCODE_CALL_CLOSURE, closureArgCount));
						break;
					}
					
					if(!mustBeExpression)
						instructions.addSingle(new Instruction(Instruction.OPCODE_POP));
				}
			};
		}
	}
	
	public static class ClassField implements PrimitiveVisitorFactory {
		@Override
		public PrimitiveVisitor create(ASTToCode visitor, CodeEmitter instructions, boolean mustBeExpression) {
			return new PrimitiveVisitor() {
				@Override
				public void visitPrimitive(String id, AST[] args) {
					if(mustBeExpression) {
						ASTString className = (ASTString)args[0];
						ASTString fieldName = (ASTString)args[1];
						
						instructions.addSingle(new Instruction(Instruction.OPCODE_NATIVE_CLASS_FIELD, className.string, fieldName.string));
					}
				}
			};
		}
	}
	
	public static class InstanceInvoke implements PrimitiveVisitorFactory {
		@Override
		public PrimitiveVisitor create(ASTToCode visitor, CodeEmitter instructions, boolean mustBeExpression) {
			return new PrimitiveVisitor() {
				@Override
				public void visitPrimitive(String id, AST[] args) {
					AST receiver = args[0];
					ASTString className = (ASTString)args[1];
					ASTString methodName = (ASTString)args[2];
					String[] parameters = Util.split(((ASTString)args[3]).string, ";");
					AST[] arguments = new AST[args.length - 4];
					
					visitor.visitAsExpression(receiver);
					for(int i = 0; i < arguments.length; i++)
						visitor.visitAsExpression(args[4 + i]);
					
					instructions.addSingle(new Instruction(Instruction.OPCODE_NATIVE_INSTANCE_INVOKE, className.string, methodName.string, parameters));

					if(!mustBeExpression)
						instructions.addSingle(new Instruction(Instruction.OPCODE_POP));
				}
			};
		}
	}
	
	public static class NewInstance implements PrimitiveVisitorFactory {
		@Override
		public PrimitiveVisitor create(ASTToCode visitor, CodeEmitter instructions, boolean mustBeExpression) {
			return new PrimitiveVisitor() {
				@Override
				public void visitPrimitive(String id, AST[] args) {
					ASTString className = (ASTString)args[0];
					String[] parameters = Util.split(((ASTString)args[1]).string, ";");
					AST[] arguments = new AST[args.length - 2];
					
					for(int i = 0; i < arguments.length; i++)
						visitor.visitAsExpression(args[i + 2]);
					
					instructions.addSingle(new Instruction(Instruction.OPCODE_NATIVE_NEW_INSTANCE, className.string, parameters));

					if(!mustBeExpression)
						instructions.addSingle(new Instruction(Instruction.OPCODE_POP));
				}
			};
		}
	}
	
	public static class Receive implements PrimitiveVisitorFactory {
		@Override
		public PrimitiveVisitor create(ASTToCode visitor, CodeEmitter instructions, boolean mustBeExpression) {
			return new PrimitiveVisitor() {
				@Override
				public void visitPrimitive(String id, AST[] args) {
//					instructions.addSingle(new Instruction(Instruction.OPCODE_RECEIVE));
					instructions.addSingle(new Instruction(Instruction.OPCODE_RECEIVE));

//					if(mustBeExpression)
//						instructions.addSingle(new Instruction(Instruction.OPCODE_LOAD_NULL));
				}
			};
		}
	}	
}
