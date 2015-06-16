package cocu.runtime.repl;

import cocu.debugging.Debug;
import cocu.reflang.*;
import cocu.runtime.*;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class Main {
    private static int startIndex = 0;

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setTitle("Cocu");

        JTextPane pendingScript = new JTextPane();

        Color bgColor = Color.BLACK;
        Color fgColor = Color.WHITE;

        String shellPrefix = "> ";

        pendingScript.setDocument(new DefaultStyledDocument() {
            @Override
            public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
                if (offs >= startIndex)
                    super.insertString(offs, str, a);
            }

            @Override
            public void remove(int offs, int len) throws BadLocationException {
                if (offs >= startIndex)
                    super.remove(offs, len);
            }
        });

        try {
            pendingScript.getDocument().insertString(0, shellPrefix, null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        startIndex = pendingScript.getDocument().getLength();
        pendingScript.setCaretPosition(startIndex);

        pendingScript.setBackground(bgColor);
        pendingScript.setForeground(fgColor);
        pendingScript.setCaretColor(fgColor);
        pendingScript.setSelectionColor(fgColor);

        pendingScript.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));

        SymbolTable symbolTable = SymbolTable.ROOT;

        cocu.reflang.Compiler compiler = new cocu.reflang.Compiler();
        FrameLoader frameLoader;
        String commonsPath = "commons";
        String currentDir = new File("").getAbsolutePath();

        if(!new File(currentDir + "/" + commonsPath).exists()) {
            // Try load commons from loaded jar
            System.err.println("Attempting to load commons relative to jar...");
            frameLoader = new SystemResourceFrameLoader();
        } else {
            frameLoader = new FileFrameLoader(currentDir, commonsPath);
        }

        Processor processor = new Processor(compiler, frameLoader);


        ByteArrayOutputStream consoleOutputStream = new ByteArrayOutputStream();
        PrintStream consolePrintStream = new PrintStream(consoleOutputStream);

        System.err.println("currentDir=" + currentDir);

        processor.setup(symbolTable, commonsPath, currentDir);

        pendingScript.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String code = null;
                    try {
                        code = pendingScript.getDocument().getText(startIndex, pendingScript.getDocument().getLength() - startIndex);
                    } catch (BadLocationException e1) {
                        e1.printStackTrace();
                    }

                    StringBuilder output = new StringBuilder();
                    InputStream inputStream;
                    try {
                        inputStream = new ByteArrayInputStream(code.getBytes());

                        Set<String> fields = Arrays.asList(processor.getAny().getNames2(symbolTable)).stream().collect(Collectors.toSet());
                        Compilation compilation = compiler.compile(inputStream, true, fields);

                        if (compilation.hasErrors())
                            compilation.printErrors(errorMessage -> output.append(errorMessage + "\n"));
                        else {
                            processor.setFrame(compilation.frame.localCount, compilation.frame.maxStackSize, compilation.frame.instructions);

                            PrintStream oldSystemOut = System.out;

                            ByteArrayOutputStream debugOutput = new ByteArrayOutputStream();
                            Debug.setPrintStream(new PrintStream(debugOutput));

                            consoleOutputStream.reset();
                            System.setOut(consolePrintStream);

                            processor.process();
                            String consoleOutput = new String(consoleOutputStream.toByteArray());
                            output.append("\n");

                            if(consoleOutput.length() > 0)
                                output.append(consoleOutput + "\n");

                            oldSystemOut.println(new String(debugOutput.toByteArray()));

                            System.setOut(oldSystemOut);
                            Debug.setPrintStream(oldSystemOut);

                            if(processor.canPopStack()) {
                                cocu.runtime.Process result = processor.popStack();

                                /*Instruction[] sendToString = new Instruction[] {
                                    new Instruction(Instruction.OPCODE_LOAD_LOC, 0),
                                    new Instruction(Instruction.OPCODE_SEND_CODE_0, SymbolTable.Codes.toString),
                                    new Instruction(Instruction.OPCODE_FINISH),
                                };

                                processor = new Processor(1, 1, process.instructions);
                                processor.setup(symbolTable, commonsPath, currentDir);
                                processor.process(new InteractionHistory(Arrays.asList()));*/

                                // Send toString() message to result
                                output.append(result);
                            }
                        }
                    } catch (IOException ex) {
                        System.err.println("Compilation failed.");
                        ex.printStackTrace();
                    } catch (CompilationException ex) {
                        //System.err.println("Compilation failed:");
                        ex.getErrors().printMessages(errorMessage -> output.append("\n" + errorMessage));
                    }

                    // Can the code be parsed? Then run it.
                    try {
                        pendingScript.getDocument().insertString(pendingScript.getDocument().getLength(), output + "\n" + shellPrefix, null);
                    } catch (BadLocationException e1) {
                        e1.printStackTrace();
                    }

                    startIndex = pendingScript.getDocument().getLength();
                    pendingScript.setCaretPosition(startIndex);
                }
            }
        });

        frame.getContentPane().add(new JScrollPane(pendingScript), BorderLayout.CENTER);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1028, 768);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        new Thread(() -> {
            compiler.warmup();
        }).run();
    }
}
