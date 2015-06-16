package cocu.reflang;

import cocu.runtime.FrameInfo;

import java.io.*;

public class FileFrameLoader implements FrameLoader {
    private String currentPath;
    private String commonsPath;

    public FileFrameLoader(String currentPath, String commonsPath) {
        this.currentPath = currentPath;
        this.commonsPath = commonsPath;
    }

    @Override
    public FrameInfo load(String path, Compiler compiler) throws IOException, ClassNotFoundException, CompilationException {
        String sourcePath = getPathInCommons( path + ".drs");
        String codePath = getPathInCommons( path + ".drr");

        return compiler.loadFrame(sourcePath, codePath);
    }

    private String getPathInCommons(String path) {
        if(path.startsWith("/"))
            return currentPath + "/" + commonsPath + path;
        else
            return currentPath + "/" + commonsPath + "/" + path;
    }
}
