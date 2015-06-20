package cocu;

import cocu.lang.Parser;
import cocu.lang.ast.AST;
import cocu.lang.ast.ASTVisitor;
import javafx.util.Pair;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Main {
    private interface SpecialAST extends AST {
        default <T> T accept(ASTVisitor<? extends T> visitor) {
            throw new UnsupportedOperationException();
        }

        <T> T acceptSpecial(SpecialASTVisitor<? extends T> visitor);
    }

    private interface SpecialASTVisitor<T> extends ASTVisitor<T> {
        T visitQuote(AST ast);
        T visitReceive();
        T visitReply(AST envelope, AST value);
        T visitMatch(AST target, Map<Object, AST> table);
        T visitSignal(AST astSignal);
        T visitResumeWith(AST astContext, AST astSignal);
    }

    private static class Spawned {
        //public Consumer<Object> responseHandler;
        public EvalFrame frame;
        public Runnable yielder;
        public Hashtable<String, Object> variables = new Hashtable<>();
    }

    private static class Envelope {
        public SendFrame sender;
        public String selector;
        public Object[] arguments;

        public Envelope(SendFrame sender, Object[] arguments, String selector) {
            this.sender = sender;
            this.arguments = arguments;
            this.selector = selector;
        }
    }

    private static class SendFrame {
        //public Consumer<Object> responseHandler;
        public Spawned receiver;
        public SendFrame sender;

        /*public SendFrame(Consumer<Object> responseHandler, SendFrame sender) {
            this.receiver = new Spawned();
            this.receiver.responseHandler = responseHandler;
            this.sender = sender;
        }*/

        public SendFrame(Spawned receiver, SendFrame sender) {
            this.receiver = receiver;
            this.sender = sender;
        }
    }

    private static class SignalHandler {
        public SendFrame handlerFrame;
        public BiConsumer<SendFrame, Object> handler;

        public SignalHandler(SendFrame handlerFrame, BiConsumer<SendFrame, Object> handler) {
            this.handlerFrame = handlerFrame;
            this.handler = handler;
        }

        //void handleSignal(EvalFrame context, Object signal);
    }

    private static class EvalFrame {
        public EvalFrame outer;

        public EvalFrame(EvalFrame outer, Consumer<Object> responseHandler, SignalHandler signalHandler) {
            this.outer = outer;
            this.responseHandler = responseHandler;
            this.signalHandler = signalHandler;
        }

        public Consumer<Object> responseHandler;
        public SignalHandler signalHandler;
    }

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

    public static void main(String[] args) {
        Parser parser = new Parser();
        String src = Arrays.asList(
            "signal: \"An error\""
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
        AST ast = parser.parse(src);

        System.out.println("Source:");
        System.out.println(src);

        ArrayList<Function<String, Function<List<AST>, SpecialAST>>> allMacros = new ArrayList<>();

        Hashtable<String, Function<List<AST>, SpecialAST>> indexedMacros = new Hashtable<>();

        allMacros.add(x -> {
            List<String> split = splitByCamelCase(x);
            if (split.get(0).equals("match")) {
                // Check whether only CaseThen pairs proceeds

                return asts -> {
                    AST target = asts.get(0);

                    Hashtable<Object, AST> table = new Hashtable<>();

                    for (int i = 1; i < asts.size(); i += 2) {
                        Object key = asts.get(i).accept(new ASTVisitor<Object>() {
                            @Override
                            public Object visitProgram(List<AST> expressions) {
                                return null;
                            }

                            @Override
                            public Integer visitInteger(int value) {
                                return value;
                            }

                            @Override
                            public String visitString(String value) {
                                return value;
                            }

                            @Override
                            public Object visitVariableDefinition(boolean isDeclaration, String id, AST value) {
                                return null;
                            }

                            @Override
                            public Object visitEnvironmentMessage(String selector, List<AST> args) {
                                return null;
                            }

                            @Override
                            public Object visitSpawn(AST environment, List<AST> expressions) {
                                return null;
                            }

                            @Override
                            public Object visitVariableUsage(String id) {
                                return null;
                            }

                            @Override
                            public Object visitMessageSend(AST receiver, String selector, List<AST> args) {
                                return null;
                            }
                        });
                        AST then = asts.get(i + 1);
                        table.put(key, then);
                    }

                    return new SpecialAST() {
                        @Override
                        public <T> T acceptSpecial(SpecialASTVisitor<? extends T> visitor) {
                            return visitor.visitMatch(target, table);
                        }
                    };
                };
            }

            return null;
        });

        allMacros.add(x -> indexedMacros.get(x));

        indexedMacros.put("quote", asts -> new SpecialAST() {
            @Override
            public <T> T acceptSpecial(SpecialASTVisitor<? extends T> visitor) {
                return visitor.visitQuote(asts.get(0));
            }
        });

        indexedMacros.put("receive", asts -> new SpecialAST() {
            @Override
            public <T> T acceptSpecial(SpecialASTVisitor<? extends T> visitor) {
                return visitor.visitReceive();
            }
        });

        indexedMacros.put("reply", asts -> new SpecialAST() {
            @Override
            public <T> T acceptSpecial(SpecialASTVisitor<? extends T> visitor) {
                return visitor.visitReply(asts.get(0), asts.get(1));
            }
        });

        indexedMacros.put("signal", asts -> new SpecialAST() {
            @Override
            public <T> T acceptSpecial(SpecialASTVisitor<? extends T> visitor) {
                return visitor.visitSignal(asts.get(0));
            }
        });

        indexedMacros.put("resumeWith", asts -> new SpecialAST() {
            @Override
            public <T> T acceptSpecial(SpecialASTVisitor<? extends T> visitor) {
                return visitor.visitResumeWith(asts.get(0), asts.get(1));
            }
        });

        Spawned root = new Spawned();
        root.frame = new EvalFrame(null,
            value -> System.out.println("Result: " + value),
            new SignalHandler(null, (context, signal) -> System.out.println("Signal: " + signal))
        );

        root.yielder = () -> {
        };

        ast.accept(new SpecialASTVisitor<Object>() {
            SendFrame sendFrame = new SendFrame(
                root,
                null
            );

            private void pushFrame(Consumer<Object> newResponseHandler) {
                SendFrame sendFrame = this.sendFrame;

                sendFrame.receiver.frame = new EvalFrame(
                    sendFrame.receiver.frame,
                    newResponseHandler,
                    sendFrame.receiver.frame.signalHandler
                );
            }

            private void popFrame(Object result) {
                Consumer<Object> responseHandler = sendFrame.receiver.frame.responseHandler;
                sendFrame.receiver.frame = sendFrame.receiver.frame.outer;
                responseHandler.accept(result);
            }

            @Override
            public Object visitVariableDefinition(boolean isDeclaration, String id, AST value) {
                if (value != null) {
                    pushFrame(result -> {
                        sendFrame.receiver.variables.put(id, result);
                        popFrame(result);
                    });

                    value.accept(this);
                }

                return null;
            }

            @Override
            public Object visitVariableUsage(String id) {
                Object value = sendFrame.receiver.variables.get(id);

                popFrame(value);

                return null;
            }

            @Override
            public Object visitProgram(List<AST> expressions) {
                evaluateExpressionsReturnLast(expressions, 0);

                return null;
            }

            private void evaluateExpressionsReturnLast(List<AST> expressions, int index) {
                if (index < expressions.size() - 1)
                    pushFrame(result ->
                        evaluateExpressionsReturnLast(expressions, index + 1));

                expressions.get(index).accept(this);
            }

            @Override
            public Object visitInteger(int value) {
                popFrame(value);

                return null;
            }

            @Override
            public Object visitString(String value) {
                popFrame(value);

                return null;
            }

            @Override
            public Object visitEnvironmentMessage(String selector, List<AST> args) {
                Optional<Function<List<AST>, SpecialAST>> applicableMacro =
                    allMacros.stream()
                        .map(x -> x.apply(selector))
                        .filter(x -> x != null)
                        .findFirst();

                if (applicableMacro.isPresent()) {
                    Function<List<AST>, SpecialAST> macro = applicableMacro.get();
                    SpecialAST replacement = macro.apply(args);

                    replacement.acceptSpecial(this);
                }

                // Invoke environment
                return null;
            }

            @Override
            public Object visitMessageSend(AST receiver, String selector, List<AST> args) {
                pushFrame(receiverValue -> {
                    visitMessageArgs(selector, (Spawned) receiverValue, new ArrayList<>(), args, 0);
                });
                receiver.accept(this);

                return null;
            }

            private void visitMessageArgs(String selector, Spawned receiverValue, List<Object> argValues, List<AST> args, int i) {
                if (i < args.size()) {
                    pushFrame(argValue -> {
                        argValues.add(argValue);
                        visitMessageArgs(selector, receiverValue, argValues, args, i + 1);
                    });
                    args.get(i).accept(this);
                } else {
                    send(selector, receiverValue);
                }
            }

            private void send(Object message, Spawned receiver) {
                SendFrame sender = this.sendFrame;

                // Setup yield for next receive
                receiver.yielder = () -> {
                    // If no reply is made, then control is never yielded back to outer frame
                    receiver.yielder = () -> {
                    };
                };

                sendFrame = new SendFrame(
                    receiver,
                    sender
                );

                popFrame(new Envelope(sender, null, (String) message));
            }

            private void signal(Object signal) {
                SendFrame signalFrame = sendFrame;
                sendFrame = sendFrame.receiver.frame.signalHandler.handlerFrame;
                signalFrame.receiver.frame.signalHandler.handler.accept(signalFrame, signal);
            }

            @Override
            public Object visitSpawn(AST environment, List<AST> expressions) {
                Spawned receiver = new Spawned();

                SendFrame outerSendFrame = sendFrame;

                receiver.yielder = () -> {
                    sendFrame = outerSendFrame;
                    popFrame(receiver);
                };

                sendFrame = new SendFrame(
                    receiver,
                    null
                );

                evaluateExpressions(expressions, 0);

                return null;
            }

            private void evaluateExpressions(List<AST> expressions, int index) {
                if (index < expressions.size()) {
                    pushFrame(result -> evaluateExpressions(expressions, index + 1));

                    expressions.get(index).accept(this);
                } else {
                    // Implicitly yield control
                    sendFrame.receiver.yielder.run();
                }
            }

            // Specials
            @Override
            public Object visitQuote(AST ast) {
                popFrame(ast);

                return null;
            }

            @Override
            public Object visitReceive() {
                sendFrame.receiver.yielder.run();

                return null;
            }

            @Override
            public Object visitReply(AST envelope, AST value) {
                pushFrame(envelopeValue -> {
                    pushFrame(valueValue -> {
                        sendFrame.receiver.yielder = () -> {
                            SendFrame sender = ((Envelope) envelopeValue).sender;
                            this.sendFrame = sender;
                            popFrame(valueValue);
                        };

                        popFrame(value);
                    });
                    value.accept(this);
                });
                envelope.accept(this);

                return null;
            }

            @Override
            public Object visitMatch(AST target, Map<Object, AST> table) {
                pushFrame(key -> {
                    AST then = table.get(key);
                    then.accept(this);
                });
                target.accept(this);

                return null;
            }

            @Override
            public Object visitSignal(AST astSignal) {
                pushFrame(signal -> {
                    signal(signal);
                });
                astSignal.accept(this);

                return null;
            }

            @Override
            public Object visitResumeWith(AST astContext, AST astValue) {
                pushFrame(context -> {
                    pushFrame(value -> {
                        sendFrame = (SendFrame)context;

                        popFrame(value);
                    });
                    astValue.accept(this);
                });
                astContext.accept(this);

                return null;
            }
        });
    }
}
