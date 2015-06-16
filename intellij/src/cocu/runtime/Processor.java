package cocu.runtime;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import cocu.debugging.Debug;
import cocu.reflang.CompilationException;
import cocu.reflang.Compiler;
import cocu.reflang.FrameLoader;
import cocu.reflang.SymbolTable;

public class Processor {
	private static final Instruction[] FORWARD_CALL_INSTRUCTIONS_0 = new Instruction[] {
		new Instruction(Instruction.OPCODE_LOAD_LOC, 0),
		new Instruction(Instruction.OPCODE_SEND_CODE_0, SymbolTable.Codes.call, 0),
		new Instruction(Instruction.OPCODE_RET)
	};
	
	private static final Instruction[] FORWARD_CALL_INSTRUCTIONS_1 = new Instruction[] {
		new Instruction(Instruction.OPCODE_LOAD_LOC, 0),
		new Instruction(Instruction.OPCODE_LOAD_LOC, 1),
		new Instruction(Instruction.OPCODE_SEND_CODE_1, SymbolTable.Codes.call_1, 1),
		new Instruction(Instruction.OPCODE_RET)
	};
	
	private static final Instruction[] FORWARD_CALL_INSTRUCTIONS_2 = new Instruction[] {
		new Instruction(Instruction.OPCODE_LOAD_LOC, 0),
		new Instruction(Instruction.OPCODE_LOAD_LOC, 1),
		new Instruction(Instruction.OPCODE_LOAD_LOC, 2),
		new Instruction(Instruction.OPCODE_SEND_CODE_2, SymbolTable.Codes.call_2, 2),
		new Instruction(Instruction.OPCODE_RET)
	};
	
	private static final Instruction[] FORWARD_CALL_INSTRUCTIONS_3 = new Instruction[] {
		new Instruction(Instruction.OPCODE_LOAD_LOC, 0),
		new Instruction(Instruction.OPCODE_LOAD_LOC, 1),
		new Instruction(Instruction.OPCODE_LOAD_LOC, 2),
		new Instruction(Instruction.OPCODE_LOAD_LOC, 3),
		new Instruction(Instruction.OPCODE_SEND_CODE_3, SymbolTable.Codes.call_3, 3),
		new Instruction(Instruction.OPCODE_RET)
	};

	public DictionaryProcess getAny() {
		return protoAny;
	}

	public static class Frame implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public Frame sender;
		public Process[] locals;
		
		public final Instruction[] instructions;
		public int instructionPointer;
		public FrameProcess reification; 

		private Process[] stack;
		private int stackSize;
		
		public Frame(Frame sender, Process[] locals, Instruction[] instructions, int maxStackSize) {
			this.sender = sender;
			this.locals = locals;
			this.instructions = instructions;
			stackSize = 0;
			stack = new Process[maxStackSize];
		}
		
		public final FrameProcess getReifiedFrame(LocalizableProcess protoFrame) {
			if(reification == null)
				reification = new FrameProcess(protoFrame, this);
			
			return reification;
		}
		
		public final Process pop() {
			stackSize--;
			Process p = stack[stackSize];
			stack[stackSize] = null;
			return p;
		}

		public final void copy1Into(int i, Process[] a) {
			a[i] = stack[stackSize - 1];
		}

		public final void copy2Into(int i, Process[] a) {
			System.arraycopy(stack, stackSize - 2, a, i, 2);
		}

		public final void copy3Into(int i, Process[] a) {
			System.arraycopy(stack, stackSize - 3, a, i, 3);
		}

		public final void copyNInto(int i, Process[] a, int n) {
			System.arraycopy(stack, stackSize - n, a, i, n);
		}

		public final void copyNInto(int i, Process[] a, int n, int m) {
			System.arraycopy(stack, stackSize - n - m, a, i, n);
		}

		public final void set0(Process p) {
			stack[stackSize - 1] = p;
		}
		
		public final void set1(Process p) {
			stack[stackSize - 2] = p;
		}

		public final void pop1() {
			stackSize--;
			stack[stackSize] = null;
		}

		public final void pop2() {
			int newSize = stackSize - 2;
			Arrays.fill(stack, newSize, stackSize, null);
			stackSize = newSize;
		}

		public final void pop3() {
			int newSize = stackSize - 3;
			Arrays.fill(stack, newSize, stackSize, null);
			stackSize = newSize;
		}

		public final void pop4() {
			int newSize = stackSize - 4;
			Arrays.fill(stack, newSize, stackSize, null);
			stackSize = newSize;
		}

		public final void popN(int n) {
			int newSize = stackSize - n;
			Arrays.fill(stack, newSize, stackSize, null);
			stackSize = newSize;
		}
		
		public final void push(Process p) {
			stack[stackSize] = p;
			stackSize++;
		}

		public final void stackAdd(int index, Process p) {
			for(int i = stackSize - 1; i >= index; i--)
				stack[i + 1] = stack[i];
			stack[index] = p;
			stackSize++;
		}

		public final String stackToString() {
			return Arrays.toString(stack);
		}

		public final int stackSize() {
			return stackSize;
		}

		public final Process peek() {
			return stack[stackSize - 1];
		}
		
		public final void dup() {
			stack[stackSize] = stack[stackSize - 1];
			stackSize++;
		}
		
		public final void dup1() {
			stack[stackSize] = stack[stackSize - 1];
			stack[stackSize - 1] = stack[stackSize - 2];
			stack[stackSize - 2] = stack[stackSize];
			stackSize++;
		}

		public final Process peek1() {
			return stack[stackSize - 2];
		}

		public final Process peek2() {
			return stack[stackSize - 3];
		}

		public Process peek3() {
			return stack[stackSize - 4];
		}

		public Process peekn(int ordinal) {
			return stack[stackSize - ordinal];
		}

		public final Process stackGet(int i) {
			return stack[i];
		}

