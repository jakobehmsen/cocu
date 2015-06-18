package cocu;

import cocu.lang.Parser;
import cocu.lang.ast.AST;
import cocu.lang.ast.ASTVisitor;

import java.util.Hashtable;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

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
    }

    private static class Spawned {
        public Consumer<Object> responseHandler;
        public Runnable yielder;
    }

    private static class SendFrame {
        //public Consumer<Object> responseHandler;
        public Spawned receiver;
        public SendFrame sender;

        public SendFrame(Consumer<Object> responseHandler, SendFrame sender) {
            this.receiver = new Spawned();
            this.receiver.responseHandler = responseHandler;
            this.sender = sender;
        }

        public SendFrame(Spawned receiver, SendFrame sender) {
            this.receiver = receiver;
            this.sender = sender;
        }
    }

    public static void main(String[] args) {
        Parser parser = new Parser();
        AST ast = parser.parse(
            //"quote: 123"
            "var myObject = #{ receive }"
        );

        Hashtable<String, Function<List<AST>, SpecialAST>> macros = new Hashtable<>();

        macros.put("quote", asts -> new SpecialAST() {
            @Override
            public <T> T acceptSpecial(SpecialASTVisitor<? extends T> visitor) {
                return visitor.visitQuote(asts.get(0));
            }
        });

        macros.put("receive", asts -> new SpecialAST() {
            @Override
            public <T> T acceptSpecial(SpecialASTVisitor<? extends T> visitor) {
                return visitor.visitReceive();
            }
        });

        ast.accept(new SpecialASTVisitor<Object>() {
            SendFrame sendFrame = new SendFrame(
                value -> {
                    System.out.println("Result:" + value);
                },
                null
            );

            /*Consumer<Object> responseHandler = value -> {
                System.out.println("Result:" + value);
            };*/

            private void pushFrame(Consumer<Object> newResponseHandler) {
                SendFrame sendFrame = this.sendFrame;

                Consumer<Object> outerResponseHandler = sendFrame.receiver.responseHandler;
                sendFrame.receiver.responseHandler = o -> {
                    sendFrame.receiver.responseHandler = outerResponseHandler;
                    newResponseHandler.accept(o);
                };
            }

            private void popFrame(Object result) {
                sendFrame.receiver.responseHandler.accept(result);
            }

            private void send(Object message, Spawned receiver) {
                SendFrame sender = this.sendFrame;

                sendFrame = new SendFrame(
                    receiver,
                    sender
                );
            }

            private void respond(Object result) {
                SendFrame sender = this.sendFrame;
                this.sendFrame = sender;
                this.sendFrame.receiver.responseHandler.accept(result);
            }

            private Hashtable<String, Object> variables = new Hashtable<>();

            @Override
            public Object visitVariableDefinition(boolean isDeclaration, String id, AST value) {
                if (value != null) {
                    pushFrame(result -> {
                        variables.put(id, result);
                        popFrame(result);
                    });

                    value.accept(this);
                }

                return null;
            }

            @Override
            public Object visitProgram(List<AST> expressions) {
                expressions.subList(0, expressions.size() - 1).forEach(e -> e.accept(this));

                expressions.get(expressions.size() - 1).accept(this);

                return null;
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
                Function<List<AST>, SpecialAST> macro = macros.get(selector);

                if (macro != null) {
                    SpecialAST replacement = macro.apply(args);

                    //pushFrame(result -> popFrame(result));

                    replacement.acceptSpecial(this);
                }

                // Invoke environment
                return null;
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
                /*for(int i = index; i < expressions.size(); i++) {
                    int startIndex = i;
                    pushFrame(result -> {
                        if(receiving) {
                            //pushFrame(message -> popFrame(message));
                            receiving = false;

                            evaluateExpressions(expressions, startIndex);
                        }
                    });
                    expressions.get(i).accept(this);
                }*/

                //int startIndex = i;
                pushFrame(result -> {
                    evaluateExpressions(expressions, index + 1);
                });
                expressions.get(index).accept(this);
            }

            // Specials
            @Override
            public Object visitQuote(AST ast) {
                popFrame(ast);

                return null;
            }

            //private boolean receiving;

            @Override
            public Object visitReceive() {
                sendFrame.receiver.yielder.run();
                //sendFrame = sendFrame.sender;
                //receiving = true;
                //popFrame(null);

                return null;
            }
        });
    }
}
