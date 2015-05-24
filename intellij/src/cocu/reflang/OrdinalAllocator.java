package cocu.reflang;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import cocu.runtime.Instruction;

public class OrdinalAllocator {
	private static class AllocationEmit implements CodeEmit {
		public Instruction instruction;

		@Override
		public void allocate(List<Instruction> instructions, Map<Label, Integer> labelToIndex) {
			instructions.add(instruction);
		}

		@Override
		public void deploy(List<Instruction> instructions, int start, int end, Map<Label, Integer> labelToIndex) { }
	}
	
	private static class AllocationInfo {
		public final String id;
		public final ArrayList<Consumer<Integer>> ordinalHandlers;
		
		public AllocationInfo(String id) {
			this.id = id;
			this.ordinalHandlers = new ArrayList<Consumer<Integer>>();
		}
	}
	
	private ArrayList<AllocationInfo> allocation = new ArrayList<AllocationInfo>();
	private OrdinalAllocator outer;
	private ArrayList<OrdinalAllocator> childrenStart = new ArrayList<OrdinalAllocator>();
	private ArrayList<OrdinalAllocator> childrenEnd = new ArrayList<OrdinalAllocator>();
	private ArrayList<Consumer<Integer>> offsetHandlers = new ArrayList<Consumer<Integer>>();
	
	public OrdinalAllocator() { }
	
	private OrdinalAllocator(OrdinalAllocator outer) {
		this.outer = outer;
	}
	
	public OrdinalAllocator newInnerStart() {
		OrdinalAllocator child = new OrdinalAllocator(this);
		childrenStart.add(child);
		return child;
	}
	
	public OrdinalAllocator newInnerEnd() {
		OrdinalAllocator child = new OrdinalAllocator(this);
		childrenEnd.add(child);
		return child;
	}
	
	public OrdinalAllocator getOuter() {
		return outer;
	}
	
	public void declare(String id, PendingCodeEmitter instructions, Function<Integer, Instruction> instructionFunc) {
		declare(id, instructionsConsumer(instructions, instructionFunc));
	}
	
	public void declare(String id) {
		declare(id, ordinal -> {});
	}
	
	public void declare(String id, Consumer<Integer> handler) {
		AllocationInfo allocationInfo = getAllocationInfo(id);

		if(allocationInfo == null) {
			allocationInfo = new AllocationInfo(id);
			allocation.add(allocationInfo);
		}
		
		allocationInfo.ordinalHandlers.add(handler);
	}
	
	public void ordinalFor(String id, PendingCodeEmitter instructions, Function<Integer, Instruction> instructionFunc) {
		ordinalFor(id, instructionsConsumer(instructions, instructionFunc));
	}
	
	public void ordinalFor(String id, Consumer<Integer> handler) {
		AllocationInfo allocationInfo = getAllocationInfo(id);
		if(allocationInfo != null)
			allocationInfo.ordinalHandlers.add(handler);
		
		if(outer != null)
			outer.ordinalFor(id, handler);
	}
	
	public Consumer<Integer> instructionsConsumer(PendingCodeEmitter instructions, Function<Integer, Instruction> instructionFunc) {
		AllocationEmit emit = new AllocationEmit();
		instructions.add(emit);
		return ordinal -> {
			Instruction instruction = instructionFunc.apply(ordinal);
			emit.instruction = instruction;
		};
//		final int index = instructions.size();
//		instructions.addSingle(null);
//		
//		return ordinal -> {
//			Instruction instruction = instructionFunc.apply(ordinal);
//			instructions.set(index, instruction);
//		};
	}
	
	private AllocationInfo getAllocationInfo(String id) {
		for(int i = 0; i < allocation.size(); i++) {
			if(allocation.get(i).id.equals(id))
				return allocation.get(i);
		}
		
		return null;
	}

	public boolean isDeclared(String id) {
		AllocationInfo allocationInfo = getAllocationInfo(id);
		
		if(allocationInfo != null)
			return true;
		
		if(outer != null)
			return outer.isDeclared(id);
		
		return false;
	}

	public int size() {
		return allocation.size() + 
			childrenStart.stream().mapToInt(a -> a.size()).sum() +
			childrenEnd.stream().mapToInt(a -> a.size()).sum();
	}

	public int sizeExceptEnd() {
		return allocation.size() + 
			childrenStart.stream().mapToInt(a -> a.sizeExceptEnd()).sum();
	}
	
	public void getLocalParameterOffset(PendingCodeEmitter instructions, Function<Integer, Instruction> instructionFunc) {
		getLocalParameterOffset(instructionsConsumer(instructions, instructionFunc));
	}

	public void getLocalParameterOffset(Consumer<Integer> handler) {
		offsetHandlers.add(handler);
	}

	public boolean isDeclaredLocally(String id) {
		return getAllocationInfo(id) != null;
	}
	
	public void generate() {
		generate(0);
	}
	
	public void generate(int offset) {
		for(int i = 0; i < allocation.size(); i++) {
			for(Consumer<Integer> handler: allocation.get(i).ordinalHandlers) {
				handler.accept(offset + i);
			}
		}

		int childStartOffset = offset + allocation.size();
		for(int i = 0; i < childrenStart.size(); i++) {
			childrenStart.get(i).generate(childStartOffset);
			childStartOffset += childrenStart.get(i).size();
		}

		int childEndOffset = offset + allocation.size();
		for(int i = 0; i < childrenEnd.size(); i++) {
			childrenEnd.get(i).generate(childEndOffset);
			childEndOffset += childrenEnd.get(i).size();
		}
		
		for(Consumer<Integer> handler: offsetHandlers)
			handler.accept(offset);
	}
}
