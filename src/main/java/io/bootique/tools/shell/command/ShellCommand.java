package io.bootique.tools.shell.command;

import io.bootique.command.Command;

public interface ShellCommand extends Command {

    int TERMINATING_EXIT_CODE = "exit".hashCode();

}