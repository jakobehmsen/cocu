package cocu.runtime;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.function.BiFunction;

import cocu.reflang.SymbolTable;

public class Instruction implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final int OPCODE_PAUSE = 0;
	public static final int OPCODE_INC_IP = 1;
	public static final int OPCODE_FINISH = 2;
	@PushCount(1)
	public static final int OPCODE_DUP = 3;
	@PushCount(1)
	public static final int OPCODE_DUP1 = 4;
//	@PushCount(1)
//	public static final int OPCODE_DUP2 = 5;
//	@PushCount(1)
//	public static final int OPCODE_DUP_ANY = 6;
	@PopCount(1)
	public static final int OPCODE_STORE_LOC = 7;
	public static final int OPCODE_POP = 8;
//	@PopCount(1)
//	public static final int OPCODE_SWAP = 9;
//	public static final int OPCODE_SWAP1 = 10;
//	public static final int OPCODE_SWAP_ANY = 11;
	@PopCount(source = PopCount.Source.ReceiverPlusOperand.class, value = 1)
	@PushCount(1)
	public static final int OPCODE_SEND = 12;
	@PopCount(source = PopCount.Source.ReceiverPlusOperand.class, value = 1)
	@PushCount(1)
	public static final int OPCODE_SEND_CODE = 13;
	@PopCount(source = PopCount.Source.ReceiverPlusOperand.class, value = 1, timing = PopCount.Timing.BEFORE_ERROR)
	@PushCount(1)
	public static final int OPCODE_SEND_CODE_0 = 14;
	@PopCount(source = PopCount.Source.ReceiverPlusOperand.class, value = 1)
	@PushCount(1)
	public static final int OPCODE_SEND_CODE_1 = 15;
	@PopCount(source = PopCount.Source.ReceiverPlusOperand.class, value = 1)
	@PushCount(1)
	public static final int OPCODE_SEND_CODE_2 = 16;
	@PopCount(source = PopCount.Source.ReceiverPlusOperand.class, value = 1)
	@PushCount(1)
	public static final int OPCODE_SEND_CODE_3 = 17;
	public static final int OPCODE_DISPATCH = 18; // Sending messages based on first class values; receiver, sender/return point, message
	public static final int OPCODE_CALL = 19;
	public static final int OPCODE_FORWARD_CALL = 20;
	@PopCount(1)
	public static final int OPCODE_RET = 21;
	public static final int OPCODE_RET_NONE = 22;
