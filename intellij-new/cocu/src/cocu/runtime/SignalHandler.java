package cocu.runtime;

import java.util.function.BiConsumer;

public class SignalHandler {
    public SendFrame handlerFrame;
    public BiConsumer<SendFrame, Object> handler;

    public SignalHandler(SendFrame handlerFrame, BiConsumer<SendFrame, Object> handler) {
        this.handlerFrame = handlerFrame;
        this.handler = handler;
    }
}
