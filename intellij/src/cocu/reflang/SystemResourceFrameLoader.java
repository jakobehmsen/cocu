package cocu.reflang;

import cocu.debugging.Debug;
import cocu.runtime.FrameInfo;

import java.io.*;

public class SystemResourceFrameLoader implements FrameLoader {
    private static final String commonsPath = "commons";

    @Override
    public FrameInfo load(String path, Compiler compiler) throws IOException, ClassNotFoundException, CompilationException {
        // Drr files are assumed to exist in the valid version in the jar file.
        String codePath = getPathInCommons( path + ".drr");

        Debug.println(Debug.LEVEL_HIGH, "codePath=" + codePath);

        InputStream codeInput = ClassLoader.getSystemResourceAsStream(codePath);

        FrameInfo process = null;

        try (ObjectInput oo = new ObjectInputStream(codeInput)) {
            oo.readLong(); // read and ignore LastModified
            process = (FrameInfo) oo.readObject();
        }

        return process;
    }

    private String getPathInCommons(String path) {
        if(path.startsWith("/"))
            return commonsPath + path;
        else
            return commonsPath + "/" + path;
    }
}
