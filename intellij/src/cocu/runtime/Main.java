package cocu.runtime;

import java.io.File;
import java.io.IOException;
import java.util.List;

import cocu.debugging.Debug;
import cocu.reflang.CompilationException;
import cocu.reflang.Compiler;
import cocu.reflang.SymbolTable;
import cocu.transcriber.Journal;

public class Main {
	public static void main(String[] args) {
		if(args.length < 1) {
			System.out.println("Please supply path of object code.");
			return;
		}

//		long startWarmup = System.currentTimeMillis();
//		Compiler.warmup();
//		long endWarmup = System.currentTimeMillis();
//		Debug.println(Debug.LEVEL_MEDIUM, "Warm up time: " + (endWarmup - startWarmup));
	
		String mainObjectSourcePath = args[0];
			
		if(!mainObjectSourcePath.endsWith(".drs"))
			mainObjectSourcePath += ".drs";
		String mainObject = mainObjectSourcePath.substring(0, mainObjectSourcePath.length() - 4);
		String mainObjectCodePath = mainObject + ".drr";
		String mainObjectCodeJournalPath = mainObject + ".jnl";
		
		String commonsPath; 
		if(args.length > 1)
			commonsPath = args[1];
		else
			commonsPath = "commons"; 
		
		try {
			Debug.println(Debug.LEVEL_MEDIUM, "Reading main process...");
			long startReadMainProcess = System.currentTimeMillis();
			
			Compiler compiler = new Compiler();
			
			FrameInfo mainProcess = compiler.load(mainObjectSourcePath, mainObjectCodePath);
			
			if(mainProcess != null) {
				SymbolTable symbolTable = SymbolTable.ROOT;
				Processor processor = new Processor(mainProcess.localCount, mainProcess.maxStackSize, mainProcess.instructions);
				processor.setup(symbolTable, commonsPath, new File(mainObjectSourcePath).getParentFile().getCanonicalPath());
	
				long endReadMainProcess = System.currentTimeMillis();
				Debug.println(Debug.LEVEL_MEDIUM, "Read main process.");
				Debug.println(Debug.LEVEL_MEDIUM, "Read main process time: " + (endReadMainProcess - startReadMainProcess));
	
				Debug.println(Debug.LEVEL_MEDIUM, "Reading interaction history...");
				long startReadInteractionHistory = System.currentTimeMillis();
				Journal<InteractionHistory.Interaction> journal = Journal.read(mainObjectCodeJournalPath);
				List<InteractionHistory.Interaction> commands = journal.getCommands();
				long endReadInteractionHistory = System.currentTimeMillis();
				
				InteractionHistory interactionHistory = new InteractionHistory(commands);
				Debug.println(Debug.LEVEL_MEDIUM, "Read interaction history.");
				Debug.println(Debug.LEVEL_MEDIUM, "Read interaction history time: " + (endReadInteractionHistory - startReadInteractionHistory));
	
				Debug.println(Debug.LEVEL_MEDIUM, "Running...");
				long startEvaluation = System.currentTimeMillis();
				processor.replay(interactionHistory);
				long endEvaluation = System.currentTimeMillis();
				Debug.println(Debug.LEVEL_MEDIUM, "Ran.");
				Debug.println(Debug.LEVEL_MEDIUM, "Ran time: " + (endEvaluation - startEvaluation));
				
				Debug.println(Debug.LEVEL_MEDIUM, "Logging interactions...");
				long startLoggingInteractions = System.currentTimeMillis();
				if(interactionHistory.hasNewInteractions())
					journal.log(interactionHistory.getNewInteractions());
				
				journal.close();
				long endLoggingInteractions = System.currentTimeMillis();
				Debug.println(Debug.LEVEL_MEDIUM, "Logged interactions.");
				Debug.println(Debug.LEVEL_MEDIUM, "Logged interactions time: " + (endLoggingInteractions - startLoggingInteractions));
			}
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		} catch (CompilationException e) {
			System.err.println("Compilation failed:");
			e.getErrors().printMessages();
		}
	}
}
