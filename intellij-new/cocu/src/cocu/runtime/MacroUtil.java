package cocu.runtime;

import cocu.lang.ast.AST;
import cocu.lang.ast.ASTAdapter;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MacroUtil {
    private static List<String> splitByCamelCase(String str) {
        int[] capIndexes =
            IntStream.concat(
                IntStream.of(0),
                IntStream.concat(
                    IntStream.range(0, str.length()).filter(x -> Character.isUpperCase(str.charAt(x))),
                    IntStream.of(str.length())
                )
            ).toArray();

        return IntStream.range(0, capIndexes.length - 1).mapToObj(x -> {
            int start = capIndexes[x];
            int end = capIndexes[x + 1];
            return str.substring(start, end);
        }).collect(Collectors.toList());
    }

    public static ArrayList<Function<String, BiConsumer<Evaluator, List<AST>>>> createMacros() {
        ArrayList<Function<String, BiConsumer<Evaluator, List<AST>>>> allMacros = new ArrayList<>();

        Hashtable<String, BiConsumer<Evaluator, List<AST>>> indexedMacros = new Hashtable<>();

        allMacros.add(x -> {
            List<String> split = splitByCamelCase(x);
            if (split.get(0).equals("match")) {
                // Check whether only CaseThen pairs proceeds

                return (evaluator, asts) -> {
                    AST target = asts.get(0);

                    Hashtable<Object, AST> table = new Hashtable<>();

                    for (int i = 1; i < asts.size(); i += 2) {
                        Object key = asts.get(i).accept(new ASTAdapter<Object>() {
                            @Override
                            public Integer visitInteger(int value) {
                                return value;
                            }

                            @Override
                            public String visitString(String value) {
                                return value;
                            }
                        });
                        AST then = asts.get(i + 1).accept(new ASTAdapter<AST>() {
                            @Override
                            public AST visitClosure(List<String> parameters, AST body) {
                                return body;
                            }
                        });
                        table.put(key, then);
                    }

                    evaluator.match(target, table);
                };
            }

            return null;
        });

        allMacros.add(x -> indexedMacros.get(x));

        indexedMacros.put("quote", (evaluator, asts) -> evaluator.quote(asts.get(0)));

        indexedMacros.put("receive", (evaluator, asts) -> evaluator.receive());

        indexedMacros.put("reply", (evaluator, asts) -> {
            AST envelope = asts.get(0);
            AST value = asts.get(1);

            evaluator.reply(envelope, value);
        });

        indexedMacros.put("signal", (evaluator, asts) -> {
            AST astSignal = asts.get(0);

            evaluator.signal(astSignal);
        });

        indexedMacros.put("resumeWith", (evaluator, asts) -> {
            AST astContext = asts.get(0);
            AST astValue = asts.get(1);

            evaluator.resumeWith(astContext, astValue);
        });

        indexedMacros.put("tryCatch", (evaluator, asts) -> {
            AST tryBody = asts.get(0).accept(new ASTAdapter<AST>() {
                @Override
                public AST visitClosure(List<String> parameters, AST body) {
                    return body;
                }
            });

            Function<Environment, Closure> closureConstructor = asts.get(1).accept(new ASTAdapter<Function<Environment, Closure>>() {
                @Override
                public Function<Environment, Closure> visitClosure(List<String> parameters, AST body) {
                    return environment -> new Closure(parameters, body, environment);
                }
            });

            evaluator.tryCatch(tryBody, closureConstructor);
        });

        return allMacros;
    }
}
