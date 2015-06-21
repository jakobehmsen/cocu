package cocu.runtime;

public class SendFrame {
    public Spawned receiver;
    public SendFrame sender;

    public SendFrame(Spawned receiver, SendFrame sender) {
        this.receiver = receiver;
        this.sender = sender;
    }
}
