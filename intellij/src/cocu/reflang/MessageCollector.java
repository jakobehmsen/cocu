package cocu.reflang;

import java.util.ArrayList;
import java.util.function.Consumer;

import org.antlr.v4.runtime.ParserRuleContext;

public class MessageCollector {
	private static class Entry {
		public final int line;
		public final int charPositionInLine;
		public final String message;
		
		public Entry(int line, int charPositionInLine, String message) {
			this.line = line;
			this.charPositionInLine = charPositionInLine;
			this.message = message;
		}
	}
	
	private ArrayList<Entry> messages = new ArrayList<Entry>();
	
	public void appendMessage(ParserRuleContext ctx, String message) {
		messages.add(new Entry(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), message));
	}
	
	public void appendMessage(int line, int charPositionInLine, String message) {
		messages.add(new Entry(line, charPositionInLine, message));
	}
	
	public void appendMessages(MessageCollector collector) {
		messages.addAll(collector.messages);
	}
	
	public boolean hasMessages() {
		return messages.size() > 0;
	}
	
	public void printMessages() {
		printMessages(msg ->  System.err.println(msg));
	}
	
	public void printMessages(Consumer<String> messagePrinter) {
		messages.forEach(x -> messagePrinter.accept("@" + x.line + "," + (x.charPositionInLine + 1) + ": " + x.message));
	}
}
