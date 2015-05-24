package cocu.reflang;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import cocu.runtime.Instruction;

public class CodeEmitter {
	// Support for labels?
	
//	private ArrayList<Instruction> instructions = new ArrayList<Instruction>();
	private ArrayList<CodeEmit> instructionProducers = new ArrayList<CodeEmit>();
//	private int stackIndex;
//	private int maxStackSize;
	
	public void addSingle(Instruction instruction) {
//		if(instruction != null) {
//			stackIndex -= Instruction.getPopCount(instruction);
//			stackIndex += Instruction.getPushCount(instruction);
//			if(stackIndex > maxStackSize)
//				maxStackSize = stackIndex;
//		} else {
//			// Assumed to be set later using the set method
//		}
		
//		stackIndex -= Instruction.getPopCount(instruction);
//		stackIndex += Instruction.getPushCount(instruction);
//		if(stackIndex > maxStackSize)
//			maxStackSize = stackIndex;
		
//		instructions.add(instruction);
		
		if(instruction != null)
			instructionProducers.add(new CodeEmit() {
				@Override
				public void allocate(List<Instruction> instructions, Map<Label, Integer> labelToIndex) {
					instructions.add(instruction);
				}
				@Override
				public void deploy(List<Instruction> instructions, int start, int end, Map<Label, Integer> labelToIndex) { }
				
			});
		else
			instructionProducers.add(new CodeEmit() {
				@Override
				public void allocate(List<Instruction> instructions, Map<Label, Integer> labelToIndex) {
					instructions.add(null);
				}
				@Override
				public void deploy(List<Instruction> instructions, int start, int end, Map<Label, Integer> labelToIndex) { }
				
			});
	}
	
	public void add(Consumer<List<Instruction>> emit) {
		instructionProducers.add(new CodeEmit() {
			@Override
			public void allocate(List<Instruction> instructions, Map<Label, Integer> labelToIndex) {
				emit.accept(instructions);
			}
			
			@Override
			public void deploy(List<Instruction> instructions, int start, int end, Map<Label, Integer> labelToIndex) { }
		});
	}
	
	public void add(CodeEmit emit) {
		instructionProducers.add(emit);
	}
	
	public void set(int index, Instruction instruction) {
		// Assumed only non stack affecting instruction are supplied here
//		instructions.set(index, instruction);

		instructionProducers.set(index, new CodeEmit() {
			@Override
			public void allocate(List<Instruction> instructions, Map<Label, Integer> labelToIndex) {
				instructions.add(instruction);
			}
			@Override
			public void deploy(List<Instruction> instructions, int start, int end, Map<Label, Integer> labelToIndex) { }
			
		});
	}
	
	private static class Interval {
		public final int start;
		public final int end;
		
		public Interval(int start, int end) {
			this.start = start;
			this.end = end;
		}
	}
	
	public CodeEmission generate() {
		ArrayList<Instruction> instructions = new ArrayList<Instruction>();
		
		Hashtable<Label, Integer> labelToIndexMap = new Hashtable<Label, Integer>();
		
		ArrayList<Interval> allocations = new ArrayList<Interval>();
		
		for(CodeEmit instructionProducer: instructionProducers) {
			int start = instructions.size();
			instructionProducer.allocate(instructions, labelToIndexMap);
			int end = instructions.size();
			allocations.add(new Interval(start, end));
		}
		
		for(int i = 0; i < instructionProducers.size(); i++) {
			CodeEmit instructionProducer = instructionProducers.get(i);
			Interval allocation = allocations.get(i);
			instructionProducer.deploy(instructions, allocation.start, allocation.end, labelToIndexMap);
		}
		
		return new CodeEmission(instructions);
	}

//	public int getMaxStackSize() {
//		return getMaxStackSize(0, 0, 0);
//	}
//
//	private int getMaxStackSize(int stackIndex, int maxStackSize, int index) {
//		for(int i = index; i < instructions.size(); ) {
//			Instruction instruction = instructions.get(i);
//			
//			switch(instruction.opcode) {
//			case Instruction.OPCODE_IF_TRUE:
//			case Instruction.OPCODE_IF_FALSE: {
//				stackIndex--;
//				int jump = (int)instruction.operand1;
//				if(jump >= 1) {
//					int maxSizeOnJump = getMaxStackSize(stackIndex, maxStackSize, i + jump);
//					int maxSizeOnStay = getMaxStackSize(stackIndex, maxStackSize, i + 1);
//					return Math.max(maxSizeOnJump, maxSizeOnStay);
//				} else {
//					return maxStackSize;
//				}
//			} case Instruction.OPCODE_JUMP: {
//				int jump = (int)instruction.operand1;
//				if(jump >= 1) {
//					i += jump;
//					break;
//				} else {
//					return maxStackSize;
//				}
//			} default:
//				stackIndex -= Instruction.getPopCount(instruction);
//				stackIndex += Instruction.getPushCount(instruction);
//				if(stackIndex > maxStackSize)
//					maxStackSize = stackIndex;
//				i++;
//			}
//		}
//		
//		return maxStackSize;
//	}

//	public int size() {
//		return instructionProducers.size();
//	}
	
	public void label(Label label) {
		instructionProducers.add(new CodeEmit() {
			@Override
			public void allocate(List<Instruction> instructions, Map<Label, Integer> labelToIndex) {
				labelToIndex.put(label, instructions.size());
			}
			
			@Override
			public void deploy(List<Instruction> instructions, int start, int end, Map<Label, Integer> labelToIndex) { }
		});
	}
	
	public void jump(Function<Integer, Instruction> jumpToInstruction, Label label) {
		instructionProducers.add(new CodeEmit() {
			@Override
			public void allocate(List<Instruction> instructions, Map<Label, Integer> labelToIndex) {
				instructions.add(null);
			}
			
			@Override
			public void deploy(List<Instruction> instructions, int start, int end, Map<Label, Integer> labelToIndex) {
				int index = labelToIndex.get(label);
				int jump = index - start;
				Instruction instruction = jumpToInstruction.apply(jump);
				instructions.set(start, instruction);
			}
		});
	}
}
