package dev.snowdrop.command;

import com.github.dockerjava.api.exception.NotModifiedException;
import dev.snowdrop.Container;
import dev.snowdrop.MyIDP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "start", description = "Start a running container")
public class StartContainer extends Container implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartContainer.class);

    @CommandLine.Parameters(index = "0", description = "The ID or name of the container to start")
    String containerIdOrName;

    @Override
    public Integer call() {
        try {
            dockerClient.startContainerCmd(containerIdOrName)
                .exec();
            LOGGER.info("Container started: {}", containerIdOrName);
            closeDockerClient();
            return 0;
        } catch (NotModifiedException e) {
            LOGGER.info("Container is already running {}.", containerIdOrName);
            return 0;
        } catch (Exception e) {
            LOGGER.error("Error starting the container {}: {}", containerIdOrName, e.getMessage(), e);
            return 1;
        }
    }
}
