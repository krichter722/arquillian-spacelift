package org.arquillian.spacelift.process.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.arquillian.spacelift.execution.Answer;
import org.arquillian.spacelift.execution.Execution;
import org.arquillian.spacelift.execution.ExecutionException;
import org.arquillian.spacelift.execution.ExecutionInteraction;
import org.arquillian.spacelift.execution.OutputTransformer;
import org.arquillian.spacelift.execution.Sentence;
import org.arquillian.spacelift.execution.Task;
import org.arquillian.spacelift.execution.impl.SentenceImpl;
import org.arquillian.spacelift.process.Command;
import org.arquillian.spacelift.process.ProcessDetails;
import org.arquillian.spacelift.process.ProcessNamePrefixOutputTransformer;

/**
 * Task that consumes the output of the process.
 *
 * @author Stuart Douglas
 * @author Karel Piwko
 */
class ConsumeoProcessOutputTask extends Task<Process, ProcessDetails> {

    private static final Logger log = Logger.getLogger(ConsumeoProcessOutputTask.class.getName());

    private ExecutionInteraction interaction;
    private Process process;
    private Command command;

    public ConsumeoProcessOutputTask(Process process, Command command, ExecutionInteraction interaction) {

        this.interaction = interaction;

        // FIXME there should be a better way how to propagate process name
        OutputTransformer transformer = interaction.outputTransformer();
        if (transformer instanceof ProcessNamePrefixOutputTransformer) {
            ((ProcessNamePrefixOutputTransformer) transformer).setProcessName(command.getProgramName());
        }

    }

    @Override
    public Execution<ProcessDetails> execute() throws ExecutionException {

        Execution<ProcessDetails> futureBasedExecution = super.execute();

        Execution<ProcessDetails> execution = new ProcessBasedExecution(process, command.getProgramName());
        if (command.runsAsDeamon()) {
            execution.registerShutdownHook();
        }

        return execution;
    }

    @Override
    protected ProcessDetails process(Process input) throws Exception {

        // FIXME
        ProcessDetails details;
        // FIXME details
        Execution<ProcessDetails> execution;

        final InputStream stream = details.getStdoutAndStdErr();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        // close process input stream if we don't need it
        // closed input stream is a requirement for process not to hang on windows
        if (!interaction.requiresInputInteraction()) {
            try {
                details.getStdin().close();
            } catch (IOException ignore) {
            }
        }

        try {
            // read character by character
            int i;
            boolean reachedEOF = false;
            Sentence sentence = new SentenceImpl();
            // we have an extra check to figure out whether EOF was reached - using last expected response
            while (!reachedEOF && (i = reader.read()) != -1) {
                // add the character
                sentence.append((char) i);

                Answer answer = interaction.repliesTo(sentence);
                sentence.append(answer);
                answer.reply(execution);
                reachedEOF = execution.isMarkedAsFinished();

                // save and print output
                if (sentence.isFinished()) {
                    sentence.trim();
                    log.log(Level.FINEST, "({0}): {1}", new Object[] { details.getProcessName(), sentence });

                    details.appendOutput(sentence);
                    // propagate output/error to user
                    if (interaction.shouldOutput(sentence)) {
                        System.out.println(interaction.outputTransformer().transform(sentence));
                    }
                    if (interaction.shouldOutputToErr(sentence)) {
                        System.err.println(interaction.outputTransformer().transform(sentence));
                    }
                    sentence.reset();
                }
            }

            // handle last line
            if (!sentence.isEmpty()) {
                log.log(Level.FINEST, "{0} outputs: {1}", new Object[] { details.getProcessName(), sentence });

                execution.appendOutput(sentence);
                // propagate output/error to user
                if (interaction.shouldOutput(sentence)) {
                    System.out.println(interaction.outputTransformer().transform(sentence));
                }
                if (interaction.shouldOutputToErr(sentence)) {
                    System.err.println(interaction.outputTransformer().transform(sentence));
                }

            }
        } catch (IOException ignore) {
        }

        try {
            OutputStream os = details.getStdin();
            if (os != null) {
                os.close();
            }
        } catch (IOException ignore) {
        }

        return details;
    }

}