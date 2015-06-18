package cocu.lang;

import cocu.lang.antlr4.CocuBaseVisitor;
import cocu.lang.antlr4.CocuLexer;
import cocu.lang.antlr4.CocuParser;
import cocu.lang.ast.AST;
import cocu.lang.ast.ASTVisitor;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Parser {
    public AST parse(String input) {
        try {
            return parse(new ByteArrayInputStream(input.getBytes()));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public AST parse(InputStream input) throws IOException {
        CharStream charStream = new ANTLRInputStream(input);
        CocuLexer lexer = new CocuLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        CocuParser parser = new CocuParser(tokenStream);

        CocuParser.ProgramContext programCtx = parser.program();

        return antlr4ToAst(programCtx);
    }

    private AST antlr4ToAst(ParserRuleContext ctx) {
        return ctx.accept(new CocuBaseVisitor<AST>() {
            private HashSet<String> declaredVariables = new HashSet<String>();

            @Override
            public AST visitProgram(@NotNull CocuParser.ProgramContext ctx) {
                List<AST> expressions = ctx.expression().stream().map(x -> x.accept(this)).collect(Collectors.toList());

                return new AST() {
                    @Override
                    public <T> T accept(ASTVisitor<? extends T> visitor) {
                        return visitor.visitProgram(expressions);
                    }
                };
            }

            @Override
            public AST visitExpression(@NotNull CocuParser.ExpressionContext ctx) {
                AST receiver = ctx.expressionReceiver().accept(this);

                // Process chain

                return receiver;
            }

            @Override
            public AST visitMessageExchange(@NotNull CocuParser.MessageExchangeContext ctx) {
                AST receiver = ctx.receiver().accept(this);

                ArrayList<ParserRuleContext> chain = new ArrayList<>();
                ctx.messageChain().forEach(x -> chain.add(x));
                if(ctx.messageEnd() != null)
                    chain.add(ctx.messageEnd());

                // Process chain
                return visitMessageChain(receiver, chain);

                //return receiver;
            }

            private AST visitMessageChain(AST receiver, List<ParserRuleContext> chain) {
                for (ParserRuleContext msgCtx : chain) {
                    AST messageReceiver = receiver;
                    receiver = createMessageSend(msgCtx, (selector, args) -> new AST() {
                        @Override
                        public <T> T accept(ASTVisitor<? extends T> visitor) {
                            return visitor.visitMessageSend(messageReceiver, selector, args);
                        }
                    });
                }

                return receiver;
            }

            private <T> T createMessageSend(ParserRuleContext messageCtx, BiFunction<String, List<AST>, T> messageCreator) {
                return messageCtx.accept(new CocuBaseVisitor<T>() {
                    @Override
                    public T visitUnaryMessage(@NotNull CocuParser.UnaryMessageContext ctx) {
                        String selector = ctx.getText();

                        return messageCreator.apply(selector, Collections.emptyList());
                    }
                });
            }

            @Override
            public AST visitSelfMultiKeyMessage(@NotNull CocuParser.SelfMultiKeyMessageContext ctx) {
                String selector =
                    Stream.concat(
                        Arrays.asList(ctx.multiKeyMessage().multiKeyMessageHead().ID_UNCAP().getText()).stream(),
                        ctx.multiKeyMessage().multiKeyMessageTail().stream().map(x -> x.ID_CAP().getText())
                    ).collect(Collectors.joining());

                List<AST> args =
                    Stream.concat(
                        getMultiMessageArgCtxs(ctx.multiKeyMessage().multiKeyMessageHead().multiKeyMessageArgs()),
                        ctx.multiKeyMessage().multiKeyMessageTail().stream().flatMap(x -> getMultiMessageArgCtxs(x.multiKeyMessageArgs()))
                    ).collect(Collectors.toList());

                return new AST() {
                    @Override
                    public <T> T accept(ASTVisitor<? extends T> visitor) {
                        return visitor.visitEnvironmentMessage(selector, args);
                    }
                };
            }

            private Stream<AST> getMultiMessageArgCtxs(CocuParser.MultiKeyMessageArgsContext ctx) {
                return ctx.multiKeyMessageArg().stream().map(x -> getMultiMessageArgCtx(x));
            }

            private AST getMultiMessageArgCtx(CocuParser.MultiKeyMessageArgContext ctx) {
                if (ctx.selfSingleKeyMessage() != null)
                    return ctx.selfSingleKeyMessage().accept(this);
                else {
                    AST receiver = ctx.multiKeyMessageArgReceiver().accept(this);

                    // Process args

                    return receiver;
                }
            }

            @Override
            public AST visitAssignment(@NotNull CocuParser.AssignmentContext ctx) {
                String id = ctx.id().getText();
                AST value = ctx.expression().accept(this);

                return new AST() {
                    @Override
                    public <T> T accept(ASTVisitor<? extends T> visitor) {
                        return visitor.visitVariableDefinition(false, id, value);
                    }
                };
            }

            @Override
            public AST visitAccess(@NotNull CocuParser.AccessContext ctx) {
                String selector = ctx.getText();

                if (declaredVariables.contains(selector)) {
                    return new AST() {
                        @Override
                        public <T> T accept(ASTVisitor<? extends T> visitor) {
                            return visitor.visitVariableUsage(selector);
                        }
                    };
                } else
                    return new AST() {
                        @Override
                        public <T> T accept(ASTVisitor<? extends T> visitor) {
                            return visitor.visitEnvironmentMessage(selector, Collections.emptyList());
                        }
                    };
            }

            @Override
            public AST visitVariableDeclaration(@NotNull CocuParser.VariableDeclarationContext ctx) {
                String id = ctx.id().getText();

                declaredVariables.add(id);

                AST value = ctx.expression() != null ? ctx.expression().accept(this) : null;

                return new AST() {
                    @Override
                    public <T> T accept(ASTVisitor<? extends T> visitor) {
                        return visitor.visitVariableDefinition(true, id, value);
                    }
                };
            }

            @Override
            public AST visitInteger(@NotNull CocuParser.IntegerContext ctx) {
                int value = Integer.parseInt(ctx.getText());

                return new AST() {
                    @Override
                    public <T> T accept(ASTVisitor<? extends T> visitor) {
                        return visitor.visitInteger(value);
                    }
                };
            }

            @Override
            public AST visitString(@NotNull CocuParser.StringContext ctx) {
                String rawString = ctx.getText();
                String value = rawString.substring(1, rawString.length() - 1)
                    .replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t");

                return new AST() {
                    @Override
                    public <T> T accept(ASTVisitor<? extends T> visitor) {
                        return visitor.visitString(value);
                    }
                };
            }

            @Override
            public AST visitSpawn(@NotNull CocuParser.SpawnContext ctx) {
                AST environment = ctx.explicitPrototype != null ? ctx.explicitPrototype.accept(this) : null;
                List<AST> expressions = ctx.expression().stream().map(x -> x.accept(this)).collect(Collectors.toList());

                return new AST() {
                    @Override
                    public <T> T accept(ASTVisitor<? extends T> visitor) {
                        return visitor.visitSpawn(environment, expressions);
                    }
                };
            }
        });
    }
}
