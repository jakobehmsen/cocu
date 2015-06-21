package cocu.runtime;

import cocu.lang.ast.AST;

import java.util.List;

public class Closure {
    public List<String> parameters;
    public AST body;
    public Environment environment;

    public Closure(List<String> parameters, AST body, Environment environment) {
        this.parameters = parameters;
        this.body = body;
        this.environment = environment;
    }
}
