package cocu.runtime;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.List;

import cocu.reflang.SymbolTable;

public class DictionaryProcess extends LocalizableProcess {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void resume(List<InteractionHistory.Interaction> playedInstructions) {
		// Does is make sense to have this method here?
	}
	
	private static class Member implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public final int code;
		public final Process value;
		
		public Member(int code, Process value) {
			this.code = code;
			this.value = value;
		}
		
		@Override
		public String toString() {
			return SymbolTable.ROOT.getIdFromSymbolCode(code) + ": " + value;
		}
	}

	private Hashtable<Integer, Process> protos = new Hashtable<Integer, Process>();
	private Hashtable<Integer, Member> properties = new Hashtable<Integer, Member>();
	
	@Override
	public Object getCallable(Processor processor, int selectorCode, int arity) {
		Member callableMember = properties.get(selectorCode);
		
		if(callableMember != null)
			return callableMember.value;
		
		for(Process proto: protos.values()) {
			Object callable = proto.getCallable(processor, selectorCode, arity);
			if(callable != null)
				return callable;
		}
		
		return null;
	}

	@Override
	public Process lookup(int selectorCode) {
		Member valueMember = properties.get(selectorCode);
		
		if(valueMember != null)
			return valueMember.value;
		
		for(Process proto: protos.values()) {
			Process value = proto.lookup(selectorCode);
			if(value != null)
				return value;
		}
		
		return null;
		
	}
	
	@Override
	public boolean isDefined(int code) {
		return properties.containsKey(code);
	}
	
	@Override
	public String[] getNames() {
		return properties.keySet().toArray(new String[properties.size()]);
	}

	@Override
	public void defineProto(int selectorCode, Process value) {
		properties.put(selectorCode, new Member(selectorCode, value));
		protos.put(selectorCode, value);
	}
	
	@Override
	public void define(int selectorCode, Process value) {
		properties.put(selectorCode, new Member(selectorCode, value));
		protos.remove(selectorCode);
	}
	
	@Override
	public String toString() {
		return "<dict>";
	}
	
	public DictionaryProcess derive() {
		DictionaryProcess derivation = newBase();
		derivation.defineProto(SymbolTable.Codes.parent, this);
		return derivation;
	}
	
	public DictionaryProcess newBase() {
		return new DictionaryProcess();
	}
	
	@Override
	public LocalizableProcess getAsLocal() {
		LocalDictionaryProcess local = new LocalDictionaryProcess();
		local.defineProto(SymbolTable.Codes.parent, this);
		return local;
	}
	
	@Override
	public Process getEnvironment() {
		return this;
	}
}
