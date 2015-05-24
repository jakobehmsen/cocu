package cocu.runtime;

public class BooleanProcess extends DictionaryProcess implements NativeInteroperable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public boolean value;

	public BooleanProcess(boolean value) {
		this.value = value;
	}
	
	@Override
	public String toString() {
		return Boolean.toString(value);
	}
	
	@Override
	public Object toSerializable() {
		return value;
	}

	@Override
	public Object getNativeObject() {
		return value;
	}
}
