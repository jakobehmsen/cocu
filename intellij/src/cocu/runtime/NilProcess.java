package cocu.runtime;

import java.io.Serializable;

public class NilProcess extends DictionaryProcess implements NativeInteroperable {
	public static class Serialization implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public static final Serialization INSTANCE = new Serialization();
		
		private Serialization() { }
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public Object toSerializable() {
		return Serialization.INSTANCE;
	}

	@Override
	public Object getNativeObject() {
		return null;
	}
}
