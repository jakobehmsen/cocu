package cocu.runtime.repl;

import cocu.reflang.*;
import cocu.reflang.Compiler;
import cocu.runtime.*;
import cocu.runtime.Process;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setTitle("Cocu");

        JTextPane pendingScript = new JTextPane();
        JTextPane historyScript = new JTextPane();

        SymbolTable symbolTable = SymbolTable.ROOT;

        cocu.reflang.Compiler compiler = new cocu.reflang.Compiler();
        Processor processor = new Processor(compiler);
        String commonsPath = "commons";
        String currentDir = new File("").getAbsolutePath();
        processor.setup(symbolTable, commonsPath, currentDir);

        pendingScript.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String code = pendingScript.getText();

                    String output = code + "\n=>\n";
                    InputStream inputStream;
                    try {
                        inputStream = new ByteArrayInputStream(code.getBytes());
                        //FrameInfo process = compiler.compile(inputStream, true);
                        Compilation compilation = compiler.compile(inputStream, true);

                        if (compilation.hasErrors())
                            compilation.printErrors();
                        else {
                            String commonsPath = "commons";
                            String currentDir = new File("").getAbsolutePath();

                            //Processor processor = new Processor(process.localCount, process.maxStackSize, process.instructions);
                            //processor.setup(symbolTable, commonsPath, currentDir);
                            processor.setFrame(compilation.frame.localCount, compilation.frame.maxStackSize, compilation.frame.instructions);
                            processor.replay(new InteractionHistory(Arrays.asList()));

                            cocu.runtime.Process result = processor.peekStack();

                            /*Instruction[] sendToString = new Instruction[] {
                                new Instruction(Instruction.OPCODE_LOAD_LOC, 0),
                                new Instruction(Instruction.OPCODE_SEND_CODE_0, SymbolTable.Codes.toString),
                                new Instruction(Instruction.OPCODE_FINISH),
                            };

                            processor = new Processor(1, 1, process.instructions);
                            processor.setup(symbolTable, commonsPath, currentDir);
                            processor.replay(new InteractionHistory(Arrays.asList()));*/

                            // Send toString() message to result
                            output += result;
                        }
                    } catch (IOException ex) {
                        System.err.println("Compilation failed.");
                        ex.printStackTrace();
                    } catch (CompilationException ex) {
                        System.err.println("Compilation failed:");
                        ex.getErrors().printMessages();
                    }

                    // Can the code be parsed? Then run it.
                    try {
                        historyScript.getDocument().insertString(0, output + "\n", null);
                    } catch (BadLocationException e1) {
                        e1.printStackTrace();
                    }

                    pendingScript.setText("");
                }
            }
        });

        frame.getContentPane().add(pendingScript, BorderLayout.NORTH);
        frame.getContentPane().add(historyScript, BorderLayout.CENTER);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1028, 768);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        new Thread(() -> {
            compiler.warmup();
        }).run();
    }
}
