package cocu.runtime;

import java.io.Serializable;

public class Selector implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String id;
	private int parameterCount;
	
	private Selector(String id, int parameterCount) {
		this.id = id;
		this.parameterCount = parameterCount;
	}
	
	@Override
	public int hashCode() {
		return id.hashCode() * Integer.hashCode(parameterCount);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Selector) {
			Selector otherSelecter = (Selector)obj;
			return this.id.equals(otherSelecter.id) && this.parameterCount == otherSelecter.parameterCount;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return id + "\\" + parameterCount;
	}
	
	public static Selector get(String id, int parameterCount) {
		return new Selector(id, parameterCount);
	}
	
	public static Selector get(String id) {
		return get(id, 0);
	}

	public String getId() {
		return id;
	}

	public int getParameterCount() {
		return parameterCount;
	}
}
