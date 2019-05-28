package io.bootique.tools.shell.module;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.bootique.BQCoreModule;
import io.bootique.BootiqueException;
import io.bootique.annotation.DefaultCommand;
import io.bootique.command.Command;
import io.bootique.command.CommandManager;
import io.bootique.command.CommandManagerBuilder;
import io.bootique.tools.shell.JlineShell;
import io.bootique.tools.shell.Shell;
import io.bootique.tools.shell.command.CommandLineParser;
import io.bootique.tools.shell.command.DefaultCommandLineParser;
import io.bootique.tools.shell.command.ErrorCommand;
import io.bootique.tools.shell.command.ExitCommand;
import io.bootique.tools.shell.command.HelpCommand;
import io.bootique.tools.shell.command.NewCommand;
import io.bootique.tools.shell.command.RunCommand;
import io.bootique.tools.shell.command.ShellCommand;
import io.bootique.tools.shell.command.StartShellCommand;
import org.fusesource.jansi.Ansi;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import static org.jline.builtins.Completers.*;
import static org.jline.builtins.Completers.TreeCompleter.*;

public class BqShellModule implements Module {

    private static final String BANNER_STRING =
            "@|green  ____              _   _                    |@_\n" +
            "@|green | __ )  ___   ___ | |_(_) __ _ _   _  ___|@  (_) ___\n" +
            "@|green |  _ \\ / _ \\ / _ \\| __| |/ _` | | | |/ _ \\|@ | |/ _ \\\n" +
            "@|green | |_) | (_) | (_) | |_| | (_| | |_| |  __/|@_| | (_) |\n" +
            "@|green |____/ \\___/ \\___/ \\__|_|\\__, |\\__,_|\\___|@(_)_|\\___/\n" +
            "@|green                             |_||@          shell @|cyan v0.1|@\n";

    @Override
    public void configure(Binder binder) {
        BQCoreModule.extend(binder)
                .addCommand(StartShellCommand.class)
                .addCommand(NewCommand.class)
                .addCommand(RunCommand.class)
                .addCommand(ErrorCommand.class)
                .addCommand(ExitCommand.class)
                .setDefaultCommand(StartShellCommand.class);

        binder.bind(CommandLineParser.class).to(DefaultCommandLineParser.class).in(Singleton.class);
        binder.bind(Shell.class).to(JlineShell.class).in(Singleton.class);
    }

    @Provides
    @Singleton
    CommandManager provideCommandManager(Set<Command> commands,
                                         Injector injector,
                                         @DefaultCommand Command defaultCommand) {
        return new CommandManagerBuilder(commands)
                .defaultCommand(Optional.of(defaultCommand))
                .helpCommand(injector.getInstance(HelpCommand.class))
                .build();
    }

    @Provides
    @Singleton
    Terminal createTerminal() throws IOException {
        Terminal terminal = TerminalBuilder.builder()
                .jansi(true)
                .build();
        terminal.echo(false);
        terminal.enterRawMode();
        return terminal;
    }

    @Provides
    @Singleton
    Completer createCompleter(Map<String, ShellCommand> shellCommands) {
        Object[] nodes = new Object[shellCommands.size()];
        AtomicInteger counter = new AtomicInteger();
        shellCommands.forEach((name, cmd) -> nodes[counter.getAndIncrement()] = name);

        //TODO: create this tree programmatically
        return new TreeCompleter(
                node("help", node(nodes)),
                node("new", node("project", "module")),
                node("run"),
                node("info"),
                node("exit")
        );
    }

    @Provides
    @Singleton
    LineReader createLineReader(Terminal terminal, Completer completer) {
        return LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(completer)
                .build();
    }

    @Provides
    @Singleton
    Map<String, ShellCommand> getCommands(CommandManager commandManager) {
        Map<String, ShellCommand> result = new HashMap<>();
        commandManager.getAllCommands().forEach((name, cmd) -> {
            if(!cmd.isHidden()) {
                Command command = cmd.getCommand();
                if(command instanceof ShellCommand) {
                    result.put(name, (ShellCommand)command);
                }
            }
        });
        return result;
    }

    @Provides
    @Singleton
    ShellCommand defaultCommand(CommandManager commandManager) {
        ShellCommand[] command = new ShellCommand[1];
        commandManager.getAllCommands().forEach((n, cmd) -> {
            if(cmd.isHidden()) {
                Command nextCandidate = cmd.getCommand();
                if(nextCandidate instanceof ShellCommand) {
                    if(command[0] != null) {
                        throw new BootiqueException(ShellCommand.TERMINATING_EXIT_CODE
                                , "Multiple default commands configured for shell: "
                                + command[0].getMetadata().getName() + ", "
                                + nextCandidate.getMetadata().getName());
                    }
                    command[0] = (ShellCommand) nextCandidate;
                }
            }
        });

        if(command[0] != null) {
            return command[0];
        }
        throw new BootiqueException(ShellCommand.TERMINATING_EXIT_CODE
                , "No default command configured for shell.");
    }

    @Provides
    @Banner
    @Singleton
    String createBanner() {
        return Ansi.ansi().render(BANNER_STRING).toString();
    }
}