package cocu.runtime;

public class FrameProcess extends LazyCloneProcess {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public Processor.Frame frame;

	public FrameProcess(LocalizableProcess prototype, Processor.Frame frame) {
		super(prototype);
		this.frame = frame;
	}
	
//	@Override
//	public DictionaryProcess newBase() {
//		return new FrameProcess(frame);
//	}
}
