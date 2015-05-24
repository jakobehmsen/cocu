package cocu.runtime;

public class BehaviorProcess extends BuiltinProcess {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public FrameInfo frameInfo;
	
	public BehaviorProcess(Process prototype, FrameInfo frameInfo) {
		super(prototype);
		this.frameInfo = frameInfo;
	}

//	@Override
//	public DictionaryProcess newBase() {
//		return new BehaviorProcess(localCount, maxStackSize, instructions);
//	}
}
