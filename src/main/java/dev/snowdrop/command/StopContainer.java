package dev.snowdrop.command;

import com.github.dockerjava.api.exception.NotModifiedException;
import dev.snowdrop.Container;
import dev.snowdrop.MyIDP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.concurrent.Callable;

import static dev.snowdrop.ContainerUtils.fetchContainerId;

@CommandLine.Command(name = "stop", description = "Stop a running container")
public class StopContainer extends Container implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopContainer.class);

    @CommandLine.Parameters(index = "0", description = "The ID or name of the container to stop")
    String containerIdOrName;

    @CommandLine.Option(names = {"-t", "--time"}, description = "Seconds to wait for stop before killing the container")
    int stopTimeout = 10;

    @Override
    public Integer call() {
        try {
            var containerId = fetchContainerId(containerIdOrName);
            dockerClient.stopContainerCmd(containerId)
                .withTimeout(stopTimeout)
                .exec();
            LOGGER.info("Container stopped: {}", containerIdOrName);
            closeDockerClient();
            return 0;
        } catch (NotModifiedException e) {
            LOGGER.info("Container is already stopped {}.", containerIdOrName);
            return 0;
        } catch (Exception e) {
            LOGGER.error("Error stopping container {}: {}", containerIdOrName, e.getMessage(), e);
            return 1;
        }
    }
}
