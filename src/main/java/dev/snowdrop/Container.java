package dev.snowdrop;

import com.github.dockerjava.api.DockerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Container {
    private static final Logger LOGGER = LoggerFactory.getLogger(Container.class);
    protected static DockerClient dockerClient;

    protected static void initDockerClient() {
        try {
            dockerClient = ContainerUtils.ConfigureDockerClient();
        } catch (Exception e) {
            LOGGER.error("Error creating Docker client: {}", e.getMessage(), e);
            System.exit(-1);
        }
    }

    protected static void closeDockerClient() {
        try {
            if (dockerClient != null) {
                dockerClient.close();
            }
        } catch (Exception e) {
            LOGGER.error("Error closing Docker client: {}", e.getMessage(), e);
        }
    }
}