		public final void stackSet(int i, Process p) {
			stack[i] = p;
		}
	}
	
	private Frame currentFrame;
	private DictionaryProcess protoAny;
	private NilProcess singletonNil;
	private BooleanProcess singletonTrue;
	private BooleanProcess singletonFalse;
	private DictionaryProcess protoInteger;
	private DictionaryProcess protoArray;
	private DictionaryProcess protoString;
	private DictionaryProcess protoFrame;
	private DictionaryProcess protoBehavior;
	private DictionaryProcess protoClosure;
	private DictionaryProcess protoEnvelope;
	private Compiler compiler;
	private FrameLoader frameLoader;

	public Processor(Compiler compiler, FrameLoader frameLoader/*, int localCount, int maxStackSize, Instruction[] instructions*/) {
		this.compiler = compiler;
		this.frameLoader = frameLoader;
		// TODO: Consider: Should the Any prototype be this? Should CustomProcess be a DictionaryProcess?
		// Should CustomProcess not be a process at all? Should CustomProcess hold any and push it instead this?  - Also at other locations, e.g. when loading.
		// Add Any prototype
		protoAny = new DictionaryProcess();
		protoAny.define(SymbolTable.Codes.Any, protoAny);
		// Add Null singleton
		singletonNil = new NilProcess();
		singletonNil.defineProto(SymbolTable.Codes.prototype, protoAny);
		protoAny.define(SymbolTable.Codes.Null, singletonNil);
		// Add boolean True singleton
		singletonTrue = new BooleanProcess(true);
		singletonTrue.defineProto(SymbolTable.Codes.prototype, protoAny);
		protoAny.define(SymbolTable.Codes.True, singletonTrue);
		// Add boolean False singleton
		singletonFalse = new BooleanProcess(false);
		singletonFalse.defineProto(SymbolTable.Codes.prototype, protoAny);
		protoAny.define(SymbolTable.Codes.False, singletonFalse);
		// Add Array prototype
		protoArray = protoAny.derive();
		protoAny.define(SymbolTable.Codes.Array, protoArray);
		// Add String prototype
		protoString = protoAny.derive();
		protoAny.define(SymbolTable.Codes.String, protoString);
		// Add Integer prototype
		protoInteger = protoAny.derive();
		protoAny.define(SymbolTable.Codes.Integer, protoInteger);
		// Add Frame prototype
		protoFrame = protoAny.derive();
		protoAny.define(SymbolTable.Codes.Frame, protoFrame);
		// Add Behavior prototype
		protoBehavior = protoAny.derive();
		protoAny.define(SymbolTable.Codes.Behavior, protoBehavior);
		// Add Closure prototype
		protoClosure = protoAny.derive();
		protoAny.define(SymbolTable.Codes.Closure, protoClosure);
		// Add Envelope prototype
		protoEnvelope = protoAny.derive();
		protoAny.define(SymbolTable.Codes.Envelope, protoEnvelope);
		// Add Error handler
		DictionaryProcess handler = protoAny.derive();
		handler.define(SymbolTable.Codes.call_2, new BehaviorProcess(protoBehavior, new FrameInfo(3, 2, new Instruction[] {
			// Report uncaught signal as error
			new Instruction(Instruction.OPCODE_LOAD_LOC, 1), // Load signal
			new Instruction(Instruction.OPCODE_LOAD_LOC, 2), // Load frame
			new Instruction(Instruction.OPCODE_REPORT_ERROR),
			new Instruction(Instruction.OPCODE_LOAD_NULL),
			new Instruction(Instruction.OPCODE_RET)
		})));
		protoAny.define(SymbolTable.Codes.Handler, handler);
	}

	public Frame getFrame() {
		return currentFrame;
	}

	public void setFrame(int localCount, int maxStackSize, Instruction[] instructions) {
		Process[] locals = new Process[localCount];
		locals[0] = protoAny;
		currentFrame = new Frame(null, /*protoAny, */locals, instructions, maxStackSize);
	}
	
	private transient SymbolTable symbolTable;
	private transient String commonsPath;
	private transient String currentPath;
	
	public void setup(SymbolTable symbolTable, String commonsPath, String currentPath) {
		this.symbolTable = symbolTable;
		this.commonsPath = commonsPath;
		this.currentPath = currentPath;
	}

	@SuppressWarnings("unused")
	public void process() {
		stopRequested = false;
		Debug.println(Debug.LEVEL_HIGH, "process");

		while(true) {
			try {
				if(currentFrame != null) {
					while(!stopRequested) {
						Instruction instruction = currentFrame.instructions[currentFrame.instructionPointer];
						
						if(Debug.maxLevel >= Debug.LEVEL_HIGH) {
							Debug.println(Debug.LEVEL_HIGH, "stack: " + currentFrame.stackToString());
							Debug.println(Debug.LEVEL_HIGH, "play: " + instruction);
						}
						
						next(instruction);
					}
					break;
				}
			} catch(Exception e) {
				// Do some stack maintenance
				
				// If current instruction only pops stack after potential errors may occur
				Instruction currentInstruction = currentFrame.instructions[currentFrame.instructionPointer];
				if(Instruction.popsAfterError(currentInstruction)) {
					// Then the popping should be done now
					int popCount = Instruction.getPopCount(currentInstruction);
					currentFrame.popN(popCount);
				}
				
				NativeObjectHolder nativeSignal = new NativeObjectHolder(e);
				Process handler = protoAny.lookup(SymbolTable.Codes.Handler);
				Instruction[] handleInstructions = new Instruction[] {
					new Instruction(Instruction.OPCODE_LOAD_LOC, 0),
					new Instruction(Instruction.OPCODE_LOAD_LOC, 1),
					new Instruction(Instruction.OPCODE_LOAD_LOC, 2),
					new Instruction(Instruction.OPCODE_SEND_CODE_2, SymbolTable.Codes.call_2),
					new Instruction(Instruction.OPCODE_POP),
					new Instruction(Instruction.OPCODE_FINISH),
				};
				FrameProcess frame = currentFrame.getReifiedFrame(protoFrame);
				currentFrame = 
					new Frame(frame.frame, new Process[] {handler, nativeSignal, frame}, handleInstructions, 3);
//				DictionaryProcess signal = protoAny.clone();
//				NativeObjectHolder eHolder = new NativeObjectHolder(e);
//				signal.define(SymbolTable.Codes.cause, eHolder);
//				signal.define(SymbolTable.Codes.message, new StringProcess(protoString, e.getMessage()));
//				signal(signal, currentFrame.getReifiedFrame(protoFrame));
			}
		}
		
		if(currentFrame != null)
			Debug.println(Debug.LEVEL_HIGH, "stack: " + currentFrame.stackToString());
		
		Debug.println(Debug.LEVEL_HIGH, "/process");
	}

	public Process popStack() {
		return currentFrame.pop();
	}
	
