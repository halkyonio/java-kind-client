package dev.snowdrop;

import java.util.concurrent.Callable;
import dev.snowdrop.command.CreateContainer;
import dev.snowdrop.command.DeleteContainer;
import dev.snowdrop.command.StartContainer;
import dev.snowdrop.command.StopContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Option;

@CommandLine.Command(name = "idp",
    subcommands = {
        CreateContainer.class,
        StopContainer.class,
        StartContainer.class,
        DeleteContainer.class
    },
    description = "A Kind CLI to manage a local kubernetes cluster")
public class MyIDP extends Container implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MyIDP.class);

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Display this help message")
    boolean usageHelpRequested;

    public static void main(String[] args) {
        initDockerClient();
        int exitCode = new CommandLine(new MyIDP()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        // If no subcommand is specified, show usage
        CommandLine.usage(this, System.out);
        return 0;
    }
}