package cocu.reflang;

import cocu.runtime.FrameInfo;

public class Compilation {
    public final FrameInfo frame;
    public final MessageCollector errors;

    public Compilation(FrameInfo frame, MessageCollector errors) {
        this.frame = frame;
        this.errors = errors;
    }

    public boolean hasErrors() {
        return errors.hasMessages();
    }

    public void printErrors() {
        errors.printMessages();
    }
}