//	private final void signal(Process signal, FrameProcess frame) {
//		Frame nearestFrameWithHandler = frame.frame.getNearestFrameWithHandler();
//		Instruction[] signalInstructions = new Instruction[] {
//			new Instruction(Instruction.OPCODE_LOAD_LOC, 0),
//			new Instruction(Instruction.OPCODE_LOAD_LOC, 1),
//			new Instruction(Instruction.OPCODE_LOAD_LOC, 2),
//			new Instruction(Instruction.OPCODE_SEND_CODE_2, SymbolTable.Codes.call_2),
//			new Instruction(Instruction.OPCODE_RET),
//		};
//		currentFrame = 
//			new Frame(nearestFrameWithHandler.sender, new Process[] {nearestFrameWithHandler.handler, signal, frame}, signalInstructions, nearestFrameWithHandler.interfaceId, 3);
//	}
	
	private boolean stopRequested;
	
	private final void next(Instruction instruction) {
		switch(instruction.opcode) {
		case Instruction.OPCODE_INC_IP: {
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_FINISH: {
			if(currentFrame.stackSize() > 0)
				Debug.println(Debug.LEVEL_LOW, "stack isn't empty: " + currentFrame.stackToString());
			
			stopRequested = true;
			//currentFrame = null;
			
			break;
		} case Instruction.OPCODE_DUP: {
			currentFrame.dup();
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_DUP1: {
			currentFrame.dup1();
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_STORE_LOC: {
			int ordinal = (int)instruction.operand1;
			Process value = currentFrame.pop();
			currentFrame.locals[ordinal] = value;
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_POP: {
			currentFrame.pop1();
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_SEND: {
			String id = (String)instruction.operand1;
			int argumentCount = (int)instruction.operand2;
			int code = symbolTable.getSymbolCodeFromId(Selector.get(id, argumentCount));
			
			switch(argumentCount) {
			case 0: 
				currentFrame.instructions[currentFrame.instructionPointer] = new Instruction(Instruction.OPCODE_SEND_CODE_0, code, argumentCount);
				break;
			case 1: 
				currentFrame.instructions[currentFrame.instructionPointer] = new Instruction(Instruction.OPCODE_SEND_CODE_1, code, argumentCount);
				break;
			case 2: 
				currentFrame.instructions[currentFrame.instructionPointer] = new Instruction(Instruction.OPCODE_SEND_CODE_2, code, argumentCount);
				break;
			case 3: 
				currentFrame.instructions[currentFrame.instructionPointer] = new Instruction(Instruction.OPCODE_SEND_CODE_3, code, argumentCount);
				break;
			default:
				currentFrame.instructions[currentFrame.instructionPointer] = new Instruction(Instruction.OPCODE_SEND_CODE, code, argumentCount);
				break;
			}
			
			break;
		} case Instruction.OPCODE_SEND_CODE: {
			int code = (int)instruction.operand1;
			int argumentCount = (int)instruction.operand2;
			
			Process receiver = (Process)currentFrame.stackGet(currentFrame.stackSize() - argumentCount - 1);
			
			Object callable = receiver.getCallable(this, code, argumentCount);

			if(callable instanceof BehaviorProcess) {
				BehaviorProcess behavior = (BehaviorProcess)callable;
				
				Process[] locals = new Process[behavior.frameInfo.localCount];
				
				locals[0] = receiver;
				currentFrame.copyNInto(1, locals, argumentCount);
				currentFrame.popN(argumentCount + 1); // Pop arguments and receiver
				
				currentFrame = new Frame(currentFrame, locals, behavior.frameInfo.instructions, behavior.frameInfo.maxStackSize);
			} else if(callable instanceof Frame) {
				currentFrame = (Frame)callable;
			} else if(callable != null) {
				Process[] locals = new Process[1 + argumentCount];
				
				Process process = (Process)callable;
				locals[0] = process;
				currentFrame.copyNInto(1, locals, argumentCount);
				currentFrame.popN(argumentCount + 1); // Pop arguments and receiver
				
				Instruction[] forwardCallInstructions = new Instruction[argumentCount + 3];
				forwardCallInstructions[0] = new Instruction(Instruction.OPCODE_LOAD_LOC, 0);
				int callCode = symbolTable.getSymbolCodeFromId(Selector.get("call", argumentCount));
				for(int i = 0; i < argumentCount; i++)
					forwardCallInstructions[1 + i] = new Instruction(Instruction.OPCODE_LOAD_LOC, 1 + i);
				forwardCallInstructions[forwardCallInstructions.length - 2] = new Instruction(Instruction.OPCODE_SEND_CODE, callCode, argumentCount);
				forwardCallInstructions[forwardCallInstructions.length - 1] = new Instruction(Instruction.OPCODE_RET);
				
				currentFrame = new Frame(currentFrame, locals, forwardCallInstructions, 1 + argumentCount);
			} else {
				throw new RuntimeException("Cache-miss and absent callable for '" + symbolTable.getIdFromSymbolCode(code) + "'.");
			}
			
			break;
		} case Instruction.OPCODE_SEND_CODE_0: {
			int code = (int)instruction.operand1;
			
			Process receiver = currentFrame.pop();
			
			Object callable = receiver.getCallable(this, code, 0);

			if(callable instanceof BehaviorProcess) {
				BehaviorProcess behavior = (BehaviorProcess)callable;
				Process[] locals = new Process[behavior.frameInfo.localCount];
				locals[0] = receiver;
				
				currentFrame = new Frame(currentFrame, locals, behavior.frameInfo.instructions, behavior.frameInfo.maxStackSize);
			} else if(callable instanceof Frame) {
				currentFrame = (Frame)callable;
			} else if(callable != null) {
				// Send some kind of generic call message?
				Process[] locals = new Process[1];
				
				Process process = (Process)callable;
				locals[0] = process;
				
				currentFrame = new Frame(currentFrame, locals, FORWARD_CALL_INSTRUCTIONS_0, 1);
			} else {
				throw new RuntimeException("Cache-miss and absent callable for '" + symbolTable.getIdFromSymbolCode(code) + "'.");
			}
			
			break;
		} case Instruction.OPCODE_SEND_CODE_1: {
			int code = (int)instruction.operand1;
			
			Process receiver = (Process)currentFrame.stackGet(currentFrame.stackSize() - 2);
			
			Object callable = receiver.getCallable(this, code, 1);

			if(callable instanceof BehaviorProcess) {
				BehaviorProcess behavior = (BehaviorProcess)callable;
				Process[] locals = new Process[behavior.frameInfo.localCount];
				locals[0] = receiver;
				currentFrame.copy1Into(1, locals);
				currentFrame.pop2(); // Pop arguments and receiver
				
				currentFrame = new Frame(currentFrame, locals, behavior.frameInfo.instructions, behavior.frameInfo.maxStackSize);
			} else if(callable instanceof Frame) {
				currentFrame = (Frame)callable;
			} else if(callable != null) {
				// Send some kind of generic call message?
				Process[] locals = new Process[2];
				currentFrame.copy1Into(1, locals);
				currentFrame.pop2(); // Pop arguments and receiver
				
				Process process = (Process)callable;
				locals[0] = process;
				
				currentFrame = new Frame(currentFrame, locals, FORWARD_CALL_INSTRUCTIONS_1, 2);
			} else {
				throw new RuntimeException("Cache-miss and absent callable for '" + symbolTable.getIdFromSymbolCode(code) + "'.");
			}
			
			break;
		} case Instruction.OPCODE_SEND_CODE_2: {
			int code = (int)instruction.operand1;
			
			Process receiver = (Process)currentFrame.stackGet(currentFrame.stackSize() - 3);
			
			Object callable = receiver.getCallable(this, code, 2);

			if(callable instanceof BehaviorProcess) {
				BehaviorProcess behavior = (BehaviorProcess)callable;
				Process[] locals = new Process[behavior.frameInfo.localCount];
				locals[0] = receiver;
				currentFrame.copy2Into(1, locals);
				currentFrame.pop3(); // Pop arguments and receiver
				
				currentFrame = new Frame(currentFrame, locals, behavior.frameInfo.instructions, behavior.frameInfo.maxStackSize);
			} else if(callable instanceof Frame) {
				currentFrame = (Frame)callable;
			} else if(callable != null) {
				// Send some kind of generic call message?
				Process[] locals = new Process[3];
				currentFrame.copy2Into(1, locals);
				currentFrame.pop3(); // Pop arguments and receiver
				
				Process process = (Process)callable;
				locals[0] = process;
				
				currentFrame = new Frame(currentFrame, locals, FORWARD_CALL_INSTRUCTIONS_2, 3);
			} else {
				throw new RuntimeException("Cache-miss and absent callable for '" + symbolTable.getIdFromSymbolCode(code) + "'.");
			}
			
			break;
		} case Instruction.OPCODE_SEND_CODE_3: {
			int code = (int)instruction.operand1;
			
			Process receiver = (Process)currentFrame.stackGet(currentFrame.stackSize() - 4);
			
			Object callable = receiver.getCallable(this, code, 3);

			if(callable instanceof BehaviorProcess) {
				BehaviorProcess behavior = (BehaviorProcess)callable;
				Process[] locals = new Process[behavior.frameInfo.localCount];
				locals[0] = receiver;
				currentFrame.copy3Into(1, locals);
				currentFrame.pop4(); // Pop arguments and receiver
				
				currentFrame = new Frame(currentFrame, locals, behavior.frameInfo.instructions, behavior.frameInfo.maxStackSize);
			} else if(callable instanceof Frame) {
				currentFrame = (Frame)callable;
			} else if(callable != null) {
				// Send some kind of generic call message?
				Process[] locals = new Process[4];
				currentFrame.copy3Into(1, locals);
				currentFrame.pop4(); // Pop arguments and receiver
				
				Process process = (Process)callable;
				locals[0] = process;
				
				currentFrame = new Frame(currentFrame, locals, FORWARD_CALL_INSTRUCTIONS_3, 4);
			} else {
				throw new RuntimeException("Cache-miss and absent callable for '" + symbolTable.getIdFromSymbolCode(code) + "'.");
			}
			
			break;
		}/* case Instruction.OPCODE_DISPATCH: {
			// MESSAGE
			// SENDER
			// RECEIVER
			Process message = currentFrame.peek();
			Process sender = currentFrame.peek1();
			Process receiver = currentFrame.peek2();
			
			Object callable = receiver.getCallable(message, sender);

			if(callable instanceof BehaviorProcess) {
				BehaviorProcess behavior = (BehaviorProcess)callable;
				Process[] locals = new Process[behavior.frameInfo.localCount];
				locals[0] = receiver;
				currentFrame.copy3Into(1, locals);
				currentFrame.pop4(); // Pop arguments and receiver
				
				currentFrame = new Frame(currentFrame, locals, behavior.frameInfo.instructions, currentFrame.interfaceId, behavior.frameInfo.maxStackSize);
			} else if(callable instanceof Frame) {
				currentFrame = (Frame)callable;
			} else if(callable != null) {
				// Send some kind of generic call message?
				Process[] locals = new Process[4];
				currentFrame.copy3Into(1, locals);
				currentFrame.pop4(); // Pop arguments and receiver
				
				Process process = (Process)callable;
				locals[0] = process;
				
				currentFrame = new Frame(currentFrame, locals, FORWARD_CALL_INSTRUCTIONS_3, currentFrame.interfaceId, 4);
			} else {
				throw new RuntimeException("Cache-miss and absent callable for '" + symbolTable.getIdFromSymbolCode(code) + "'.");
			}
			
			break;
		} */case Instruction.OPCODE_RET: {
			Frame returnFrame = currentFrame.sender;
			returnFrame.push(currentFrame.peek());
			currentFrame = returnFrame;
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_RET_NONE: {
			currentFrame = currentFrame.sender;
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_IF_TRUE: {
			BooleanProcess value = (BooleanProcess)currentFrame.pop();
			if(value.value) {
				int jump = (int)instruction.operand1;
				currentFrame.instructionPointer += jump;
			} else
				currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_IF_FALSE: {
			BooleanProcess value = (BooleanProcess)currentFrame.pop();
			if(!value.value) {
				int jump = (int)instruction.operand1;
				currentFrame.instructionPointer += jump;
			} else
				currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_JUMP: {
			int jump = (int)instruction.operand1;
			currentFrame.instructionPointer += jump;
			
			break;
		} case Instruction.OPCODE_SET: {
			String id = (String)instruction.operand1;
			int parameterCount = (int)instruction.operand2;
			int code = symbolTable.getSymbolCodeFromId(Selector.get(id, parameterCount));
			currentFrame.instructions[currentFrame.instructionPointer] = new Instruction(Instruction.OPCODE_SET_CODE, code);
			
			break;
		} case Instruction.OPCODE_SET_CODE: {
			int code = (int)instruction.operand1;
			Process value = currentFrame.peek();
			Process receiver = (Process)currentFrame.peek1();
			receiver.define(code, value);
			currentFrame.pop2();
			currentFrame.instructionPointer++;
			
			break;
		}/* case Instruction.OPCODE_SET_SHARED: {
			String id = (String)instruction.operand1;
			int parameterCount = (int)instruction.operand2;
			int code = symbolTable.getSymbolCodeFromId(Selector.get(id, parameterCount));
			currentFrame.instructions[currentFrame.instructionPointer] = new Instruction(Instruction.OPCODE_SET_SHARED_CODE, code);
			
			break;
		} case Instruction.OPCODE_SET_SHARED_CODE: {
			int code = (int)instruction.operand1;
			Process value = currentFrame.peek();
			Process receiver = (Process)currentFrame.peek1();
			receiver.defineShared(code, value);
			currentFrame.pop2();
			currentFrame.instructionPointer++;
			
			break;
		}*/ case Instruction.OPCODE_SET_PROTO: {
			String id = (String)instruction.operand1;
			int parameterCount = (int)instruction.operand2;
			int code = symbolTable.getSymbolCodeFromId(Selector.get(id, parameterCount));
			currentFrame.instructions[currentFrame.instructionPointer] = new Instruction(Instruction.OPCODE_SET_PROTO_CODE, code);
			
			break;
		} case Instruction.OPCODE_SET_PROTO_CODE: {
			int code = (int)instruction.operand1;
			Process value = currentFrame.peek();
			Process receiver = (Process)currentFrame.peek1();
			receiver.defineProto(code, value);
			currentFrame.pop2();
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_GET: {
			String id = (String)instruction.operand1;
			int parameterCount = (int)instruction.operand2;
			int code = symbolTable.getSymbolCodeFromId(Selector.get(id, parameterCount));
			currentFrame.instructions[currentFrame.instructionPointer] = new Instruction(Instruction.OPCODE_GET_CODE, code);
			
			break;
		} case Instruction.OPCODE_GET_CODE: {
			int code = (int)instruction.operand1;
			Process receiver = (Process)currentFrame.peek();
			Process value = receiver.lookup(code);
			if(value != null) {
				currentFrame.set0(value);
				currentFrame.instructionPointer++;
			} else {
				throw new RuntimeException("Undefined slot: " + symbolTable.getIdFromSymbolCode(code));
			}
			break;
		} 
		
		 case Instruction.OPCODE_IS_DEFINED: {
			String id = (String)instruction.operand1;
			int parameterCount = (int)instruction.operand2;
			int code = symbolTable.getSymbolCodeFromId(Selector.get(id, parameterCount));
			currentFrame.instructions[currentFrame.instructionPointer] = new Instruction(Instruction.OPCODE_IS_DEFINED_CODE, code);
			
			break;
		} case Instruction.OPCODE_IS_DEFINED_CODE: {
			int code = (int)instruction.operand1;
			Process receiver = (Process)currentFrame.peek();
			currentFrame.set0(getBoolean(receiver.isDefined(code)));
			currentFrame.instructionPointer++;
			
			break;
		}
		
		case Instruction.OPCODE_SLOTS_SET: {
			Process value = currentFrame.peek();
			StringProcess key = (StringProcess)currentFrame.peek1();
			Process receiver = (Process)currentFrame.peek2();
			currentFrame.pop3();
			int code = symbolTable.getSymbolCodeFromId(Selector.get(key.str));
			receiver.define(code, value);
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_SLOTS_SET_PROTO: {
			Process value = currentFrame.peek();
			StringProcess key = (StringProcess)currentFrame.peek1();
			Process receiver = (Process)currentFrame.peek2();
			currentFrame.pop3();
			int code = symbolTable.getSymbolCodeFromId(Selector.get(key.str));
			receiver.defineProto(code, value);
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_SLOTS_GET: {
			StringProcess key = (StringProcess)currentFrame.peek();
			Process receiver = (Process)currentFrame.peek1();
			int code = symbolTable.getSymbolCodeFromId(Selector.get(key.str));
			Process value = receiver.lookup(code);
			currentFrame.set1(value);
			currentFrame.pop1();
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_SLOTS_IS_DEFINED: {
			StringProcess key = (StringProcess)currentFrame.peek();
			Process receiver = (Process)currentFrame.peek1();
			int code = symbolTable.getSymbolCodeFromId(Selector.get(key.str));
			Process value = receiver.lookup(code);
			currentFrame.set1(getBoolean(value != null));
			currentFrame.pop1();
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_SLOTS_NAMES: {
			Process receiver = (Process)currentFrame.peek();
			String[] rawNames = receiver.getNames();
			Process[] names = new Process[rawNames.length];
			for(int i = 0; i < names.length; i++)
				names[i] = createString(rawNames[i]);
			ArrayProcess namesArray = new ArrayProcess(protoArray, names);
			currentFrame.set0(namesArray);
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_CALL_CLOSURE: {
			ClosureProcess closure = (ClosureProcess)currentFrame.peek();
			BehaviorProcess behavior = closure.behavior;
			Frame frame = closure.frame;
			currentFrame.copyNInto(closure.argumentOffset, frame.locals, closure.parameterCount, 1);
			currentFrame.popN(closure.parameterCount);
			currentFrame = new Frame(currentFrame, frame.locals, behavior.frameInfo.instructions, behavior.frameInfo.maxStackSize);
			
			break;
		} case Instruction.OPCODE_CALL_CLOSURE_0: {
			ClosureProcess closure = (ClosureProcess)currentFrame.pop();
			BehaviorProcess behavior = closure.behavior;
			Frame frame = closure.frame;
			currentFrame = new Frame(currentFrame, frame.locals, behavior.frameInfo.instructions, behavior.frameInfo.maxStackSize);
			
			break;
		} case Instruction.OPCODE_CALL_CLOSURE_1: {
			ClosureProcess closure = (ClosureProcess)currentFrame.peek();
			BehaviorProcess behavior = closure.behavior;
			Frame frame = closure.frame;
			frame.locals[closure.argumentOffset] = currentFrame.peek1();
			currentFrame.pop2();
			currentFrame = new Frame(currentFrame, frame.locals, behavior.frameInfo.instructions, behavior.frameInfo.maxStackSize);
			
			break;
		} case Instruction.OPCODE_CALL_CLOSURE_2: {
			ClosureProcess closure = (ClosureProcess)currentFrame.peek();
			BehaviorProcess behavior = closure.behavior;
			Frame frame = closure.frame;
			currentFrame.copyNInto(closure.argumentOffset, frame.locals, 2, 1);
			currentFrame.pop3();
			currentFrame = new Frame(currentFrame, frame.locals, behavior.frameInfo.instructions, behavior.frameInfo.maxStackSize);
			
			break;
		} case Instruction.OPCODE_CALL_CLOSURE_3: {
			ClosureProcess closure = (ClosureProcess)currentFrame.peek();
			BehaviorProcess behavior = closure.behavior;
			Frame frame = closure.frame;
			currentFrame.copyNInto(closure.argumentOffset, frame.locals, 3, 1);
			currentFrame.pop4();
			currentFrame = new Frame(currentFrame, frame.locals, behavior.frameInfo.instructions, behavior.frameInfo.maxStackSize);
			
			break;
		} case Instruction.OPCODE_LOAD_THIS: {
			currentFrame.push(currentFrame.locals[0]);
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_LOAD_NULL: {
			currentFrame.push(singletonNil);
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_LOAD_LOC: {
			int ordinal = (int)instruction.operand1;
			Process value = currentFrame.locals[ordinal];
			currentFrame.push(value);
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_LOAD_INT: {
			int intValue = (int)instruction.operand1;
			currentFrame.push(new IntegerProcess(protoInteger, intValue));
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_LOAD_TRUE: {
			currentFrame.push(singletonTrue);
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_LOAD_FALSE: {
			currentFrame.push(singletonFalse);
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_LOAD_STRING: {
			String str = (String)instruction.operand1;
			StringProcess string = createString(str);
			currentFrame.push(string);
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_LOAD_THIS_FRAME: {
			currentFrame.push(currentFrame.getReifiedFrame(protoFrame));
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_LOAD_BEHAVIOR: {
			BehaviorProcess behavior = (BehaviorProcess)instruction.operand1;
			currentFrame.push(behavior);
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_REPORT_ERROR: {
			@SuppressWarnings("unused")
			FrameProcess frame = (FrameProcess)currentFrame.peek();
			NativeObjectHolder nativeSignalHolder = (NativeObjectHolder)currentFrame.peek1();
			Exception nativeSignal = (Exception)nativeSignalHolder.getNativeObject();
			currentFrame.pop2();
			// Look up, and print, message slot which must be a string
			System.err.println("Error: " + nativeSignal.getMessage());
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_ENVIRONMENT: {
			Process process = currentFrame.peek();
			currentFrame.set0(process.getEnvironment());
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_SPAWN: {
			BehaviorProcess body = (BehaviorProcess)currentFrame.peek();
			Process environment = currentFrame.peek1();
			// If the end is reached, then whatever processes dependent on the active process, will stall forever and in principle the program as a whole should wait forever? or just finish? or error?
			// Or reply null?
			// Wait forever seems most appropriate when there processes dependent; otherwise just do return none because active process has already been returned
			Process[] locals = new Process[body.frameInfo.localCount];
			Frame activeProcessFrame = new Frame(currentFrame, locals, body.frameInfo.instructions, body.frameInfo.maxStackSize);
			ActiveProcess activeProcess = new ActiveProcess(environment, activeProcessFrame);
			locals[0] = activeProcess;
			currentFrame.set1(activeProcess); // Return active process
			currentFrame.pop1();
			currentFrame = activeProcessFrame;
			
			break;
		} case Instruction.OPCODE_RECEIVE: {
			currentFrame = currentFrame.sender;
			currentFrame.instructionPointer++;
			
			break;
		}
		
		// Special opcodes
		case Instruction.OPCODE_SP_BOOLEAN_OR: {
			BooleanProcess rhs = (BooleanProcess)currentFrame.peek();
			BooleanProcess lhs = (BooleanProcess)currentFrame.peek1();
			currentFrame.set1(getBoolean(lhs.value || rhs.value));
			currentFrame.pop1();
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_SP_BOOLEAN_AND: {
			BooleanProcess rhs = (BooleanProcess)currentFrame.peek();
			BooleanProcess lhs = (BooleanProcess)currentFrame.peek1();
			currentFrame.set1(getBoolean(lhs.value && rhs.value));
			currentFrame.pop1();
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_SP_BOOLEAN_NOT: {
			BooleanProcess b = (BooleanProcess)currentFrame.peek();
			currentFrame.set0(getBoolean(!b.value));
			currentFrame.instructionPointer++;
			
			break;
		}  case Instruction.OPCODE_SP_ARRAY_GET: {
			IntegerProcess index = (IntegerProcess)currentFrame.peek();
			ArrayProcess array = (ArrayProcess)currentFrame.peek1();
			currentFrame.set1(array.get(index.intValue));
			currentFrame.pop1();
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_SP_ARRAY_SET: {
			Process value = currentFrame.peek();
			IntegerProcess index = (IntegerProcess)currentFrame.peek1();
			ArrayProcess array = (ArrayProcess)currentFrame.peek2();
			currentFrame.pop3();
			array.set(index.intValue, value);
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_SP_STRING_CONCAT: {
			StringProcess rhs = (StringProcess)currentFrame.peek();
			StringProcess lhs = (StringProcess)currentFrame.peek1();
			currentFrame.set1(createString(lhs.str + rhs.str));
			currentFrame.pop1();
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_SP_STRING_EQUAL: {
			StringProcess rhs = (StringProcess)currentFrame.peek();
			StringProcess lhs = (StringProcess)currentFrame.peek1();
			currentFrame.set1(getBoolean(lhs.str.equals(rhs.str)));
			currentFrame.pop1();
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_SP_INT_ADD: {
			IntegerProcess rhs = (IntegerProcess)currentFrame.peek();
			IntegerProcess lhs = (IntegerProcess)currentFrame.peek1();
			currentFrame.set1(new IntegerProcess(protoInteger, lhs.intValue + rhs.intValue));
			currentFrame.pop1();
			currentFrame.instructionPointer++;
			
			break;
		}case Instruction.OPCODE_SP_INT_SUB: {
			IntegerProcess rhs = (IntegerProcess)currentFrame.peek();
			IntegerProcess lhs = (IntegerProcess)currentFrame.peek1();
			currentFrame.set1(new IntegerProcess(protoInteger, lhs.intValue - rhs.intValue));
			currentFrame.pop1();
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_SP_INT_MULT: {
			IntegerProcess rhs = (IntegerProcess)currentFrame.peek();
			IntegerProcess lhs = (IntegerProcess)currentFrame.peek1();
			currentFrame.set1(new IntegerProcess(protoInteger, lhs.intValue * rhs.intValue));
			currentFrame.pop1();
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_SP_INT_DIV: {
			IntegerProcess rhs = (IntegerProcess)currentFrame.peek();
			IntegerProcess lhs = (IntegerProcess)currentFrame.peek1();
			currentFrame.set1(new IntegerProcess(protoInteger, lhs.intValue / rhs.intValue));
			currentFrame.pop1();
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_SP_INT_REM: {
			IntegerProcess rhs = (IntegerProcess)currentFrame.peek();
			IntegerProcess lhs = (IntegerProcess)currentFrame.peek1();
			currentFrame.set1(new IntegerProcess(protoInteger, lhs.intValue % rhs.intValue));
			currentFrame.pop1();
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_SP_INT_EQUAL: {
			IntegerProcess rhs = (IntegerProcess)currentFrame.peek();
			IntegerProcess lhs = (IntegerProcess)currentFrame.peek1();
			currentFrame.set1(getBoolean(lhs.intValue == rhs.intValue));
			currentFrame.pop1();
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_SP_INT_GREATER: {
			IntegerProcess rhs = (IntegerProcess)currentFrame.peek();
			IntegerProcess lhs = (IntegerProcess)currentFrame.peek1();
			currentFrame.set1(getBoolean(lhs.intValue > rhs.intValue));
			currentFrame.pop1();
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_SP_INT_LESS: {
			IntegerProcess rhs = (IntegerProcess)currentFrame.peek();
			IntegerProcess lhs = (IntegerProcess)currentFrame.peek1();
			currentFrame.set1(getBoolean(lhs.intValue < rhs.intValue));
			currentFrame.pop1();
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_SP_INT_TO_STRING: {
			IntegerProcess integer = (IntegerProcess)currentFrame.peek();
			currentFrame.set0(createString(Integer.toString(integer.intValue)));
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_SP_FRAME_SENDER: {
			FrameProcess frame = (FrameProcess)currentFrame.peek();
			if(frame.frame.sender != null)
				currentFrame.set0(frame.frame.sender.getReifiedFrame(protoFrame));
			else
				currentFrame.set0(singletonNil);
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_SP_FRAME_SET_SENDER: {
			FrameProcess newSender = (FrameProcess)currentFrame.peek();
			FrameProcess frame = (FrameProcess)currentFrame.peek1();
			currentFrame.pop2();
			frame.frame.sender = newSender.frame;
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_SP_FRAME_RESUME: {
			Process value = currentFrame.peek();
			FrameProcess frame = (FrameProcess)currentFrame.peek1();
			currentFrame.pop2();
			currentFrame = frame.frame;
			currentFrame.push(value);
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_SP_REF_EQUAL: {
			Process rhs = currentFrame.peek();
			Process lhs = currentFrame.peek1();
			currentFrame.set1(getBoolean(lhs == rhs));
			currentFrame.pop1();
			currentFrame.instructionPointer++;
			
			break;
		}/* case Instruction.OPCODE_MESSAGE_ID: {
			ActiveProcess ap = (ActiveProcess)currentFrame.locals[0];
			String idStr = ap.getMessageId(symbolTable);
			currentFrame.push(new StringProcess(protoString, idStr));
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_MESSAGE_ARITY: {
			ActiveProcess ap = (ActiveProcess)currentFrame.locals[0];
			currentFrame.push(new IntegerProcess(protoInteger, ap.getMessageArity()));
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_MESSAGE_ARG: {
			ActiveProcess ap = (ActiveProcess)currentFrame.locals[0];
			Process ordinal = currentFrame.peek();
			if(ordinal instanceof IntegerProcess) {
				int ordinalValue = ((IntegerProcess)ordinal).intValue;
				int arity = ap.getMessageArity();
				if(ordinalValue < arity) {
					Process arg = ap.getMessageArg(ordinalValue);
					currentFrame.set0(arg);
					currentFrame.instructionPointer++;
				} else {
					throw new RuntimeException("Ordinal " + ordinalValue + " for message arg is greater than or equal to message arity " + arity + ".");
				}
			} else {
				throw new RuntimeException("Ordinal for message arg must be integer.");
			}
			
			break;
		}*/ case Instruction.OPCODE_ENVELOPE_REPLY: {
			Process reply = currentFrame.peek();
			EnvelopeProcess envelope = (EnvelopeProcess)currentFrame.peek1();
			envelope.sender.push(reply);
			envelope.receiver.sentReply();
			currentFrame.pop2();
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_ENVELOPE_RECEIVER: {
			EnvelopeProcess envelope = (EnvelopeProcess)currentFrame.peek();
			currentFrame.set0(envelope.receiver);
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_ENVELOPE_SENDER: {
			EnvelopeProcess envelope = (EnvelopeProcess)currentFrame.peek();
			currentFrame.set0(envelope.sender.getReifiedFrame(protoFrame));
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_ENVELOPE_MESSAGE: {
			EnvelopeProcess envelope = (EnvelopeProcess)currentFrame.peek();
			currentFrame.set0(envelope.message);
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_HALT: {
//			ActiveProcess ap = (ActiveProcess)currentFrame.locals[0];
//			ap.halt();
			currentFrame = currentFrame.sender;
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_SP_NEW_DICT: {
			Process prototype = currentFrame.peek();
			DictionaryProcess newDict = new DictionaryProcess();
			newDict.defineProto(SymbolTable.Codes.prototype, prototype);
			currentFrame.set0(newDict);
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_SP_NEW_ARRAY: {
			IntegerProcess length = (IntegerProcess)currentFrame.peek();
			ArrayProcess newArray = new ArrayProcess(protoArray, length.intValue, singletonNil);
			currentFrame.set0(newArray);
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_SP_ARRAY_LENGTH: {
			ArrayProcess newArray = (ArrayProcess)currentFrame.peek();
			currentFrame.set0(new IntegerProcess(protoInteger, newArray.length()));
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_SP_LOAD: {
			StringProcess pathSource = (StringProcess)currentFrame.pop();
			String path = pathSource.str;
			try {
				/*
				String sourcePath = getPathInCommons( path + ".drs");
				String codePath = getPathInCommons( path + ".drr");

				System.out.println("sourcePath=" + sourcePath);
				
				//Compiler compiler = new Compiler();
				FrameInfo processFrame = compiler.loadFrame(sourcePath, codePath);*/

				FrameInfo processFrame = frameLoader.load(path, compiler);
				// Assumed to end with finish instruction. Replace finish with pop_frame.
				processFrame.instructions[processFrame.instructions.length - 1] = new Instruction(Instruction.OPCODE_RET_NONE);
				Process[] locals = new Process[processFrame.localCount];
				locals[0] = protoAny;
//				customProcess.currentFrame.locals = new Process[]{protoAny};
				currentFrame = new Frame(
					currentFrame, locals, processFrame.instructions, processFrame.maxStackSize);
			} catch (ClassNotFoundException | IOException | CompilationException e) {
				e.printStackTrace();
//				e.printStackTrace();
				throw new RuntimeException("Could not load " + path + ":\n" + e.getMessage(), e);
			}
			
			break;
		} case Instruction.OPCODE_SP_NEW_CLOSURE: {
			int argumentOffset = (int)instruction.operand1;
			int parameterCount = (int)instruction.operand2;
			
			BehaviorProcess behavior = (BehaviorProcess)currentFrame.peek();
			ClosureProcess closure = new ClosureProcess(protoClosure, currentFrame, behavior, argumentOffset, parameterCount);
			currentFrame.set0(closure);
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_SP_NEW_BEHAVIOR: {
			int localCount = (int)instruction.operand1;
			int maxStackSize = (int)instruction.operand2;
			Instruction[] instructions = (Instruction[])instruction.operand3;
			BehaviorProcess behavior = new BehaviorProcess(protoBehavior, new FrameInfo(localCount, maxStackSize, instructions));
			currentFrame.push(behavior);
			currentFrame.instructionPointer++;
			
			break;
		}/* case Instruction.OPCODE_SP_CLONE: {
			DictionaryProcess dict = (DictionaryProcess)currentFrame.peek();
			DictionaryProcess clone = dict.clone();
			currentFrame.set0(clone);
			currentFrame.instructionPointer++;
			
			break;
		}*/ case Instruction.OPCODE_NATIVE_CLASS_FIELD: {
			String className = (String)instruction.operand1;
			String fieldName = (String)instruction.operand2;
			try {
				Class<?> theClass = Class.forName(className);
				Field field = theClass.getField(fieldName);
				if(field.getType().equals(int.class))
					currentFrame.instructions[currentFrame.instructionPointer] = new Instruction(Instruction.OPCODE_NATIVE_CLASS_FIELD_INT, field);
				else if(field.getType().equals(String.class))
					currentFrame.instructions[currentFrame.instructionPointer] = new Instruction(Instruction.OPCODE_NATIVE_CLASS_FIELD_STRING, field);
				else
					currentFrame.instructions[currentFrame.instructionPointer] = new Instruction(Instruction.OPCODE_NATIVE_CLASS_FIELD_OTHER, field);
			} catch (ClassNotFoundException | NoSuchFieldException | SecurityException e) {
				// Call handler
				e.printStackTrace();
				currentFrame.instructionPointer++;
			}
			
			break;
		} case Instruction.OPCODE_NATIVE_CLASS_FIELD_INT: {
			Field field = (Field)instruction.operand1;
			try {
				int nativeObject = field.getInt(null);
				currentFrame.push(new IntegerProcess(protoInteger, nativeObject));
			} catch (IllegalArgumentException | IllegalAccessException e) {
				// Call handler
				currentFrame.push(new IntegerProcess(protoInteger, 0));
				e.printStackTrace();
			}
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_NATIVE_CLASS_FIELD_STRING: {
			Field field = (Field)instruction.operand1;
			try {
				String nativeObject = (String)field.get(null);
				currentFrame.push(new StringProcess(protoString, nativeObject));
			} catch (IllegalArgumentException | IllegalAccessException e) {
				// Call handler
				currentFrame.push(new StringProcess(protoString, ""));
				e.printStackTrace();
			}
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_NATIVE_CLASS_FIELD_OTHER: {
			Field field = (Field)instruction.operand1;
			try {
				Object nativeObject = field.get(null);
				currentFrame.push(wrapNativeObject(nativeObject));
			} catch (IllegalArgumentException | IllegalAccessException e) {
				// Call handler
				currentFrame.push(singletonNil);
				e.printStackTrace();
			}
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_NATIVE_INSTANCE_INVOKE: {
			String className = (String)instruction.operand1;
			String methodName = (String)instruction.operand2;
			String[] methodParameters = (String[])instruction.operand3;
			try {
				Class<?> theClass = Class.forName(className);
				Class<?>[] parameterTypes = new Class<?>[methodParameters.length];
				for(int i = 0; i < methodParameters.length; i++)
					parameterTypes[i] = Class.forName(methodParameters[0]);
				Method method = theClass.getMethod(methodName, parameterTypes);
				if(method.getReturnType().equals(int.class))
					currentFrame.instructions[currentFrame.instructionPointer] = new Instruction(Instruction.OPCODE_NATIVE_INSTANCE_INVOKE_INT, method);
				else if(method.getReturnType().equals(String.class))
					currentFrame.instructions[currentFrame.instructionPointer] = new Instruction(Instruction.OPCODE_NATIVE_INSTANCE_INVOKE_STRING, method);
				else
					currentFrame.instructions[currentFrame.instructionPointer] = new Instruction(Instruction.OPCODE_NATIVE_INSTANCE_INVOKE_OTHER, method);
			} catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
				// Call handler
				e.printStackTrace();
				currentFrame.instructionPointer++;
			}
			
			break;
		} case Instruction.OPCODE_NATIVE_INSTANCE_INVOKE_INT: {
			Method method = (Method)instruction.operand1;
			Object[] arguments = new Object[method.getParameterCount()];
			for(int i = method.getParameterCount() - 1; i >= 0; i--) {
				NativeInteroperable nativeInteroperable = (NativeInteroperable)currentFrame.pop();
				arguments[i] = nativeInteroperable.getNativeObject();
			}
			NativeInteroperable receiver = (NativeInteroperable)currentFrame.pop();
			Object nativeReceiver = receiver.getNativeObject();
			try {
				int nativeObject = (int)method.invoke(nativeReceiver, arguments);
				currentFrame.push(new IntegerProcess(protoInteger, nativeObject));
			} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
				// Call handler
				currentFrame.push(new IntegerProcess(protoInteger, 0));
				e.printStackTrace();
			}
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_NATIVE_INSTANCE_INVOKE_STRING: {
			Method method = (Method)instruction.operand1;
			Object[] arguments = new Object[method.getParameterCount()];
			for(int i = method.getParameterCount() - 1; i >= 0; i--) {
				NativeInteroperable nativeInteroperable = (NativeInteroperable)currentFrame.pop();
				arguments[i] = nativeInteroperable.getNativeObject();
			}
			NativeInteroperable receiver = (NativeInteroperable)currentFrame.pop();
			Object nativeReceiver = receiver.getNativeObject();
			try {
				String nativeObject = (String)method.invoke(nativeReceiver, arguments);
				currentFrame.push(new StringProcess(protoString, nativeObject));
			} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
				// Call handler
				currentFrame.push(new StringProcess(protoString, ""));
				e.printStackTrace();
			}
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_NATIVE_INSTANCE_INVOKE_OTHER: {
			Method method = (Method)instruction.operand1;
			Object[] arguments = new Object[method.getParameterCount()];
			for(int i = method.getParameterCount() - 1; i >= 0; i--) {
				NativeInteroperable nativeInteroperable = (NativeInteroperable)currentFrame.pop();
				arguments[i] = nativeInteroperable.getNativeObject();
			}
			NativeInteroperable receiver = (NativeInteroperable)currentFrame.pop();
			Object nativeReceiver = receiver.getNativeObject();
			try {
				Object nativeObject = method.invoke(nativeReceiver, arguments);
				currentFrame.push(wrapNativeObject(nativeObject));
			} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
				// Call handler
				currentFrame.push(singletonNil);
				e.printStackTrace();
			}
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_NATIVE_NEW_INSTANCE: {
			String className = (String)instruction.operand1;
			String[] methodParameters = (String[])instruction.operand2;
			try {
				Class<?> theClass = Class.forName(className);
				Class<?>[] parameterTypes = new Class<?>[methodParameters.length];
				for(int i = 0; i < methodParameters.length; i++)
					parameterTypes[i] = Class.forName(methodParameters[0]);
				Constructor<?> constructor = theClass.getConstructor(parameterTypes);
				if(theClass.equals(String.class))
					currentFrame.instructions[currentFrame.instructionPointer] = new Instruction(Instruction.OPCODE_NATIVE_NEW_INSTANCE_STRING, constructor);
				else
					currentFrame.instructions[currentFrame.instructionPointer] = new Instruction(Instruction.OPCODE_NATIVE_NEW_INSTANCE_OTHER, constructor);
			} catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
				// Call handler
				e.printStackTrace();
				currentFrame.instructionPointer++;
			}
			
			break;
		} case Instruction.OPCODE_NATIVE_NEW_INSTANCE_STRING: {
			Constructor<?> constructor = (Constructor<?>)instruction.operand1;
			Object[] arguments = new Object[constructor.getParameterCount()];
			for(int i = constructor.getParameterCount() - 1; i >= 0; i--) {
				NativeInteroperable nativeInteroperable = (NativeInteroperable)currentFrame.pop();
				arguments[i] = nativeInteroperable.getNativeObject();
			}
			try {
				String nativeObject = (String)constructor.newInstance(arguments);
				currentFrame.push(new StringProcess(protoString, nativeObject));
			} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
				// Call handler
				currentFrame.push(new StringProcess(protoString, ""));
				e.printStackTrace();
			}
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_NATIVE_NEW_INSTANCE_OTHER: {
			Constructor<?> constructor = (Constructor<?>)instruction.operand1;
			Object[] arguments = new Object[constructor.getParameterCount()];
			for(int i = constructor.getParameterCount() - 1; i >= 0; i--) {
				NativeInteroperable nativeInteroperable = (NativeInteroperable)currentFrame.pop();
				arguments[i] = nativeInteroperable.getNativeObject();
			}
			try {
				Object nativeObject = constructor.newInstance(arguments);
				currentFrame.push(wrapNativeObject(nativeObject));
			} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
				// Call handler
				currentFrame.push(singletonNil);
				e.printStackTrace();
			}
			currentFrame.instructionPointer++;
			
			break;
		} case Instruction.OPCODE_NONE: {
			// Loop forever
			break;
		} case Instruction.OPCODE_BREAK_POINT: {
			currentFrame.instructionPointer++;
			break;
		}
		}
	}

	private String getPathInCommons(String path) {
		if(path.startsWith("/"))
			return currentPath + "/" + commonsPath + path;
		else
			return currentPath + "/" + commonsPath + "/" + path;
	}

	private Process wrapNativeObject(Object nativeObject) {
		return nativeObject != null ? new NativeObjectHolder(nativeObject) : singletonNil;
	}
	
	public final BooleanProcess getBoolean(boolean value) {
		return value ? singletonTrue : singletonFalse;
	}

	public final StringProcess createString(String str) {
		return new StringProcess(protoString, str);
	}

	public final Process getEnvelopeFromHere(Process receiver, int selectorCode, int arity) {
		Frame sender = currentFrame;
		DictionaryProcess message = protoAny.derive();
		message.define(SymbolTable.Codes.id, new StringProcess(protoString, symbolTable.getIdFromSymbolCode(selectorCode).getId()));
		Process[] argumentsArray = new Process[arity];
		sender.copyNInto(0, argumentsArray, arity);
		sender.popN(1 + arity);
		ArrayProcess arguments = new ArrayProcess(protoArray, argumentsArray);
		message.define(SymbolTable.Codes.arguments, arguments);

		return new EnvelopeProcess(protoEnvelope, receiver, sender, message);
	}
}
