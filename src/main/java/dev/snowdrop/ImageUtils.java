package dev.snowdrop;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.PullImageResultCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImageUtils.class);

    protected static void pullImage(DockerClient dockerClient, String imageName) throws InterruptedException {
        LOGGER.info("Pulling image: {}", imageName);
        dockerClient.pullImageCmd(imageName)
            .exec(new PullImageResultCallback())
            .awaitCompletion();
        LOGGER.info("Image pulled: {}", imageName);
    }
}
