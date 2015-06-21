package cocu.runtime;

import java.util.Hashtable;

public class Environment {
    public Environment outer;
    private Hashtable<String, Object> variables = new Hashtable<>();

    public Environment(Environment outer) {
        this.outer = outer;
    }

    public Environment() { }

    public Object get(String name) {
        Object value = variables.get(name);

        if (value != null)
            return value;

        if (outer != null)
            return outer.get(name);

        return null;
    }

    public void declare(String name, Object value) {
        variables.put(name, value);
    }

    public void set(String name, Object value) {
        if (variables.containsKey(name))
            variables.put(name, value);
        else {
            if (outer != null)
                outer.set(name, value);
        }
    }
}
