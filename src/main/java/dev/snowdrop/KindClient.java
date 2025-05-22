package dev.snowdrop;

import dev.snowdrop.command.cluster.CreateContainer;
import dev.snowdrop.command.cluster.DeleteContainer;
import dev.snowdrop.command.cluster.StartContainer;
import dev.snowdrop.command.cluster.StopContainer;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;

@TopCommand
@CommandLine.Command(
    mixinStandardHelpOptions = true,
    subcommands = {
        CreateContainer.class,
        StopContainer.class,
        StartContainer.class,
        DeleteContainer.class
})
public class KindClient extends Container {
    static {
        initDockerClient();
    }
}