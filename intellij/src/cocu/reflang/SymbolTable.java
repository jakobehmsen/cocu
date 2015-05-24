package cocu.reflang;

import java.lang.reflect.Field;
import java.util.Hashtable;

import cocu.runtime.Selector;

public class SymbolTable {
	public static final class Codes {
		public static final int parent = 0;
		public static final int Any = 1;
		public static final int Null = 2;
		public static final int True = 3;
		public static final int prototype = 4;
		public static final int False = 5;
		public static final int Array = 6;
		public static final int String = 7;
		public static final int Integer = 8;
		public static final int Behavior = 9;
		public static final int Closure = 10;
		public static final int Frame = 11;
		public static final int call = 12;
		public static final int call_1 = 13;
		public static final int call_2 = 14;
		public static final int call_3 = 15;
		public static final int Handler = 16;
		public static final int id = 17;
		public static final int arguments = 18;
		public static final int message = 19;
		public static final int sender = 20;
		public static final int Envelope = 21;
	}
	
	public static final SymbolTable ROOT = createRoot();
	
	
	private static SymbolTable createRoot() {
		SymbolTable symbolTable = new SymbolTable();

		try {
			for(Field codeField: Codes.class.getFields()) {
				String selector = codeField.getName();
				String[] selectorSplit = selector.split("_");
				String id = selectorSplit[0];
				int arity = selectorSplit.length == 2 ? Integer.parseInt(selectorSplit[1]) : 0;
				int symbolCode = codeField.getInt(null);

				symbolTable.add(Selector.get(id, arity), symbolCode);
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		
		return symbolTable;
	}
	
	private Hashtable<Selector, Integer> idToCodeMap = new Hashtable<Selector, Integer>();
	private Hashtable<Integer, Selector> codeToIdMap = new Hashtable<Integer, Selector>();
	
	private SymbolTable() { }
	
	private void add(Selector id, int symbolCode) {
		idToCodeMap.put(id, symbolCode);
		codeToIdMap.put(symbolCode, id);
	}
	
	public Selector getIdFromSymbolCode(int symbolCode) {
		return codeToIdMap.get(symbolCode);
	}
	
	public int getSymbolCodeFromId(Selector id) {
		Integer symbolCode = idToCodeMap.get(id);
		
		if(symbolCode == null) {
			symbolCode = idToCodeMap.size();
			idToCodeMap.put(id, symbolCode);
			codeToIdMap.put(symbolCode, id);
		}
		
		return symbolCode;
	}
}
