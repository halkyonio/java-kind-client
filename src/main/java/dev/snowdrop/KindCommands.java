package dev.snowdrop;

import dev.snowdrop.command.cluster.*;
import dev.snowdrop.command.pkg.PackageCommand;
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
        PackageCommand.class
})
public class KindCommands extends Container {
    static {
        initDockerClient();
    }
}