//	public static final int OPCODE_RET_FORWARD = 22;
	public static final int OPCODE_IF_TRUE = 23;
	public static final int OPCODE_IF_FALSE = 24;
	public static final int OPCODE_JUMP = 25;
	@PopCount(2)
	public static final int OPCODE_SET = 26;
	public static final int OPCODE_SET_CODE = 27;
	/*@PopCount(2)
	public static final int OPCODE_SET_SHARED = 28;
	public static final int OPCODE_SET_SHARED_CODE = 29;*/
	@PopCount(2)
	public static final int OPCODE_SET_PROTO = 30;
	public static final int OPCODE_SET_PROTO_CODE = 31;
	@PopCount(1)
	@PushCount(1)
	public static final int OPCODE_GET = 32;
	@PopCount(1)
	@PushCount(1)
	public static final int OPCODE_GET_CODE = 33;
	@PopCount(1)
	@PushCount(1)
	public static final int OPCODE_IS_DEFINED = 34;
	@PopCount(1)
	@PushCount(1)
	public static final int OPCODE_IS_DEFINED_CODE = 35;
	@PopCount(3)
	public static final int OPCODE_SLOTS_SET = 36;
	@PopCount(3)
	public static final int OPCODE_SLOTS_SET_PROTO = 37;
	@PopCount(3)
	@PushCount(1)
	public static final int OPCODE_SLOTS_GET = 38;
	@PopCount(2)
	@PushCount(1)
	public static final int OPCODE_SLOTS_IS_DEFINED = 39;
	@PopCount(1)
	@PushCount(1)
	public static final int OPCODE_SLOTS_NAMES = 40;
	@PopCount(source = PopCount.Source.Operand.class, value = 0)
	@PushCount(1)
	public static final int OPCODE_CALL_CLOSURE = 42;
	@PopCount(value = 1, timing = PopCount.Timing.BEFORE_ERROR)
	@PushCount(1)
	public static final int OPCODE_CALL_CLOSURE_0 = 43;
	@PopCount(2)
	@PushCount(1)
	public static final int OPCODE_CALL_CLOSURE_1 = 44;
	@PopCount(3)
	@PushCount(1)
	public static final int OPCODE_CALL_CLOSURE_2 = 45;
	@PopCount(4)
	@PushCount(1)
	public static final int OPCODE_CALL_CLOSURE_3 = 46;
	public static final int OPCODE_EXTEND_INTER_ID = 47;
	@PushCount(1)
	public static final int OPCODE_SHRINK_INTER_ID = 48;
	@PushCount(1)
	public static final int OPCODE_LOAD_THIS = 49;
	@PushCount(1)
	public static final int OPCODE_LOAD_NULL = 50;
	@PushCount(1)
	public static final int OPCODE_LOAD_LOC = 51;
	@PushCount(1)
	public static final int OPCODE_LOAD_INT = 52;
	@PushCount(1)
	public static final int OPCODE_LOAD_TRUE = 53;
	@PushCount(1)
	public static final int OPCODE_LOAD_FALSE = 54;
	@PushCount(1)
	public static final int OPCODE_LOAD_STRING = 55;
	@PushCount(1)
	public static final int OPCODE_LOAD_THIS_FRAME = 56;
	@PushCount(1)
	public static final int OPCODE_LOAD_BEHAVIOR = 57;
	@PopCount(2)
	public static final int OPCODE_REPORT_ERROR = 58;
	@PopCount(1)
	@PushCount(1)
	public static final int OPCODE_ENVIRONMENT = 60;
	@PopCount(2)
	@PushCount(1)
	public static final int OPCODE_SPAWN = 61;
	@PushCount(1)
	public static final int OPCODE_RECEIVE = 62;

	@PopCount(2)
	@PushCount(1)
	public static final int OPCODE_SP_BOOLEAN_OR = 65;
	@PopCount(2)
	@PushCount(1)
	public static final int OPCODE_SP_BOOLEAN_AND = 66;
	@PopCount(1)
	@PushCount(1)
	public static final int OPCODE_SP_BOOLEAN_NOT = 67;
	@PopCount(2)
	@PushCount(1)
	public static final int OPCODE_SP_ARRAY_GET = 68;
	@PopCount(3)
	public static final int OPCODE_SP_ARRAY_SET = 69;
	@PopCount(2)
	@PushCount(1)
	public static final int OPCODE_SP_STRING_CONCAT = 70;
	@PopCount(2)
	@PushCount(1)
	public static final int OPCODE_SP_STRING_EQUAL = 71;
	@PopCount(2)
	@PushCount(1)
	public static final int OPCODE_SP_INT_ADD = 72;
	@PopCount(2)
	@PushCount(1)
	public static final int OPCODE_SP_INT_SUB = 73;
	@PopCount(2)
	@PushCount(1)
	public static final int OPCODE_SP_INT_MULT = 74;
	@PopCount(2)
	@PushCount(1)
	public static final int OPCODE_SP_INT_DIV = 75;
	@PopCount(2)
	@PushCount(1)
	public static final int OPCODE_SP_INT_REM = 76;
	@PopCount(2)
	@PushCount(1)
	public static final int OPCODE_SP_INT_EQUAL = 77;
	@PopCount(2)
	@PushCount(1)
	public static final int OPCODE_SP_INT_GREATER = 78;
	@PopCount(2)
	@PushCount(1)
	public static final int OPCODE_SP_INT_LESS = 79;
	@PopCount(1)
	@PushCount(1)
	public static final int OPCODE_SP_INT_TO_STRING = 80;
	@PopCount(1)
	@PushCount(1)
	public static final int OPCODE_SP_FRAME_SENDER = 81;
	@PopCount(1)
	public static final int OPCODE_SP_FRAME_SET_SENDER = 82;
	@PopCount(2)
	public static final int OPCODE_SP_FRAME_RESUME = 83;
	@PopCount(2)
	@PushCount(1)
	public static final int OPCODE_SP_REF_EQUAL = 84;
