package cocu.runtime;

public class LocalDictionaryProcess extends DictionaryProcess {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@Override
	public LocalizableProcess getAsLocal() {
		return this;
	}
	
	@Override
	public DictionaryProcess newBase() {
		return new LocalDictionaryProcess();
	}
}
