package cocu.io;

import java.io.IOException;

public interface WritableTree {
	void writeTo(TreeWriter writer) throws IOException;
}