//	@PushCount(1)
//	public static final int OPCODE_MESSAGE_ID = 85;
//	@PushCount(1)
//	public static final int OPCODE_MESSAGE_ARITY = 86;
//	@PopCount(1)
//	@PushCount(1)
//	public static final int OPCODE_MESSAGE_ARG = 87;
	@PopCount(2)
	public static final int OPCODE_ENVELOPE_REPLY = 88;
	@PopCount(1)
	@PushCount(1)
	public static final int OPCODE_ENVELOPE_RECEIVER = 89;
	@PopCount(1)
	@PushCount(1)
	public static final int OPCODE_ENVELOPE_SENDER = 90;
	@PopCount(1)
	@PushCount(1)
	public static final int OPCODE_ENVELOPE_MESSAGE = 91;
	public static final int OPCODE_HALT = 92;

	@PushCount(1)
	public static final int OPCODE_SP_NEW_DICT = 129;
	@PopCount(1)
	@PushCount(1)
	public static final int OPCODE_SP_NEW_ARRAY = 130;
	@PopCount(1)
	@PushCount(1)
	public static final int OPCODE_SP_ARRAY_LENGTH = 131;
	@PopCount(value = 1, timing = PopCount.Timing.BEFORE_ERROR)
	public static final int OPCODE_SP_LOAD = 132;
	@PopCount(1)
	@PushCount(1)
	public static final int OPCODE_SP_NEW_CLOSURE = 133;
	@PushCount(1)
	public static final int OPCODE_SP_NEW_BEHAVIOR = 134;
