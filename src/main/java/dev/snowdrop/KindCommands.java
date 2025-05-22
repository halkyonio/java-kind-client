package dev.snowdrop;

import dev.snowdrop.command.cluster.*;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;

@TopCommand
@CommandLine.Command(
    mixinStandardHelpOptions = true,
    subcommands = {
        CreateContainer.class,
        StopContainer.class,
        StartContainer.class,
        DeleteContainer.class,
        RunController.class,
        StopController.class
})
public class KindCommands extends Container {
    static {
        initDockerClient();
    }
}