package cocu.runtime;

import java.io.Serializable;

public class FrameInfo implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public int localCount; // receiver + arguments + variables
	public int maxStackSize;
	public Instruction[] instructions;
	
	public FrameInfo(int localCount, int maxStackSize, Instruction[] instructions) {
		this.localCount = localCount;
		this.maxStackSize = maxStackSize;
		this.instructions = instructions;
	}
}
