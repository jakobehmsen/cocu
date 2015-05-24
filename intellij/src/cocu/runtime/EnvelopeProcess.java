package cocu.runtime;

public class EnvelopeProcess extends LazyCloneProcess {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public Process receiver;
	public Processor.Frame sender;
	public Process message;

	public EnvelopeProcess(LocalizableProcess prototype, Process receiver, Processor.Frame sender, Process message) {
		super(prototype);
		this.receiver = receiver;
		this.sender = sender;
		this.message = message;
	}
}
