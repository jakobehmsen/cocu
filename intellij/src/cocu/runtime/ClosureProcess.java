package cocu.runtime;

public class ClosureProcess extends BuiltinProcess {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public final Processor.Frame frame;
	public final BehaviorProcess behavior;
	public final int argumentOffset;
	public final int parameterCount;

	public ClosureProcess(Process prototype, Processor.Frame frame, BehaviorProcess behavior, int argumentOffset, int parameterCount) {
		super(prototype);
		this.frame = frame;
		this.behavior = behavior;
		this.argumentOffset = argumentOffset;
		this.parameterCount = parameterCount;
	}
	
//	@Override
//	public DictionaryProcess newBase() {
//		return new ClosureProcess(frame, behavior, argumentOffset, parameterCount);
//	}
}
