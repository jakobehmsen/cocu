package cocu.runtime;

import cocu.lang.ast.AST;
import cocu.lang.ast.ASTVisitor;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

public class Evaluator implements ASTVisitor<Object> {
    public SendFrame sendFrame;
    private ArrayList<Function<String, BiConsumer<Evaluator, List<AST>>>> allMacros;

    public Evaluator(Spawned root) {
        sendFrame = new SendFrame(
            root,
            null
        );

        allMacros = MacroUtil.createMacros();
    }

    public void pushFrame(Consumer<Object> newResponseHandler) {
        SendFrame sendFrame = this.sendFrame;

        sendFrame.receiver.frame = new EvalFrame(
            sendFrame.receiver.frame,
            newResponseHandler,
            sendFrame.receiver.frame.signalHandler
        );
    }

    public void popFrame(Object result) {
        Consumer<Object> responseHandler = sendFrame.receiver.frame.responseHandler;
        sendFrame.receiver.frame = sendFrame.receiver.frame.outer;
        responseHandler.accept(result);
    }

    @Override
    public Object visitVariableDefinition(boolean isDeclaration, String id, AST value) {
        if (value != null) {
            pushFrame(result -> {
                if (isDeclaration)
                    sendFrame.receiver.environment.declare(id, result);
                else
                    sendFrame.receiver.environment.set(id, result);
                popFrame(result);
            });

            value.accept(this);
        } else {
            if (isDeclaration)
                sendFrame.receiver.environment.declare(id, null);
        }

        return null;
    }

    @Override
    public Object visitVariableUsage(String id) {
        Object value = sendFrame.receiver.environment.get(id);

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
    public Object visitClosure(List<String> parameters, AST body) {
        popFrame(new Closure(parameters, body, sendFrame.receiver.environment));

        return null;
    }

    @Override
    public Object visitEnvironmentMessage(String selector, List<AST> args) {
        Optional<BiConsumer<Evaluator, List<AST>>> applicableMacro =
            allMacros.stream()
                .map(x -> x.apply(selector))
                .filter(x -> x != null)
                .findFirst();

        if (applicableMacro.isPresent()) {
            BiConsumer<Evaluator, List<AST>> macro = applicableMacro.get();
            macro.accept(this, args);
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

    @Override
    public Object visitSpawn(AST environment, List<AST> expressions) {
        Spawned receiver = new Spawned();

        SendFrame outerSendFrame = sendFrame;

        receiver.yielder = () -> {
            sendFrame = outerSendFrame;
            popFrame(receiver);
        };
        // Forward signal handler
        receiver.frame = new EvalFrame(null, null, outerSendFrame.receiver.frame.signalHandler);

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

    @Override
    public Object visitGroup(List<AST> expressions) {
        evaluateExpressionsReturnLast(expressions, 0);

        return null;
    }

    // Specials helpers
    public Object quote(AST ast) {
        popFrame(ast);

        return null;
    }

    public Object receive() {
        sendFrame.receiver.yielder.run();

        return null;
    }

    public Object reply(AST envelope, AST value) {
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

    public Object match(AST target, Map<Object, AST> table) {
        pushFrame(key -> {
            AST then = table.get(key);
            then.accept(this);
        });
        target.accept(this);

        return null;
    }

    public Object signal(AST astSignal) {
        pushFrame(signal -> {
            SendFrame signalFrame = sendFrame;
            sendFrame = sendFrame.receiver.frame.signalHandler.handlerFrame;
            signalFrame.receiver.frame.signalHandler.handler.accept(signalFrame, signal);
        });
        astSignal.accept(this);

        return null;
    }

    public Object resumeWith(AST astContext, AST astValue) {
        pushFrame(context -> {
            pushFrame(value -> {
                sendFrame = (SendFrame) context;

                popFrame(value);
            });
            astValue.accept(this);
        });
        astContext.accept(this);

        return null;
    }

    public void apply(List<Object> arguments, Closure closure) {
        // Allocate inner environment bound to current environment
        Environment applicationEnvironment = new Environment(closure.environment);
        pushFrame(result -> {
            sendFrame.receiver.environment = applicationEnvironment.outer;
            popFrame(result);
        });

        // Bind arguments to local environment
        IntStream.range(0, closure.parameters.size()).forEach(x -> {
            String name = closure.parameters.get(x);
            Object argument = arguments.get(x);
            applicationEnvironment.declare(name, argument);
        });
        sendFrame.receiver.environment = applicationEnvironment;
        closure.body.accept(this);
    }

    public Object tryCatch(AST astBody, Function<Environment, Closure> closureConstructor) {
        Closure closure = closureConstructor.apply(sendFrame.receiver.environment);

        EvalFrame signalHandlerFrame = sendFrame.receiver.frame;
        BiConsumer<SendFrame, Object> signalHandler = (context, signal) -> {
            // Reset to to signal handler frame
            // - otherwise, responseHandler can be invoked twice
            sendFrame.receiver.frame = signalHandlerFrame;
            apply(Arrays.asList(context, signal), closure);
        };

        sendFrame.receiver.frame =
            new EvalFrame(sendFrame.receiver.frame, sendFrame.receiver.frame.responseHandler, new SignalHandler(sendFrame, signalHandler));
        astBody.accept(this);

        return null;
    }
}
