package cocu.runtime;

import java.util.function.Consumer;

public class EvalFrame {
    public EvalFrame outer;

    public EvalFrame(EvalFrame outer, Consumer<Object> responseHandler, SignalHandler signalHandler) {
        this.outer = outer;
        this.responseHandler = responseHandler;
        this.signalHandler = signalHandler;
    }

    public Consumer<Object> responseHandler;
    public SignalHandler signalHandler;
}
