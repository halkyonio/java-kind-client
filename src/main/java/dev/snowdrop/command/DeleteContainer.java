package dev.snowdrop.command;

import dev.snowdrop.Container;
import dev.snowdrop.MyIDP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.concurrent.Callable;

import static dev.snowdrop.ContainerUtils.fetchContainerId;

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

            closeDockerClient();
            return 0;
        } catch (Exception e) {
            LOGGER.error("Error deleting the container {}: {}", containerIdOrName, e.getMessage(), e);
            return 1;
        }
    }
}
