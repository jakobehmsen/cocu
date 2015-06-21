package cocu;

import cocu.lang.Parser;
import cocu.lang.ast.AST;
import cocu.runtime.*;

import java.util.*;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        Parser parser = new Parser();
        String src = Arrays.asList(
            "try'\n" +
            "    signal: \"stuff\"\n" +
            "Catch' |aCtx aSignal|\n" +
            "    (resume: aCtx With: \"Resumed with some value\")"

            /*"var val = try'\n" +
            "    signal: \"stuff\"\n" +
            "Catch' |aCtx aSignal|\n" +
            "    (resume: aCtx With: \"Resumed with some value\")" +
            "val"*/

            /*"try'\n" +
            "    signal: \"stuff\"\n" +
            "Catch' |aCtx aSignal|\n" +
            "    \"Direct return of some value\""*/

            /*"var val = try'\n" +
            "    signal: \"stuff\"\n" +
            "Catch' |aCtx aSignal|\n" +
            "    \"Direct return of some value\"" +
            "val"*/

            //"signal: \"An error\""

            /*"match: 100",
            "    Case: 2 Then' 1",
            "    Case: \"aMsg2\" Then' 2",
            "    Case: 100 Then' 200"*/

            /*"var myObject = #{",
            "    var envelope = receive",
            "    reply: envelope, \"A reply\"",
            "    envelope = receive",
            "    reply: envelope, \"Another reply\"",
            "}",
            "var firstReply = myObject.msg",
            "var secondReply = myObject.msg"*/
        ).stream().collect(Collectors.joining("\n"));

        System.out.println("Source:");
        System.out.println(src);

        AST ast = parser.parse(src);

        Spawned root = new Spawned();
        root.frame = new EvalFrame(null,
            value -> System.out.println("Result: " + value),
            new SignalHandler(null, (context, signal) -> System.out.println("Signal: " + signal))
        );

        root.yielder = () -> {
        };

        Evaluator evaluator = new Evaluator(root);

        ast.accept(evaluator);
    }
}
