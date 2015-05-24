package cocu.reflang;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import cocu.runtime.Instruction;

public class PendingCodeEmitter {
	private ArrayList<Consumer<CodeEmitter>> pendingEmits = new ArrayList<Consumer<CodeEmitter>>();

	public void add(Consumer<List<Instruction>> emit) {
		pendingEmits.add(emitter -> emitter.add(emit));
	}

	public void add(CodeEmit pendingEmit) {
		pendingEmits.add(emitter -> emitter.add(pendingEmit));
	}
	
	public void addSingle(Instruction instruction) {
		pendingEmits.add(emitter -> emitter.addSingle(instruction));
	}
	
	public CodeEmission generate() {
		CodeEmitter emitter = new CodeEmitter();
		for(Consumer<CodeEmitter> pendingEmit: pendingEmits)
			pendingEmit.accept(emitter);
		return emitter.generate();
	}

	public void label(Label label) {
		pendingEmits.add(emitter -> emitter.label(label));
	}

	public void jump(Function<Integer, Instruction> jumpToInstruction, Label label) {
		pendingEmits.add(emitter -> emitter.jump(jumpToInstruction, label));
	}
}
