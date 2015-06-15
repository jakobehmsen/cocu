package cocu.runtime;

import java.io.Serializable;
import java.util.List;

public abstract class Process implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	//public abstract void resume(List<InteractionHistory.Interaction> playedInstructions);

	public abstract Object getCallable(Processor processor, int selectorCode, int arity);

	public abstract Process lookup(int selectorCode);

	public abstract boolean isDefined(int code);

	public abstract String[] getNames();

	public abstract void define(int selectorCode, Process value);

	public abstract void defineProto(int selectorCode, Process value);
	
	public abstract Process getEnvironment();
	
	public Object toSerializable() {
		throw new RuntimeException("Serialization unsupported.");
	}

	public void sentReply() { }
}
