package cocu.runtime;

public class BuiltinProcess extends Process {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Process prototype;

	public BuiltinProcess(Process prototype) {
		this.prototype = prototype;
	}

	@Override
	public Object getCallable(Processor processor, int selectorCode, int arity) {
		return prototype.getCallable(processor, selectorCode, arity);
	}

	@Override
	public Process lookup(int selectorCode) {
		return prototype.lookup(selectorCode);
	}

	@Override
	public String[] getNames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void define(int selectorCode, Process value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void defineProto(int selectorCode, Process value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isDefined(int code) {
		return prototype.isDefined(code);
	}
	
	@Override
	public Process getEnvironment() {
		return this;
	}
}
