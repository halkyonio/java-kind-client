package dev.snowdrop.command;

import com.github.dockerjava.api.exception.NotFoundException;
import dev.snowdrop.Container;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.concurrent.Callable;

import static dev.snowdrop.container.ContainerUtils.fetchContainerId;

@CommandLine.Command(name = "delete", description = "Delete a container")
public class DeleteContainer extends Container implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteContainer.class);

    @CommandLine.Parameters(index = "0", description = "The ID or name of the container to delete")
    String containerIdOrName;

    @Override
    public Integer call() {
        try {
            var containerId = fetchContainerId(containerIdOrName);
            dockerClient.removeContainerCmd(containerId)
                .exec();
            LOGGER.info("Container deleted: {}", containerIdOrName);
            return 0;
        } catch (NotFoundException nfe) {
            LOGGER.warn("The container with ID or name: {} do not exist and can't be stopped", containerIdOrName);
            return 0;
        } catch (Exception e) {
            LOGGER.error("Error deleting the container {}: {}", containerIdOrName, e.getMessage(), e);
            return 1;
        } finally {
            closeDockerClient();
        }
    }
}
