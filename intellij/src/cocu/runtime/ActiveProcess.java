package cocu.runtime;

public class ActiveProcess extends Process {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Process environment;
	private Processor.Frame frame;

	public ActiveProcess(Process environment, Processor.Frame frame) {
		this.environment = environment;
		this.frame = frame;
	}
	
	@Override
	public Object getCallable(Processor processor, int selectorCode, int arity) {
		Process envelope = processor.getEnvelopeFromHere(this, selectorCode, arity);
		frame.push(envelope);
		frame.instructionPointer++;
		frame.instructions[frame.instructions.length - 1] = new Instruction(Instruction.OPCODE_FINISH);
		return frame;
	}

	@Override
	public void sentReply() {
		frame.instructions[frame.instructions.length - 1] = new Instruction(Instruction.OPCODE_HALT);
	}
	
	@Override
	public void define(int selectorCode, Process value) {

	}
	
	@Override
	public void defineProto(int selectorCode, Process value) {

	}

	@Override
	public Process lookup(int selectorCode) {
		return null;
	}

	@Override
	public String[] getNames() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public boolean isDefined(int code) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public Process getEnvironment() {
		return environment;
	}
}
