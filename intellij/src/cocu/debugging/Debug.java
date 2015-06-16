package cocu.debugging;

import java.io.PrintStream;

public class Debug {
	public static final int LEVEL_LOW = 0;
	public static final int LEVEL_MEDIUM = 1;
	public static final int LEVEL_HIGH = 2;
	
//	public static final int maxLevel = LEVEL_LOW;
	public static final int maxLevel = LEVEL_MEDIUM;
//	public static final int maxLevel = LEVEL_HIGH;

	private static PrintStream printStream = System.out;

	public static void setPrintStream(PrintStream printStream) {
		Debug.printStream = printStream;
	}
	
	public static final void println(int level, String message) {
		if(level <= maxLevel) {
			printStream.println("DEBUG: " + message);
		}
	}
}
