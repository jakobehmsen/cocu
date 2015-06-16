package cocu.reflang;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.dfa.DFA;

import cocu.debugging.Debug;
import cocu.io.TreeWriter;
import cocu.reflang.antlr4.CocuLexer;
import cocu.reflang.antlr4.CocuParser;
import cocu.reflang.antlr4.CocuParser.ProgramContext;
import cocu.reflang.ast.ANTLRToAST;
import cocu.reflang.ast.AST;
import cocu.reflang.ast.ASTBuilder;
import cocu.reflang.ast.ASTToCode;
import cocu.runtime.FrameInfo;
import cocu.runtime.Instruction;
import cocu.runtime.Selector;

public class Compiler {
	//private MessageCollector errors = new MessageCollector();
	private Hashtable<Selector, PrimitiveVisitorFactory> primitiveMap;

	public Compiler() {
		primitiveMap = new Hashtable<>();

		primitiveMap.put(Selector.get("ifElse", 3), new PrimitiveVisitorFactory.IfElse());
		primitiveMap.put(Selector.get("if", 2), new PrimitiveVisitorFactory.IfElse());
		primitiveMap.put(Selector.get("while", 2), new PrimitiveVisitorFactory.While());
		primitiveMap.put(Selector.get("not", 1), new PrimitiveVisitorFactory.ConstInstruction(new Instruction(Instruction.OPCODE_SP_BOOLEAN_NOT), true));
		primitiveMap.put(Selector.get("pause", 0), new PrimitiveVisitorFactory.ConstInstruction(new Instruction(Instruction.OPCODE_PAUSE), true));
		primitiveMap.put(Selector.get("finish", 0), new PrimitiveVisitorFactory.ConstInstruction(new Instruction(Instruction.OPCODE_FINISH), true));
		primitiveMap.put(Selector.get("receive", 0), new PrimitiveVisitorFactory.Receive());
//		primitiveMap.put(Selector.get("messageId", 0), new PrimitiveVisitorFactory.ConstInstruction(new Instruction(Instruction.OPCODE_MESSAGE_ID), true));
//		primitiveMap.put(Selector.get("messageArity", 0), new PrimitiveVisitorFactory.ConstInstruction(new Instruction(Instruction.OPCODE_MESSAGE_ARITY), true));
//		primitiveMap.put(Selector.get("messageArg", 1), new PrimitiveVisitorFactory.ConstInstruction(new Instruction(Instruction.OPCODE_MESSAGE_ARG), true));
		primitiveMap.put(Selector.get("envelopeReply", 2), new PrimitiveVisitorFactory.ConstInstruction(new Instruction(Instruction.OPCODE_ENVELOPE_REPLY), false));
		primitiveMap.put(Selector.get("envelopeReceiver", 1), new PrimitiveVisitorFactory.ConstInstruction(new Instruction(Instruction.OPCODE_ENVELOPE_RECEIVER), true));
		primitiveMap.put(Selector.get("envelopeSender", 1), new PrimitiveVisitorFactory.ConstInstruction(new Instruction(Instruction.OPCODE_ENVELOPE_SENDER), true));
		primitiveMap.put(Selector.get("envelopeMessage", 1), new PrimitiveVisitorFactory.ConstInstruction(new Instruction(Instruction.OPCODE_ENVELOPE_MESSAGE), true));
		primitiveMap.put(Selector.get("eval", 1), new PrimitiveVisitorFactory.Eval());
		primitiveMap.put(Selector.get("evalWith", 2), new PrimitiveVisitorFactory.Eval());
		primitiveMap.put(Selector.get("evalWithWith", 3), new PrimitiveVisitorFactory.Eval());
		primitiveMap.put(Selector.get("evalWithWithWith", 4), new PrimitiveVisitorFactory.Eval());
		primitiveMap.put(Selector.get("evalWithWithWithWith", 5), new PrimitiveVisitorFactory.Eval());
		primitiveMap.put(Selector.get("return", 1), new PrimitiveVisitorFactory.ConstInstruction(new Instruction(Instruction.OPCODE_RET), true));

		primitiveMap.put(Selector.get("classField", 2), new PrimitiveVisitorFactory.ClassField());
		primitiveMap.put(Selector.get("onInvoke", 4), new PrimitiveVisitorFactory.InstanceInvoke());
		primitiveMap.put(Selector.get("onInvokeWith", 5), new PrimitiveVisitorFactory.InstanceInvoke());
		primitiveMap.put(Selector.get("onInvokeWith", 6), new PrimitiveVisitorFactory.InstanceInvoke());
		primitiveMap.put(Selector.get("onInvokeWith", 7), new PrimitiveVisitorFactory.InstanceInvoke());
		primitiveMap.put(Selector.get("newInstance", 1), new PrimitiveVisitorFactory.NewInstance());
		primitiveMap.put(Selector.get("newInstanceWith", 1), new PrimitiveVisitorFactory.NewInstance());
		primitiveMap.put(Selector.get("newInstanceWith", 2), new PrimitiveVisitorFactory.NewInstance());
		primitiveMap.put(Selector.get("newInstanceWith", 3), new PrimitiveVisitorFactory.NewInstance());
		primitiveMap.put(Selector.get("newInstanceWith", 4), new PrimitiveVisitorFactory.NewInstance());

		primitiveMap.put(Selector.get("isSet", 2), new PrimitiveVisitorFactory.IsSet());
		primitiveMap.put(Selector.get("slotsSet", 2), new PrimitiveVisitorFactory.ConstInstruction(new Instruction(Instruction.OPCODE_SLOTS_SET), true));
		primitiveMap.put(Selector.get("slotsSetProto", 2), new PrimitiveVisitorFactory.ConstInstruction(new Instruction(Instruction.OPCODE_SLOTS_SET_PROTO), true));
		primitiveMap.put(Selector.get("slotsGet", 2), new PrimitiveVisitorFactory.ConstInstruction(new Instruction(Instruction.OPCODE_SLOTS_GET), true));
		primitiveMap.put(Selector.get("slotsIsDefined", 2), new PrimitiveVisitorFactory.ConstInstruction(new Instruction(Instruction.OPCODE_SLOTS_IS_DEFINED), true));
		primitiveMap.put(Selector.get("slotsNames", 2), new PrimitiveVisitorFactory.ConstInstruction(new Instruction(Instruction.OPCODE_SLOTS_NAMES), true));

		primitiveMap.put(Selector.get("arrayGet", 2), new PrimitiveVisitorFactory.ConstInstruction(new Instruction(Instruction.OPCODE_SP_ARRAY_GET), true));
		primitiveMap.put(Selector.get("arraySet", 3), new PrimitiveVisitorFactory.ConstInstruction(new Instruction(Instruction.OPCODE_SP_ARRAY_SET), false));

		primitiveMap.put(Selector.get("stringConcat", 2), new PrimitiveVisitorFactory.ConstInstruction(new Instruction(Instruction.OPCODE_SP_STRING_CONCAT), true));
		primitiveMap.put(Selector.get("stringEqual", 2), new PrimitiveVisitorFactory.ConstInstruction(new Instruction(Instruction.OPCODE_SP_STRING_EQUAL), true));

		primitiveMap.put(Selector.get("intAdd", 2), new PrimitiveVisitorFactory.ConstInstruction(new Instruction(Instruction.OPCODE_SP_INT_ADD), true));
		primitiveMap.put(Selector.get("intSub", 2), new PrimitiveVisitorFactory.ConstInstruction(new Instruction(Instruction.OPCODE_SP_INT_SUB), true));
		primitiveMap.put(Selector.get("intMult", 2), new PrimitiveVisitorFactory.ConstInstruction(new Instruction(Instruction.OPCODE_SP_INT_MULT), true));
		primitiveMap.put(Selector.get("intDiv", 2), new PrimitiveVisitorFactory.ConstInstruction(new Instruction(Instruction.OPCODE_SP_INT_DIV), true));
		primitiveMap.put(Selector.get("intRem", 2), new PrimitiveVisitorFactory.ConstInstruction(new Instruction(Instruction.OPCODE_SP_INT_REM), true));
		primitiveMap.put(Selector.get("intEqual", 2), new PrimitiveVisitorFactory.ConstInstruction(new Instruction(Instruction.OPCODE_SP_INT_EQUAL), true));
		primitiveMap.put(Selector.get("intGreater", 2), new PrimitiveVisitorFactory.ConstInstruction(new Instruction(Instruction.OPCODE_SP_INT_GREATER), true));
		primitiveMap.put(Selector.get("intLess", 2), new PrimitiveVisitorFactory.ConstInstruction(new Instruction(Instruction.OPCODE_SP_INT_LESS), true));
		primitiveMap.put(Selector.get("intToString", 1), new PrimitiveVisitorFactory.ConstInstruction(new Instruction(Instruction.OPCODE_SP_INT_TO_STRING), true));

		primitiveMap.put(Selector.get("frameSender", 1), new PrimitiveVisitorFactory.ConstInstruction(new Instruction(Instruction.OPCODE_SP_FRAME_SENDER), true));
		primitiveMap.put(Selector.get("frameSetSender", 2), new PrimitiveVisitorFactory.ConstInstruction(new Instruction(Instruction.OPCODE_SP_FRAME_SET_SENDER), false));
		primitiveMap.put(Selector.get("frameResume", 2), new PrimitiveVisitorFactory.ConstInstruction(new Instruction(Instruction.OPCODE_SP_FRAME_RESUME), true));

		primitiveMap.put(Selector.get("refEqual", 2), new PrimitiveVisitorFactory.ConstInstruction(new Instruction(Instruction.OPCODE_SP_REF_EQUAL), true));
		primitiveMap.put(Selector.get("newDict", 0), new PrimitiveVisitorFactory.ConstInstruction(new Instruction(Instruction.OPCODE_SP_NEW_DICT), true));
		primitiveMap.put(Selector.get("newArray", 1), new PrimitiveVisitorFactory.ConstInstruction(new Instruction(Instruction.OPCODE_SP_NEW_ARRAY), true));
		primitiveMap.put(Selector.get("arrayLength", 1), new PrimitiveVisitorFactory.ConstInstruction(new Instruction(Instruction.OPCODE_SP_ARRAY_LENGTH), true));
		primitiveMap.put(Selector.get("load", 1), new PrimitiveVisitorFactory.ConstInstruction(new Instruction(Instruction.OPCODE_SP_LOAD), false));
//		primitiveMap.put(Selector.get("clone", 1), new PrimitiveVisitorFactory.ConstInstruction(new Instruction(Instruction.OPCODE_SP_CLONE), true));
	}
	
