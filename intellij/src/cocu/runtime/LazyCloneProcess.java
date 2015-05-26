package cocu.runtime;

public class LazyCloneProcess extends Process {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private LocalizableProcess protoOrInstance;

	public LazyCloneProcess(LocalizableProcess protoOrInstance) {
		this.protoOrInstance = protoOrInstance;
	}

	@Override
	public Object getCallable(Processor processor, int selectorCode, int arity) {
		return protoOrInstance.getCallable(processor, selectorCode, arity);
	}

	@Override
	public Process lookup(int selectorCode) {
		return protoOrInstance.lookup(selectorCode);
	}

	@Override
	public boolean isDefined(int code) {
		return protoOrInstance.isDefined(code);
	}

	@Override
	public String[] getNames() {
		return protoOrInstance.getNames();
	}

	@Override
	public void define(int selectorCode, Process value) {
		protoOrInstance = protoOrInstance.getAsLocal();
		protoOrInstance.define(selectorCode, value);
	}

	@Override
	public void defineProto(int selectorCode, Process value) {
		protoOrInstance = protoOrInstance.getAsLocal();
		protoOrInstance.define(selectorCode, value);
	}
	
	@Override
	public Process getEnvironment() {
		return this;
	}
}
