package dev.snowdrop;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ImageUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImageUtils.class);

    public static void pullImage(DockerClient dockerClient, String imageName) throws InterruptedException {

        boolean imageExists = false;

        List<Image> images = dockerClient.listImagesCmd().exec();
        for (Image image : images) {
            if (image.getRepoTags() != null) {
                for (String repoTag : image.getRepoTags()) {
                    var fullImageReference = "docker.io/" + imageName;
                    if (repoTag.equals(fullImageReference)) {
                        imageExists = true;
                        break;
                    }
                }
            }
        }

        if (!imageExists) {
            LOGGER.info("Pulling kindest/node image: {}", imageName);
            dockerClient.pullImageCmd(imageName)
                .exec(new PullImageResultCallback())
                .awaitCompletion();
            LOGGER.info("Image pulled: {}", imageName);
        } else {
            LOGGER.info("Image already pulled: {}", imageName);
        }
    }
}
