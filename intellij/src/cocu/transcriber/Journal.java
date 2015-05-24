package cocu.transcriber;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Journal<C> {
	private static class Entry<C> implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// Probably, there should be some sort of identifier to identify the respective reference
		// to execute commands on.
		// For now, commands are simply executed on root
		
		public List<C> replayableCommands;

		public Entry(List<C> replayableCommands) {
			this.replayableCommands = replayableCommands;
		}
	}
	
	private static final String journalFileName = "log";
	private static final String journalFile = journalFileName + ".jnl";
	
	private String journalPath;
	private ExecutorService journalLogger;

	public Journal(String journalPath) throws IOException, ClassNotFoundException {
		this.journalPath = journalPath;
		journalLogger = Executors.newSingleThreadExecutor();
	}
	
	private static class JournalInfo<C> {
		private final ArrayList<Entry<C>> transactions;
		
		public JournalInfo(ArrayList<Entry<C>> transactions) {
			this.transactions = transactions;
		}
	}
	
	public static <C> Journal<C> read(String journalPath) throws ClassNotFoundException, IOException {
		Journal<C> journal = new Journal<C>(journalPath);
		
		if(!java.nio.file.Files.exists(Paths.get(journalPath)))
			java.nio.file.Files.createDirectory(Paths.get(journalPath));
		
		if(!java.nio.file.Files.exists(Paths.get(journalPath + "/" + journalFile)))
			java.nio.file.Files.createFile(Paths.get(journalPath + "/" + journalFile));
		
		return journal;
	}
	
	public List<C> getCommands() throws ClassNotFoundException, IOException {
		JournalInfo<C> journalInfo = readJournal(journalPath + "/" + journalFile);
		
		ArrayList<C> commands = new ArrayList<C>();
		
		for(Entry<C> transaction: journalInfo.transactions)
			commands.addAll(transaction.replayableCommands);
		
		return commands;
	}

	@SuppressWarnings("unchecked")
	private static <C> JournalInfo<C> readJournal(String journalPath) throws ClassNotFoundException, IOException {
		ArrayList<Entry<C>> transactions = new ArrayList<Entry<C>>();
		
		FileInputStream fileOutput = new FileInputStream(journalPath);
		BufferedInputStream bufferedOutput = new BufferedInputStream(fileOutput);
		
		try {
			while(bufferedOutput.available() != 0) {
				// Should be read in chunks
				ObjectInputStream objectInput = new ObjectInputStream(bufferedOutput);
				Entry<C> transaction = (Entry<C>)objectInput.readObject();
					
				transactions.add(transaction);
			}
		} finally {
			bufferedOutput.close();
		}
		
		return new JournalInfo<C>(transactions);
	}
	
	public void log(/*Here should be the identifier/reference?, */ final List<C> commands) {
		journalLogger.execute(new Runnable() {
			@Override
			public void run() {
				try {
					FileOutputStream fileOutput = new FileOutputStream(journalPath + "/" + journalFile, true);
					BufferedOutputStream bufferedOutput = new BufferedOutputStream(fileOutput);
					ObjectOutputStream objectOutput = new ObjectOutputStream(bufferedOutput);
					
					objectOutput.writeObject(new Entry<C>(commands));
					
					objectOutput.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

	public void close() {
		journalLogger.shutdown();
	}
}
