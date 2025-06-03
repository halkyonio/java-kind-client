package io.halkyon.command.cluster;

import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import io.halkyon.Container;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.concurrent.Callable;

import static io.halkyon.container.ContainerUtils.fetchContainerId;

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
            return 0;
        } catch (NotFoundException nfe) {
            LOGGER.warn("The container with ID or name: {} do not exist and can't be stopped !", containerIdOrName);
            return 0;
        } catch (NotModifiedException e) {
            LOGGER.info("Container is already stopped {}.", containerIdOrName);
            return 0;
        } catch (Exception e) {
            LOGGER.error("Error stopping container {}: {}", containerIdOrName, e.getMessage(), e);
            return 1;
        } finally {
            closeDockerClient();
        }
    }
}
