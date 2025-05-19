package dev.snowdrop;

import dev.snowdrop.command.*;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import static dev.snowdrop.Container.initDockerClient;

@TopCommand
@CommandLine.Command(
    mixinStandardHelpOptions = true,
    subcommands = {
        CreateContainer.class,
        StopContainer.class,
        StartContainer.class,
        DeleteContainer.class
})
public class KindClient extends KindContainer {
    static {
        initDockerClient();
    }
}