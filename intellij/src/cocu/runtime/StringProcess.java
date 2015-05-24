package cocu.runtime;

public class StringProcess extends BuiltinProcess implements NativeInteroperable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public final String str;

	public StringProcess(Process prototype, String str) {
		super(prototype);
		this.str = str;
	}

	@Override
	public Object getNativeObject() {
		return str;
	}
	
	@Override
	public Object toSerializable() {
		return str;
	}
	
//	@Override
//	public DictionaryProcess newBase() {
//		return new StringProcess(str);
//	}
}
