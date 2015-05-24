package cocu.runtime;

import java.util.ArrayList;

import cocu.transcriber.Journal;

public class Runtime {
	private Journal<InteractionHistory.Interaction> journal;
	
	public Runtime(Journal<InteractionHistory.Interaction> journal) {
		this.journal = journal;
	}

	public void resume(ActiveProcess process) {
		// Start off where the process left of, which may be from its beginning
		
		ArrayList<InteractionHistory.Interaction> playedInstructions = new ArrayList<InteractionHistory.Interaction>();
		process.resume(playedInstructions);
		
		journal.log(playedInstructions);
	}
}
