package dev.snowdrop;

import dev.snowdrop.command.HelloCommand;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;

@TopCommand
@CommandLine.Command(mixinStandardHelpOptions = true,subcommands = {HelloCommand.class})
public class KindClient {
}