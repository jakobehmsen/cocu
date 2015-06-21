package cocu.runtime;

public class Envelope {
    public SendFrame sender;
    public String selector;
    public Object[] arguments;

    public Envelope(SendFrame sender, Object[] arguments, String selector) {
        this.sender = sender;
        this.arguments = arguments;
        this.selector = selector;
    }
}
