package io.halkyon;

import io.halkyon.command.cluster.CreateContainer;
import io.halkyon.command.cluster.DeleteContainer;
import io.halkyon.command.cluster.StartContainer;
import io.halkyon.command.cluster.StopContainer;
import io.halkyon.command.pkg.PackageCommand;
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