//	@PopCount(1)
//	@PushCount(1)
//	public static final int OPCODE_SP_CLONE = 135;
	@PushCount(1)
	public static final int OPCODE_NATIVE_CLASS_FIELD = 136;
	public static final int OPCODE_NATIVE_CLASS_FIELD_INT = 137;
	public static final int OPCODE_NATIVE_CLASS_FIELD_STRING = 138;
	public static final int OPCODE_NATIVE_CLASS_FIELD_OTHER = 139;
	@PopCount(source = PopCount.Source.OperandArrayLength.class, value = 2)
	@PushCount(1)
	public static final int OPCODE_NATIVE_INSTANCE_INVOKE = 140;
	public static final int OPCODE_NATIVE_INSTANCE_INVOKE_INT = 141;
	public static final int OPCODE_NATIVE_INSTANCE_INVOKE_STRING = 142;
	public static final int OPCODE_NATIVE_INSTANCE_INVOKE_OTHER = 143;
	@PopCount(source = PopCount.Source.OperandArrayLength.class, value = 1)
	@PushCount(1)
	public static final int OPCODE_NATIVE_NEW_INSTANCE = 144;
	public static final int OPCODE_NATIVE_NEW_INSTANCE_STRING = 145;
	public static final int OPCODE_NATIVE_NEW_INSTANCE_OTHER = 146;

	public static final int OPCODE_NONE = 254;
	public static final int OPCODE_BREAK_POINT = 255;

	public final int opcode;
	public final Object operand1;
	public final Object operand2;
	public final Object operand3;
	
	// Have x sets of 3 operands, where x is the number of different types, e.g. int, Instruction[], String, and whatevet 
	
	public Instruction(int opcode) {
		this(opcode, null);
	}
	
	public Instruction(int opcode, Object operand1) {
		this(opcode, operand1, null);
	}
	
	public Instruction(int opcode, Object operand1, Object operand2) {
		this(opcode, operand1, operand2, null);
	}
	
	public Instruction(int opcode, Object operand1, Object operand2, Object operand3) {
		this.opcode = opcode;
		this.operand1 = operand1;
		this.operand2 = operand2;
		this.operand3 = operand3;
	}
	
	private static class InstructionInfo {
		public final String name;
		public final PushCount pushCount;
		public final PopCount popCount;
		
		public InstructionInfo(String name, PushCount pushCount, PopCount popCount) {
			this.name = name;
			this.pushCount = pushCount;
			this.popCount = popCount;
		}
	}
	
	private static Hashtable<Integer, InstructionInfo> opcodeToIdMap;
	private static Hashtable<String, Integer> idToOpcodeMap;
	
	private static String getNameFromOpcode(int opcode) {
		ensureMapCreated();
		
		return opcodeToIdMap.get(opcode).name;
	}

	private static void ensureMapCreated() {
		if(opcodeToIdMap == null) {
			opcodeToIdMap = new Hashtable<Integer, InstructionInfo>();
			idToOpcodeMap = new Hashtable<String, Integer>();
			
			try {
				for (Field field : Instruction.class.getDeclaredFields()) {
				    if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) && field.getName().startsWith("OPCODE_")) {
				    	int fieldOpcode = (int)field.get(null);
				    	String name = field.getName().substring("OPCODE_".length());
				    	
				    	InstructionInfo info = new InstructionInfo(name, 
			    			field.getDeclaredAnnotation(PushCount.class), 
			    			field.getDeclaredAnnotation(PopCount.class));
				    	
				    	opcodeToIdMap.put(fieldOpcode, info);
				    	idToOpcodeMap.put(name.toLowerCase(), fieldOpcode);
				    }
				}
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}

	public static Integer getOpcodeFromId(String opcodeId) {
		ensureMapCreated();
		
		return idToOpcodeMap.get(opcodeId);
	}
	
	@Override
	public String toString() {
		ArrayList<Object> operands = new ArrayList<Object>();
		if(operand1 != null)
			operands.add(operand1);
		if(operand2 != null)
			operands.add(operand2);
		if(operand3 != null)
			operands.add(operand3);
		
		switch(opcode) {
		case Instruction.OPCODE_SEND_CODE:
		case Instruction.OPCODE_SEND_CODE_0:
		case Instruction.OPCODE_SEND_CODE_1:
		case Instruction.OPCODE_SEND_CODE_2:
		case Instruction.OPCODE_SEND_CODE_3:
			operands.clear();
			operands.add(0, SymbolTable.ROOT.getIdFromSymbolCode((int)operand1).getId());
			break;
		case Instruction.OPCODE_GET_CODE:
			operands.clear();
			operands.add(0, SymbolTable.ROOT.getIdFromSymbolCode((int)operand1));
			break;
		}
		return getNameFromOpcode(opcode) + operands;
	}

	public static boolean isReturn(int opcode) {
		return opcode == OPCODE_RET || opcode == OPCODE_RET_NONE/* || opcode == OPCODE_RET_FORWARD*/;
	}
	
	public Object getOperand(int index) {
		switch(index) {
		case 0:
			return operand1;
		case 1:
			return operand2;
		case 2:
			return operand3;
		}
		
		return null;
	}

	public static int getPopCount(Instruction instruction) {
		ensureMapCreated();

		PopCount popCount = opcodeToIdMap.get(instruction.opcode).popCount;
		
		if(popCount != null) {
			try {
				BiFunction<Integer, Instruction, Integer> sourceFunc = popCount.source().newInstance();
				return sourceFunc.apply(popCount.value(), instruction);
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
			}
//			switch(popCount.source()) {
//			case PopCount.Source.VALUE:
//				return popCount.value();
//			case PopCount.Source.OPERAND:
//				return (int)instruction.getOperand(popCount.value());
//			case PopCount.Source.OPERAND_ARRAY_LENGTH:
//				return ((Object[])instruction.getOperand(popCount.value())).length;
//			}
			
			return -1;
		}
		
		return 0;
	}

	public static boolean popsAfterError(Instruction instruction) {
		ensureMapCreated();

		PopCount popCount = opcodeToIdMap.get(instruction.opcode).popCount;
		return popCount == null || popCount.timing() == PopCount.Timing.AFTER_ERROR;
	}

	public static int getPushCount(Instruction instruction) {
		ensureMapCreated();

		PushCount pushCount = opcodeToIdMap.get(instruction.opcode).pushCount;
		return pushCount != null ? pushCount.value() : 0;
	}
}
