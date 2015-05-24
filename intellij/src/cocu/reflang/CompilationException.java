package cocu.reflang;

public class CompilationException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private MessageCollector errors;

	public CompilationException(MessageCollector errors) {
		this.errors = errors;
	}
	
	public MessageCollector getErrors() {
		return errors;
	}
	
	@Override
	public String getMessage() {
		StringBuilder errorsBuilder = new StringBuilder();
		errors.printMessages(msg -> errorsBuilder.append(msg + "\n"));
		return errorsBuilder.toString();
	}
}
