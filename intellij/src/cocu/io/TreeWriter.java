package cocu.io;

import java.io.IOException;
import java.io.Writer;

public class TreeWriter {
	private Writer writer;
	private String indention = "";
	private boolean printIndention;

	public TreeWriter(Writer writer) {
		this.writer = writer;
	}
	
	private String indentionBlock() {
		return "    ";
	}
	
	private int indentionBlockLength() {
		return indentionBlock().length();
	}
	
	public int depth() {
		return indention.length() / indentionBlockLength();
	}
	
	public void write(Object obj) throws IOException {
		write(obj.toString());
	}
	
	public void write(String str) throws IOException {
		String[] lines = str.split("\\r?\\n");
		if(lines.length > 0) {
			append(lines[0]);
			for(int i = 1; i < lines.length; i++) {
				appendln();
				append(lines[i]);
			}
		}
	}
	
	public void writeln()  throws IOException {
		appendln();
	}
	
	private void append(String str) throws IOException {
		if(printIndention) {
			writer.write(indention);
			printIndention = false;
		}
		writer.write(str);
	}
	
	private void appendln() throws IOException {
		writer.write("\n");
		printIndention = true;
	}
	
	public void indentnl() throws IOException {
		appendln();
		indention = new String(indention + indentionBlock());
	}
	
	public void dedentnl() throws IOException {
		if(indention.length() == 0)
			throw new RuntimeException("Cannot dedent when indention depth is zero.");
		
		appendln();
		indention = indention.substring(0, indention.length() - indentionBlockLength());
	}
}
