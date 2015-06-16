package cocu.reflang;

import cocu.runtime.FrameInfo;

import java.io.IOException;

public interface FrameLoader {
    FrameInfo load(String path, Compiler compiler) throws IOException, ClassNotFoundException, CompilationException;
}