	/*private void appendError(int line, int charPositionInLine, String message) {
		errors.appendMessage(line, charPositionInLine, message);
	}*/
	
	/*public boolean hasErrors() {
		return errors.hasMessages();
	}*/
	
	/*public void printErrors() {
		errors.printMessages();
	}*/

	public Compilation compile(InputStream sourceCode, boolean mustBeExpression) throws IOException, CompilationException {
		Set<String> fields = new HashSet<String>();
		return compile(sourceCode, mustBeExpression, fields);
	}
	
	public Compilation compile(InputStream sourceCode, boolean mustBeExpression, Set<String> fields) throws IOException, CompilationException {
		MessageCollector errors = new MessageCollector();
		CharStream charStream = new ANTLRInputStream(sourceCode);
		CocuLexer lexer = new CocuLexer(charStream);
		CommonTokenStream tokenStream = new CommonTokenStream(lexer);
		CocuParser parser = new CocuParser(tokenStream);
		parser.removeErrorListeners();
		parser.addErrorListener(new ANTLRErrorListener() {
			@Override
			public void syntaxError(Recognizer<?,?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
				//appendError(line, charPositionInLine, msg);
				errors.appendMessage(line, charPositionInLine, msg);
			}
			
			@Override
			public void reportContextSensitivity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, int prediction, ATNConfigSet configs) {
				new String();
			}
			
			@Override
			public void reportAttemptingFullContext(Parser recognizer, DFA dfa, int startIndex, int stopIndex, java.util.BitSet conflictingAlts, ATNConfigSet configs) {
				new String();
			}
			
			@Override
			public void reportAmbiguity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, boolean exact, java.util.BitSet ambigAlts, ATNConfigSet configs) {
				new String();
			}
		});
		
		long startParse = System.currentTimeMillis();
		Debug.println(Debug.LEVEL_HIGH, "Parsing program...");
		ProgramContext programCtx;
		
		// Parsing approach following https://theantlrguy.atlassian.net/wiki/pages/viewpage.action?pageId=1900591
		parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
		try {
			programCtx = parser.program();  // STAGE 1
		}
		catch (Exception ex) {
			tokenStream.reset(); // rewind input stream
		    parser.reset();
		    parser.getInterpreter().setPredictionMode(PredictionMode.LL);
		    programCtx = parser.program();  // STAGE 2
		    // if we parse ok, it's LL not SLL
		}
		
		// Syntax errors?
		if(errors.hasMessages())
			throw new CompilationException(errors);

		long endParse = System.currentTimeMillis();
		Debug.println(Debug.LEVEL_MEDIUM, "Parsed program.");
		Debug.println(Debug.LEVEL_MEDIUM, "Parse time: " + (endParse - startParse));

		long startGen = System.currentTimeMillis();
		OrdinalAllocator idToParameterOrdinalMap = new OrdinalAllocator();
		OrdinalAllocator idToVariableOrdinalMap = new OrdinalAllocator();
		Debug.println(Debug.LEVEL_MEDIUM, "Generating program...");

		//Set<String> fields = new HashSet<String>();
		ANTLRToAST antlrProgramVisitor = new ANTLRToAST(idToParameterOrdinalMap, idToVariableOrdinalMap, errors, fields, fields);
		ASTBuilder programAstBuilder = programCtx.accept(antlrProgramVisitor);
		int parameterOffset = 1;
		idToParameterOrdinalMap.generate(parameterOffset);
		int variableOffset = parameterOffset + idToParameterOrdinalMap.size();
		idToVariableOrdinalMap.generate(variableOffset);
		AST programAst = programAstBuilder.build();

		// Semantic errors?
		if(errors.hasMessages())
			throw new CompilationException(errors);
		
		StringWriter astStringWriter = new StringWriter();
		TreeWriter astWriter = new TreeWriter(astStringWriter);
		programAst.writeTo(astWriter);
		Debug.println(Debug.LEVEL_MEDIUM, "Generated ast:\n" + astStringWriter.toString());
		
		// Postpone code generation from ast's till runtime?
		// Instead, a single instruction, GENERATE with the ast as an operand, is generated?
		CodeEmitter instructions = new CodeEmitter();
		ASTToCode programAstToCode = new ASTToCode(primitiveMap, instructions, mustBeExpression);
		programAst.accept(programAstToCode);
		
		long endGen = System.currentTimeMillis();
		Debug.println(Debug.LEVEL_MEDIUM, "Generated program.");
		Debug.println(Debug.LEVEL_MEDIUM, "Generate time: " + (endGen - startGen));
		
		CodeEmission code = instructions.generate();
		
		int localCount = 1 + idToParameterOrdinalMap.size() + idToVariableOrdinalMap.size();
		Instruction[] instructionArray = code.toArray(new Instruction[code.size()]);
		//if(mustBeExpression)
		//	instructionArray[instructionArray.length - 1] = new Instruction(Instruction.OPCODE_RET);
		FrameInfo frame = new FrameInfo(localCount, code.getMaxStackSize(), instructionArray);
		return new Compilation(frame, errors);
	}
	
	public FrameInfo loadFrame(String sourcePath, String codePath) throws FileNotFoundException, IOException, ClassNotFoundException, CompilationException {
		FrameInfo process = null;
		
		File mainObjectSourceFile = new File(sourcePath);
		File mainObjectCodeFile = new File(codePath);
		long mainObjectSourceFileLastModified = mainObjectSourceFile.lastModified();
		
		boolean shouldCompile = true;
		
		if(mainObjectCodeFile.exists()) {
			try (ObjectInput oo = new ObjectInputStream(new FileInputStream(codePath))) {
				long compilationSourceLastModified = (long) oo.readLong();
				if(compilationSourceLastModified == mainObjectSourceFileLastModified) {
					process = (FrameInfo) oo.readObject();
					shouldCompile = false;
					Debug.println(Debug.LEVEL_MEDIUM, "Loaded object file for '" + sourcePath + "'.");
				} else {
					Debug.println(Debug.LEVEL_MEDIUM, "Object file for '" + sourcePath + "' is out of date.");
				}
		    }
		} else {
			Debug.println(Debug.LEVEL_MEDIUM, "Missing object file for '" + sourcePath + "'.");
		}
		
		if(shouldCompile) {
			Debug.println(Debug.LEVEL_MEDIUM, "Compiling '" + sourcePath + "' into '" + codePath + "'...");
			FileInputStream inputStream;
			try {
				inputStream = new FileInputStream(sourcePath);
				Compilation compilation = compile(inputStream, false);
				
				if(compilation.hasErrors()) {
					System.err.println("Errors were found during compilation of '" + sourcePath + "':");
					compilation.printErrors();
				} else {
					
					try (ObjectOutput oo = new ObjectOutputStream(new FileOutputStream(codePath))) {
						oo.writeLong(mainObjectSourceFileLastModified);
				        oo.writeObject(process);
				    }
					
					Debug.println(Debug.LEVEL_MEDIUM, "Compiled '" + codePath + "' into '" + codePath + "'.");
				}
			} catch (IOException e) {
				System.err.println("Compilation of '" + sourcePath + "' failed.");
				e.printStackTrace();
			}
		}
		
		return process;
	}

	public void warmup() {
		String testSource = 
			"var x = 0\n" + 
			"var c = {if' true, true Else: false}\n" + 
			"obj = #[x = 0 y => x]";
		
		Compiler compiler = new Compiler();
		try {
			for(int i = 0; i < 10; i++)
				try {
					compiler.compile(new ByteArrayInputStream(testSource.getBytes("UTF-8")), false);
				} catch (CompilationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
